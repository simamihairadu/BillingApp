package net.gsdgroup.billing.webservice.billDTO;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import net.gsdgroup.billing.webservice.accountDTO.AccountDTO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BillDTO {

    private int id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy'T'HH:mm:ss")
    private Date issueDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy'T'HH:mm:ss")
    private Date dueDate;
    private int accountId;
    @JsonBackReference
    private AccountDTO account;
    @JsonManagedReference
    private List<BillChargeDTO> billCharges = new ArrayList<>();

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

    public AccountDTO getAccount() {
        return account;
    }

    public void setAccount(AccountDTO account) {
        this.account = account;
    }

    public List<BillChargeDTO> getBillCharges() {
        return billCharges;
    }

    public void setBillCharges(List<BillChargeDTO> billCharges) {
        this.billCharges = billCharges;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }
}
