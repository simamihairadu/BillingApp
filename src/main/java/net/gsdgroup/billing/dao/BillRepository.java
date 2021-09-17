package net.gsdgroup.billing.dao;

import net.gsdgroup.billing.entity.Bill;
import net.gsdgroup.billing.webservice.billDTO.MonthlyAmountDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BillRepository extends AbstractCommonRepository<Bill> {

    public List<MonthlyAmountDTO> getTotalChargedEachMonth(){

        StringBuilder query = new StringBuilder("SELECT NEW " +
                "net.gsdgroup.billing.webservice.billDTO.MonthlyAmountDTO (SUM(bc.amount+bc.tax), MONTHNAME(b.issueDate))" +
                "FROM BillCharge bc " +
                "JOIN bc.bill b " +
                "GROUP BY MONTH(b.issueDate)");

        List<MonthlyAmountDTO> result = factory.createEntityManager()
                .createQuery(query.toString(),MonthlyAmountDTO.class)
                .getResultList();
        return result;
    }
}
