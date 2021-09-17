package net.gsdgroup.billing.entity;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account")
public class Account {
    //TODO  cascade remove only
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true)
    private int id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @OneToMany(mappedBy = "account",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
                fetch = FetchType.LAZY)
    private List<Bill> bills = new ArrayList<>();

    public void addBill(Bill bill) {

        bills.add(bill);
        bill.setAccount(this);
    }

    public void removeBill(Bill bill) {

        bills.remove(bill);
        bill.setAccount(null);
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", bills=" + bills +
                '}';
    }
}
