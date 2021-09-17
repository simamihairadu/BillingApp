package net.gsdgroup.billing.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "bill")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true)
    private int id;

    @Column(name = "issue_date")
    private Date issueDate;

    @Column(name = "due_date")
    private Date dueDate;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @OneToMany(mappedBy = "bill",
               cascade = CascadeType.ALL,
               orphanRemoval = true,fetch = FetchType.LAZY)
    private List<BillCharge> billCharges = new ArrayList<>();

    public void addBillCharge(BillCharge billCharge) {

        billCharges.add(billCharge);
        billCharge.setBill(this);
    }

    public void removeBillCharge(BillCharge billCharge) {

        billCharges.remove(billCharge);
        billCharge.setBill(null);
    }

    public List<BillCharge> getBillCharges() {
        return billCharges;
    }

    public void setBillCharges(List<BillCharge> billCharges) {
        this.billCharges = billCharges;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "id=" + id +
                ", issueDate=" + issueDate +
                ", dueDate=" + dueDate +
                ", billCharges=" + billCharges +
                '}';
    }
}
