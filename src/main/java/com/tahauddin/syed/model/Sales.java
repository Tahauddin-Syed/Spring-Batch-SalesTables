package com.tahauddin.syed.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Sales {

    /**
     * Region,
     * Country,
     * Item Type,
     * Sales Channel,
     * Order Priority,
     * Order Date,
     * Order ID,
     * Ship Date,
     * Units Sold,
     * Unit Price,
     * Unit Cost,
     * Total Revenue,
     * Total Cost,
     * Total Profit
     */

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

    /**
     * Create Table Query ::
     *
     create table SALES_TABLE (
     ID varchar(100) PRIMARY KEY,
     REGION varchar(255),
     COUNTRY varchar(255),
     ITEM_TYPE varchar(255),
     SALES_CHANNEL varchar(255),
     ORDER_PRIORITY varchar(255),
     ORDER_DATE varchar(255),
     ORDER_ID varchar(255),
     SHIP_DATE varchar(255),
     UNITS_SOLD varchar(255),
     UNIT_PRICE varchar(255),
     UNIT_COST varchar(255),
     TOTAL_REVENUE varchar(255),
     TOTAL_COST varchar(255),
     TOTAL_PROFIT varchar(255)
     );
     */
}
