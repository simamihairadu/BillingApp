package net.gsdgroup.billing.webservice;

import net.gsdgroup.billing.business.AccountService;
import net.gsdgroup.billing.business.BillService;
import net.gsdgroup.billing.business.JsonResponseMessage;
import net.gsdgroup.billing.webservice.accountDTO.AccountDTO;
import net.gsdgroup.billing.webservice.billDTO.MonthlyAmountDTO;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Controller class that handles custom endpoints for custom queries.
 */
public class AnalysisController extends ServerResource {

    @Autowired
    private AccountService accountService;
    @Autowired
    private BillService billService;

    /**
     * Looks up the accounts with bills that have due date > current date.
     */
    @Get("json?op=overdue_bills")
    public Representation getAllAccountsWithOverdueBills(){
        //TODO catch exception
        List<AccountDTO> accountDTOList = accountService.getAccountsWithOverdueBills();
        return new JacksonRepresentation<List<AccountDTO>>(accountDTOList);
    }

    /**
     * Gets a list of : Amount | Month values.
     */
    @Get("json?op=monthly_amount")
    public Representation getMonthlyChargedAmount(){

        List<MonthlyAmountDTO> monthlyAmountDTOList = billService.getTotalChargedEachMonth();
        return new JacksonRepresentation<List<MonthlyAmountDTO>>(monthlyAmountDTOList);
    }
}
