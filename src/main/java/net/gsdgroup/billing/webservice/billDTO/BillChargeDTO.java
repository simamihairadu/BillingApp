package net.gsdgroup.billing.webservice.billDTO;

import com.fasterxml.jackson.annotation.JsonBackReference;

public class BillChargeDTO {

    private int id;
    private String chargeType;
    @JsonBackReference
    private BillDTO bill;
    private float amount;
    private float tax;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChargeType() {
        return chargeType;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public BillDTO getBill() {
        return bill;
    }

    public void setBill(BillDTO bill) {
        this.bill = bill;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public float getTax() {
        return tax;
    }

    public void setTax(float tax) {
        this.tax = tax;
    }
}
