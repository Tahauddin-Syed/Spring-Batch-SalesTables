package com.tahauddin.syed.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SalesEntity {


    private String id;
    private String region;
    private String country;
    private String itemType;
    private String saleChannel;
    private String orderPriority;
    private String orderDate;
    private String orderID;
    private String shipDate;
    private String unitsSold;
    private String unitPrice;
    private String unitCost;
    private String totalRevenue;
    private String totalCost;
    private String totalProfit;
}
