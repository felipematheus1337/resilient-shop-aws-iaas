package resilient_shop_aws_iaas.v1.domain;

import java.math.BigDecimal;

public class Items {

    private String name;

    private String sku;

    private BigDecimal value;

    private Integer quantity;

    public Items() {
    }

    public Items(String name, String sku, BigDecimal value, Integer quantity) {
        this.name = name;
        this.sku = sku;
        this.value = value;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
