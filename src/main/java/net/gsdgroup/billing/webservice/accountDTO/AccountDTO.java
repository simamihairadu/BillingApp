package net.gsdgroup.billing.webservice.accountDTO;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import net.gsdgroup.billing.webservice.billDTO.BillDTO;

import java.util.ArrayList;
import java.util.List;

public class AccountDTO {

    private int id;
    private String firstName;
    private String lastName;
    @JsonManagedReference
    private List<BillDTO> bills = new ArrayList<>();

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

    public List<BillDTO> getBills() {
        return bills;
    }

    public void setBills(List<BillDTO> bills) {
        this.bills = bills;
    }

    @Override
    public String toString() {
        return "AccountDTO{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", bills=" + bills +
                '}';
    }
}
