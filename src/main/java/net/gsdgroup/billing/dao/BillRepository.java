package net.gsdgroup.billing.dao;

import net.gsdgroup.billing.entity.Bill;
import net.gsdgroup.billing.webservice.billDTO.MonthlyAmountDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BillRepository extends AbstractCommonRepository<Bill> {

    public List<Object[]> getTotalChargedEachMonth(){
        //TODO refactor
        StringBuilder query = new StringBuilder("SELECT SUM(bc.amount+bc.tax) AS amount,MONTH(b.issueDate) AS month FROM BillCharge bc JOIN bc.bill b GROUP BY MONTH(b.issueDate)");
        List<Object[]> result = factory.createEntityManager()
                .createQuery("SELECT SUM(bc.amount + IFNULL(bc.tax,0)) AS amount,MONTH(b.issueDate) AS month FROM BillCharge bc JOIN bc.bill b GROUP BY MONTH(b.issueDate)",Object[].class)
                .getResultList();
        return result;
    }
}
