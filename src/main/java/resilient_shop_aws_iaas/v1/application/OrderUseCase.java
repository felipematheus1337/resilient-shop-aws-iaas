package resilient_shop_aws_iaas.v1.application;

import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import resilient_shop_aws_iaas.v1.domain.Order;

import java.util.List;

@Component
public class OrderUseCase {

    private final MongoTemplate mongoTemplate;

    private static final Integer BATCH_COUNT_SIZE = 1000;

    public OrderUseCase(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        System.out.println("MongoTemplate injetado: " + mongoTemplate.getDb().getName());
    }

    public void createOrders(List<Order> orders) {

        for (int i = 0; i < orders.size(); i += BATCH_COUNT_SIZE) {

            int end = Math.min(i + BATCH_COUNT_SIZE, orders.size());
            List<Order> batch = orders.subList(i, end);

            BulkOperations bulkOps =
                    mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Order.class);

            bulkOps.insert(batch);
            bulkOps.execute();
        }
    }
}
