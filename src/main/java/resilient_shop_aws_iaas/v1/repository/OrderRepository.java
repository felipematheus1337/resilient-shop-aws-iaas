package resilient_shop_aws_iaas.v1.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import resilient_shop_aws_iaas.v1.domain.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
}
