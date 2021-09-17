package net.gsdgroup.billing.webservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.gsdgroup.billing.business.BillService;
import net.gsdgroup.billing.business.JsonResponseMessage;
import net.gsdgroup.billing.entity.Bill;
import net.gsdgroup.billing.exceptions.ServiceException;
import net.gsdgroup.billing.webservice.billDTO.BillDTO;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
/**
 * Bill endpoints. Unless other json data is requested the endpoint's response is a JsonResponseMessage.
 */
public class BillController extends ServerResource {

    @Autowired
    private BillService billService;
    @Autowired
    private ObjectMapper mapper;

    /**
     * Endpoint for requesting bills. If the bill id is missing from the parameter, it returns all bills.
     * Otherwise it returns the bill identified by that id.
     */
    @Get("json")
    public Representation getBills(){

        String requestAttribute = (String) getRequestAttributes().get("billId");

        if (requestAttribute == null) {

            List<BillDTO> billDTOList = billService.getAllBills();
            return new JacksonRepresentation<List<BillDTO>>(billDTOList);
        }

        try {
            int billId = Integer.parseInt(requestAttribute);
            BillDTO billDTO = billService.getBillById(billId);
            return new JacksonRepresentation<BillDTO>(billDTO);

        } catch (NumberFormatException e){

            getResponse().setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST);
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
     * Takes the bill json from the request body and converts it into an entity to be added into the database.
     */
    @Post("json")
    public Representation addBill(String billJson){

        try{
            BillDTO billDTO = mapper.readValue(billJson, BillDTO.class);
            Bill billEntity = billService.buildBillEntity(billDTO);
            int billId = billService.addBill(billEntity);

            return new JacksonRepresentation<JsonResponseMessage>(new JsonResponseMessage(String.valueOf(billId)));

        } catch (JsonProcessingException | IllegalArgumentException | ServiceException e) {

            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            int statusCode = getStatus().getCode();
            JsonResponseMessage jsonResponseMessage = new JsonResponseMessage(e.getMessage(), statusCode);
            return new JacksonRepresentation<JsonResponseMessage>(jsonResponseMessage);
        }
    }

    /**
     * Endpoint for deleting a bill. The endpoint requires a parameter to look up the bill to be deleted.
     */
    @Delete("json")
    public Representation deleteBill(){

        try{
            String requestAttribute = (String) getRequestAttributes().get("billId");
            int billId =  Integer.parseInt(requestAttribute);
            billService.deleteBill(billId);
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
     * Takes an bill json from the request body and updates its values accordingly. The given bill json
     * must have an id in order to be successfully updated.
     */
    @Put("json")
    public Representation updateBill(String billJson){

        try{
            BillDTO billDTO = mapper.readValue(billJson, BillDTO.class);
            Bill billEntity = billService.buildBillEntity(billDTO);
            billService.updateBill(billEntity);

            return new JacksonRepresentation<JsonResponseMessage>(new JsonResponseMessage("Operation successful."));

        } catch (Exception e) {

            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            int statusCode = getStatus().getCode();
            JsonResponseMessage jsonResponseMessage = new JsonResponseMessage(e.getMessage(), statusCode);
            return new JacksonRepresentation<JsonResponseMessage>(jsonResponseMessage);
        }
    }
}