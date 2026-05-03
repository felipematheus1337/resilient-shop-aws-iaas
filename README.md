# Resilient Shop AWS IaaS

Projeto de laboratório para praticar uma arquitetura IaaS na AWS usando EC2, Docker, Application Load Balancer, Target Group, Auto Scaling Group, NAT Gateway e MongoDB em instância separada.

> Status: projeto utilizado para estudo/prática de infraestrutura AWS. A aplicação Spring Boot já possui imagem publicada no Docker Hub e a proposta é executá-la manualmente em EC2 via `docker pull`, sem ECS e sem ECR.

---

## Objetivo

O objetivo deste projeto é praticar, de forma didática, a montagem de uma arquitetura AWS baseada em infraestrutura como serviço, simulando um ambiente com:

- VPC própria;
- subnets públicas e privadas;
- Application Load Balancer exposto à internet;
- EC2s de aplicação executando container Docker;
- Auto Scaling Group para escalar as instâncias da aplicação;
- Target Group para registrar e verificar a saúde das instâncias;
- EC2 separada para MongoDB;
- NAT Gateway para permitir saída à internet a partir de subnets privadas.

---

## Arquitetura planejada

Fluxo principal:

```text
User / Browser
    ↓
Internet
    ↓
Application Load Balancer - HTTP :80
    ↓
Target Group
    ↓
EC2 App 1 / EC2 App 2 / EC2 App 3
    ↓
EC2 MongoDB
```

A aplicação Spring Boot é executada em containers Docker dentro de instâncias EC2. O MongoDB fica em uma EC2 separada, idealmente em subnet privada. O Load Balancer recebe o tráfego externo e encaminha apenas para instâncias saudáveis registradas no Target Group.

---

## Componentes utilizados

### VPC

A VPC isola a rede do projeto dentro da AWS. Dentro dela são criadas as subnets, route tables, security groups e demais recursos de rede.

### Subnets públicas

Usadas para recursos que precisam ser alcançáveis pela internet ou precisam estar diretamente associados à saída pública, como:

- Application Load Balancer;
- NAT Gateway;
- eventualmente uma instância bastion para administração.

### Subnets privadas

Usadas para recursos internos, como:

- EC2s da aplicação;
- EC2 do MongoDB.

A ideia é evitar exposição direta de servidores internos à internet.

### Internet Gateway

Permite que recursos em subnets públicas tenham comunicação com a internet, desde que exista rota `0.0.0.0/0` apontando para ele.

### NAT Gateway

Permite que instâncias em subnets privadas acessem a internet para baixar pacotes, imagens Docker e atualizações, sem permitir acesso direto de entrada vindo da internet.

### Elastic IP

IP público fixo usado pelo NAT Gateway. Enquanto o NAT Gateway existir, o Elastic IP fica associado a ele. Ao deletar o NAT Gateway, é necessário liberar o Elastic IP manualmente caso ele não seja mais usado.

### Application Load Balancer

Recebe requisições HTTP na porta 80 e encaminha para o Target Group. Ele funciona como ponto único de entrada da aplicação.

### Target Group

Grupo de destinos usado pelo Load Balancer. No projeto, o Target Group aponta para as EC2s da aplicação na porta 8080.

Também é responsável por executar health checks para saber quais instâncias estão saudáveis.

### Auto Scaling Group

Grupo responsável por manter a quantidade desejada de EC2s da aplicação. Pode aumentar ou reduzir a quantidade de instâncias com base em políticas como uso médio de CPU.

### Launch Template

Modelo usado pelo Auto Scaling Group para criar novas EC2s. Define AMI, tipo da instância, security group, key pair e user data.

### EC2 App

Instâncias EC2 que executam a imagem Docker da aplicação Spring Boot.

Exemplo de execução esperada:

```bash
docker run -d \
  --name resilient-shop \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e SPRING_DATA_MONGODB_URI='mongodb://admin:SENHA@IP_PRIVADO_MONGO:27017/resilient_shop?authSource=admin' \
  usuario/resilient-shop-aws-iaas:latest
```

### EC2 MongoDB

Instância separada para banco de dados MongoDB, executando via Docker.

Exemplo:

```bash
docker run -d \
  --name resilient-shop-mongodb \
  --restart unless-stopped \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD='SENHA_FORTE' \
  -v mongodb_data:/data/db \
  mongo:7
```

---

## Security Groups sugeridos

### Security Group do ALB

Inbound:

```text
HTTP 80
Source: 0.0.0.0/0
```

Outbound:

```text
All traffic
Destination: 0.0.0.0/0
```

### Security Group da aplicação

Inbound:

```text
TCP 8080
Source: Security Group do ALB
```

SSH, se necessário para debug:

```text
TCP 22
Source: seu IP /32
```

Outbound:

```text
All traffic
Destination: 0.0.0.0/0
```

### Security Group do MongoDB

Inbound:

```text
TCP 27017
Source: Security Group da aplicação
```

SSH, se necessário e com bastion/app:

```text
TCP 22
Source: Security Group da aplicação ou bastion
```

Outbound:

```text
All traffic
Destination: 0.0.0.0/0
```

---

## Variáveis de ambiente da aplicação

A aplicação deve receber a URI do MongoDB via variável de ambiente:

```yaml
spring:
  data:
    mongodb:
      uri: ${SPRING_DATA_MONGODB_URI}

server:
  port: ${SERVER_PORT:8080}
```

Exemplo de URI:

```text
mongodb://admin:SENHA@10.0.10.50:27017/resilient_shop?authSource=admin
```

---

## Health Check

Para uso com Target Group, o ideal é que a aplicação possua um endpoint de saúde.

Exemplo com Spring Actuator:

```text
/actuator/health
```

Caso não use Actuator, pode ser criado um endpoint simples que retorne HTTP 200.

O Target Group só considera a instância saudável se o health check retornar código de sucesso.

---

## Ordem de criação dos recursos

Ordem recomendada:

1. Criar VPC;
2. Criar Internet Gateway;
3. Criar subnets públicas e privadas;
4. Criar route table pública com rota para Internet Gateway;
5. Criar Elastic IP;
6. Criar NAT Gateway em subnet pública;
7. Criar route table privada com rota para NAT Gateway;
8. Criar Security Groups;
9. Criar EC2 do MongoDB;
10. Criar Launch Template da aplicação;
11. Criar Target Group;
12. Criar Application Load Balancer;
13. Criar Auto Scaling Group;
14. Verificar health checks;
15. Testar a aplicação pelo DNS do Load Balancer.

---

## Possíveis problemas encontrados

### Instâncias unhealthy no Target Group

Causas comuns:

- aplicação não subiu;
- Docker não instalou corretamente;
- `docker pull` falhou;
- variável `SPRING_DATA_MONGODB_URI` incorreta;
- MongoDB inacessível pela rede;
- Security Group bloqueando porta 8080;
- Security Group bloqueando porta 27017;
- health check apontando para path errado;
- aplicação demorando mais que o grace period do Auto Scaling Group;
- subnet privada sem rota para NAT Gateway;
- imagem Docker com erro de inicialização.

### Timeout ao tentar acessar EC2

Causas comuns:

- EC2 sem IP público;
- EC2 em subnet privada;
- Security Group sem regra de SSH;
- tentativa de acesso direto a instância privada sem bastion ou Session Manager;
- rota incorreta na subnet;
- Network ACL bloqueando tráfego.

---

## Comandos úteis para debug

Ver containers:

```bash
docker ps -a
```

Ver logs da aplicação:

```bash
docker logs resilient-shop
```

Ver logs do MongoDB:

```bash
docker logs resilient-shop-mongodb
```

Ver logs do user data:

```bash
sudo cat /var/log/cloud-init-output.log
```

Testar aplicação localmente na EC2:

```bash
curl localhost:8080/actuator/health
```

Testar porta do MongoDB a partir da EC2 App:

```bash
nc -vz IP_PRIVADO_DO_MONGO 27017
```

---

## Checklist para evidências

Sugestões de prints para documentar no GitHub:

- diagrama da arquitetura;
- VPC criada;
- subnets públicas e privadas;
- route tables;
- NAT Gateway;
- Security Groups;
- EC2 MongoDB;
- Launch Template;
- Target Group;
- Application Load Balancer;
- Auto Scaling Group;
- health checks do Target Group;
- aplicação acessível via DNS do ALB;
- logs do container rodando.

---

## Aviso de custos

Este projeto pode gerar custos na AWS, especialmente por causa de:

- NAT Gateway;
- Elastic IP não utilizado;
- Load Balancer;
- EC2s ligadas;
- tráfego processado;
- volumes EBS.

Após finalizar o laboratório, delete todos os recursos criados para evitar cobranças.

---

## Tecnologias

- Java;
- Spring Boot;
- MongoDB;
- Docker;
- AWS EC2;
- AWS VPC;
- AWS Application Load Balancer;
- AWS Auto Scaling Group;
- AWS NAT Gateway.

---

## Autor

Projeto desenvolvido para estudo e prática de arquitetura IaaS na AWS.
