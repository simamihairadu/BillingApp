package net.gsdgroup.billing.business;

import net.gsdgroup.billing.entity.Account;
import net.gsdgroup.billing.entity.Bill;
import org.springframework.stereotype.Component;

@Component
public class DataValidator {

    public boolean validateAccountData(Account account){

        if(account.getFirstName() == null || account.getLastName() == null){
            return false;
        }

        return true;
    }
//TODO check for bill charges
    public boolean validateBillData(Bill bill){

        if(bill.getIssueDate() == null ||
        bill.getDueDate() == null ||
        bill.getAccount() == null){
            return false;
        }

        return true;
    }
}
