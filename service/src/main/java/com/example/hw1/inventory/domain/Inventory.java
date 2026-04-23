package com.example.hw1.inventory.domain;

public class Inventory {

    private Long productId;
    private Integer total;
    private Integer available;
    private Integer locked;
    private Long version;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Integer getAvailable() { return available; }
    public void setAvailable(Integer available) { this.available = available; }
    public Integer getLocked() { return locked; }
    public void setLocked(Integer locked) { this.locked = locked; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
