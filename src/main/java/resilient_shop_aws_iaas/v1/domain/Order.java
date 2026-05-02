package resilient_shop_aws_iaas.v1.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
public class Order {

    @Id
    private String id;

    private String name;

    private String email;

    private List<Items> itens;

    public Order() {
    }

    public Order(String id, String name, String email, List<Items> itens) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.itens = itens;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Items> getItens() {
        return itens;
    }

    public void setItens(List<Items> itens) {
        this.itens = itens;
    }
}
