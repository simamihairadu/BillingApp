package net.gsdgroup.billing.entity;

import javax.persistence.*;

@Entity
@Table(name = "bill_charges")
public class BillCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "charge_type")
    private String chargeType;

    @ManyToOne
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Column(name = "amount")
    private float amount;

    @Column(name = "tax")
    private float tax;


    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

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

    @Override
    public String toString() {
        return "BillCharge{" +
                "id=" + id +
                ", chargeType='" + chargeType + '\'' +
                ", amount=" + amount +
                ", tax=" + tax +
                '}';
    }
}
