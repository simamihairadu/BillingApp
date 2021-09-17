package net.gsdgroup.billing.webservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.gsdgroup.billing.business.AccountService;
import net.gsdgroup.billing.business.JsonResponseMessage;
import net.gsdgroup.billing.entity.Account;
import net.gsdgroup.billing.exceptions.ServiceException;
import net.gsdgroup.billing.exceptions.WebServiceException;
import net.gsdgroup.billing.webservice.accountDTO.AccountDTO;
import net.gsdgroup.billing.webservice.accountDTO.SimpleAccountDTO;
import org.hibernate.StaleObjectStateException;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException;

import java.util.List;

/**
 * Account endpoints. Unless other json data is requested the endpoint's response is a JsonResponseMessage.
 */
public class AccountController extends ServerResource {

    @Autowired
    private AccountService accountService;
    @Autowired
    private ObjectMapper mapper;

    /**
     * Endpoint for requesting accounts. If the account id is missing from the parameter, it returns all accounts.
     * Otherwise it returns the account identified by that id.
     */
    @Get("json")
    public Representation getAccounts(){

        String requestAttribute = (String) getRequestAttributes().get("accountId");

        if(requestAttribute == null){

            List<AccountDTO> accountDTOList = accountService.getAllAccounts();
            return new JacksonRepresentation<List<AccountDTO>>(accountDTOList);
        }

        try{
            int id = Integer.parseInt(requestAttribute);
            AccountDTO accountDTO = accountService.getAccountById(id);
            return new JacksonRepresentation<AccountDTO>(accountDTO);

        } catch (NumberFormatException e){

            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            int statusCode = getStatus().getCode();
            JsonResponseMessage jsonResponseMessage = new JsonResponseMessage("Missing or invalid parameter.",statusCode);
            return new JacksonRepresentation<JsonResponseMessage>(jsonResponseMessage);

        } catch (ServiceException e){

            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            int statusCode = getStatus().getCode();
            JsonResponseMessage jsonResponseMessage = new JsonResponseMessage(e.getMessage(),statusCode);
            return new JacksonRepresentation<JsonResponseMessage>(jsonResponseMessage);
        }
    }

    /**
     * Endpoint for deleting an account. The endpoint requires a parameter to look up the account to be deleted.
     */
    @Delete("json")
    public Representation deleteAccount() {

        try{
            String requestAttribute = (String) getRequestAttributes().get("accountId");
            int id = Integer.parseInt(requestAttribute);
            accountService.deleteAccount(id);
            return new JacksonRepresentation<JsonResponseMessage>(new JsonResponseMessage("Operation successful."));

        } catch (NumberFormatException e){

            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            int statusCode = getStatus().getCode();
            JsonResponseMessage jsonResponseMessage = new JsonResponseMessage("Missing or invalid parameter.", statusCode);
            return new JacksonRepresentation<JsonResponseMessage>(jsonResponseMessage);

        } catch (ServiceException e){

            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            int statusCode = getStatus().getCode();
            JsonResponseMessage jsonResponseMessage = new JsonResponseMessage(e.getMessage(),statusCode);
            return new JacksonRepresentation<JsonResponseMessage>(jsonResponseMessage);
        }
    }

    /**
     * Takes the account json from the request body and converts it into an entity to be added into the database.
     * @param accountJson
     */
    @Post("json")
    public Representation addAccount(String accountJson) {

        try {
            SimpleAccountDTO accountDTO = mapper.readValue(accountJson, SimpleAccountDTO.class);
            Account accountEntity = accountService.buildAccountEntity(accountDTO);
            int id = accountService.addAccount(accountEntity);

            return new JacksonRepresentation<JsonResponseMessage>(new JsonResponseMessage(String.valueOf(id)));

        } catch (JsonProcessingException | IllegalArgumentException | ServiceException e) {

            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            int statusCode = getStatus().getCode();
            JsonResponseMessage jsonResponseMessage = new JsonResponseMessage(e.getMessage(), statusCode);
            return new JacksonRepresentation<JsonResponseMessage>(jsonResponseMessage);
        }
    }

    /**
     * Takes an account json from the request body and updates its values accordingly. The given account json
     * must have an id in order to be successfully updated.
     */
    @Put("json")
    public Representation updateAccount(String accountJson) {

        try{
            //TODO refactor update
/*            AccountDTO accountDTO = mapper.readValue(accountJson, AccountDTO.class);
            int accountId = (int) getRequestAttributes().get("accountId");
            Account accountEntity = accountService.getAccountById(accountId);
            accountService.updateAccount(accountEntity); //2 params : accountId, BaseAccountDTO*/
            //404 error
            return new JacksonRepresentation<JsonResponseMessage>(new JsonResponseMessage("Operation successful."));

        } catch (Exception e) {

            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            int statusCode = getStatus().getCode();
            JsonResponseMessage jsonResponseMessage = new JsonResponseMessage(e.getMessage(), statusCode);
            return new JacksonRepresentation<JsonResponseMessage>(jsonResponseMessage);
        }
    }
}
