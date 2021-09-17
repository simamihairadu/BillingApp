package net.gsdgroup.tests.account;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.gsdgroup.billing.business.AccountService;
import net.gsdgroup.billing.business.BillService;
import net.gsdgroup.billing.business.JsonResponseMessage;
import net.gsdgroup.billing.entity.Account;
import net.gsdgroup.billing.entity.Bill;
import net.gsdgroup.billing.entity.BillCharge;
import net.gsdgroup.billing.exceptions.ServiceException;
import net.gsdgroup.billing.webservice.accountDTO.AccountDTO;
import net.gsdgroup.billing.webservice.billDTO.BillDTO;
import net.gsdgroup.billing.webservice.billDTO.MonthlyAmountDTO;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Component;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class BillWebServiceTests {

    private static AccountService accountService;
    private static BillService billService;
    private static SessionFactory sessionFactory;
    private static ObjectMapper objectMapper;

    /**
     * Starts the Spring context. And fetches all the beans necessary and starts the Restlet server.
     * @throws Exception
     */
    @BeforeClass
    public static void testInit() throws Exception {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        ((Component) context.getBean("top")).start();

        accountService = context.getBean(AccountService.class);
        billService = context.getBean(BillService.class);
        sessionFactory = context.getBean(SessionFactory.class);
        objectMapper = new ObjectMapper();

    }

    /**
     * Deletes all data from all the tables. This ensures that each test runs on a clean database.
     */
    @Before
    public void deleteTableData(){

        try(Session session = sessionFactory.openSession()){
            Transaction txn = session.beginTransaction();

            session.createQuery("DELETE FROM BillCharge").executeUpdate();
            session.createQuery("DELETE FROM Bill").executeUpdate();
            session.createQuery("DELETE FROM Account").executeUpdate();

            txn.commit();
        }
    }

    /**
     * Happy flow for accessing the endpoint to get all the bills.
     */
    @Test
    public void testGetAllBillsEndpoint() throws ParseException {

        List<Bill> billList = populateBillTable();
        List<BillDTO> billDTOList = null;

        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet("http://localhost:8182/bills");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String entityString = entityContentToString(entity);
            billDTOList = Arrays.asList(objectMapper.readValue(entityString, BillDTO[].class));

        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean listMatch = compareBillListValues(billList,billDTOList);

        assertEquals(listMatch,true);
        assertEquals(3, billDTOList.size());
    }

    /**
     * Happy flow for getting one bill using an id given as parameter in the URI.
     * @throws ParseException
     */
    @Test
    public void testGetBillByIdEndpoint_1() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account account = createAccount();
        Bill bill = createBillWithNoId();
        BillDTO billDTO = null;

        account.addBill(bill);
        accountService.addAccount(account);
        int id = bill.getId();

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpGet httpGet = new HttpGet("http://localhost:8182/bills/"+id);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            objectMapper.setDateFormat(dateFormat);
            billDTO = objectMapper.readValue(entityContentToString(entity), BillDTO.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(billDTO);
        assertEquals(id, billDTO.getId());
        assertEquals(dateFormat.parse("31/12/1998"),billDTO.getIssueDate());
        assertEquals(dateFormat.parse("31/12/1998"),billDTO.getDueDate());
    }

    /**
     * The id given as parameter no longer exists or doesn't exist in the database. This prompts the "No result." error message
     * with the 404 status code.
     */
    @Test
    public void testGetBillByIdEndpoint_2a() throws ParseException {

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        Bill bill = createBillWithNoId();

        account.addBill(bill);
        accountService.addAccount(account);

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpGet httpGet = new HttpGet("http://localhost:8182/bills/-1");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            jsonResponseMessage = objectMapper.readValue(entityContentToString(entity),JsonResponseMessage.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(jsonResponseMessage);
        assertEquals("No result.",jsonResponseMessage.getMessage());
        assertEquals(404,jsonResponseMessage.getStatusCode());
    }

    /**
     * The id given as a parameter has an invalid format (string in this case). This prompts the "Missing or invalid parameter."
     * error message with the 400 status code.
     */
    @Test
    public void testGetBillByIdEndpoint_2b() throws ParseException {

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        Bill bill = createBillWithNoId();
        BillDTO billDTO = null;

        account.addBill(bill);
        accountService.addAccount(account);

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpGet httpGet = new HttpGet("http://localhost:8182/bills/abc");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            jsonResponseMessage = objectMapper.readValue(entityContentToString(entity),JsonResponseMessage.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(jsonResponseMessage);
        assertEquals("Missing or invalid parameter.",jsonResponseMessage.getMessage());
        assertEquals(400,jsonResponseMessage.getStatusCode());
    }
    /**
     * Happy flow for adding a bill. The given bill data is converted into JSON format
     * and then passed in the request body.
     */
    @Test
    public void testAddBillEndpoint_1() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account account = createAccount();
        accountService.addAccount(account);

        int billId = 0;
        Bill bill = createBillWithNoId();
        bill.setAccount(account);
        BillDTO billDTO = billService.buildBillDTO(bill);
        billDTO.setAccountId(account.getId());

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpPost httpPost = new HttpPost("http://localhost:8182/bills");
            String billJson = objectMapper.writeValueAsString(billDTO);

            HttpEntity entity = new StringEntity(billJson);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);

            assertEquals(200,response.getStatusLine().getStatusCode());

            String entityString = entityContentToString(response.getEntity());
            JsonResponseMessage jsonResponseMessage = objectMapper.readValue(entityString,JsonResponseMessage.class);
            billId = Integer.parseInt(jsonResponseMessage.getMessage());

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotEquals(0, billId);

        BillDTO addedBillDTO = billService.getBillById(billId);

        assertEquals(billId, addedBillDTO.getId());
        assertEquals(dateFormat.parse("31/12/1998"),addedBillDTO.getIssueDate());
        assertEquals(dateFormat.parse("31/12/1998"),addedBillDTO.getDueDate());
        assertEquals(account.getId(), addedBillDTO.getAccountId());
    }

    /**
     * Passing a null object to the POST method. This prompts the "Error processing request. The bill entity cannot be null."
     * error message with the 400 status code.
     */
    @Test
    public void testAddBillEndpoint_2a() {

        JsonResponseMessage jsonResponseMessage = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost("http://localhost:8182/bills");
            String billJson = objectMapper.writeValueAsString(null);

            HttpEntity entity = new StringEntity(billJson);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);

            String entityString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(entityString,JsonResponseMessage.class);

        } catch (IOException e) {
            fail("Not expected.");
        }

        assertEquals("Error processing request. The bill entity cannot be null.", jsonResponseMessage.getMessage());
        assertEquals(400, jsonResponseMessage.getStatusCode());
    }

    /**
     * Attempting to add a bill with an assigned account but null values for issueDate and dueDate.
     */
    @Test
    public void testAddBillEndpoint_2b() {

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        accountService.addAccount(account);

        BillDTO billDTO = new BillDTO();
        billDTO.setIssueDate(null);
        billDTO.setDueDate(null);
        billDTO.setAccountId(account.getId());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost("http://localhost:8182/bills");
            String accountJson = objectMapper.writeValueAsString(billDTO);

            HttpEntity entity = new StringEntity(accountJson);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);

            String entityString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(entityString,JsonResponseMessage.class);

        } catch (IOException e) {
            fail("Not expected.");
        }

        assertEquals("Bill entity validation error.", jsonResponseMessage.getMessage());
        assertEquals(400, jsonResponseMessage.getStatusCode());
    }

    /**
     * Attempting to add a bill with a missing account id.
     */
    @Test
    public void testAddBillEndpoint_2c() {

        JsonResponseMessage jsonResponseMessage = null;
        BillDTO billDTO = new BillDTO();
        billDTO.setAccountId(0);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost("http://localhost:8182/bills");
            String accountJson = objectMapper.writeValueAsString(billDTO);

            HttpEntity entity = new StringEntity(accountJson);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);

            String entityString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(entityString,JsonResponseMessage.class);

        } catch (IOException e) {
            fail("Not expected.");
        }

        assertEquals("Missing account id.", jsonResponseMessage.getMessage());
        assertEquals(400, jsonResponseMessage.getStatusCode());
    }

    /**
     * Happy flow for deleting a bill. The account id is given as parameter.
     * The expected result is the record no longer being in the database.
     */
    @Test
    public void testDeleteBillEndpoint_1() throws ParseException {

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        Bill bill = createBillWithNoId();

        account.addBill(bill);
        accountService.addAccount(account);
        int id = bill.getId();

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpDelete httpDelete = new HttpDelete("http://localhost:8182/bills/"+id);
            CloseableHttpResponse response = httpClient.execute(httpDelete);
            String responseString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(responseString,JsonResponseMessage.class);

            assertEquals(200,response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotEquals(0, id);
        assertEquals("Operation successful.", jsonResponseMessage.getMessage());

        boolean exceptionThrown = false;

        try{
            billService.getBillById(id);

        } catch (ServiceException e){
            assertEquals("No result.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Attempting to delete a bill without giving an id as parameter. This operation requires an id. This prompts
     * an "Missing or invalid parameter." error message with the 400 status code.
     */
    @Test
    public void testDeleteBillEndpoint_2a() throws ParseException {

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        Bill bill = createBillWithNoId();

        account.addBill(bill);
        accountService.addAccount(account);

        assertNotEquals(0, bill.getId());

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpDelete httpDelete = new HttpDelete("http://localhost:8182/bills");
            CloseableHttpResponse response = httpClient.execute(httpDelete);
            String responseString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(responseString,JsonResponseMessage.class);

            assertEquals(400,response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("Missing or invalid parameter.", jsonResponseMessage.getMessage());
        assertEquals(400, jsonResponseMessage.getStatusCode());
    }

    /**
     * Attempting to delete a bill  without giving a valid id as parameter. The bill cannot be found by the given id, to be deleted. This prompts
     * an "Bill not found." error message with the 404 status code.
     */
    @Test
    public void testDeleteBillEndpoint_2b() throws ParseException {

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        Bill bill = createBillWithNoId();

        account.addBill(bill);
        accountService.addAccount(account);

        assertNotEquals(0, bill.getId());

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpDelete httpDelete = new HttpDelete("http://localhost:8182/bills/-1");
            CloseableHttpResponse response = httpClient.execute(httpDelete);
            String responseString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(responseString,JsonResponseMessage.class);

            assertEquals(404,response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("Bill not found.", jsonResponseMessage.getMessage());
        assertEquals(404, jsonResponseMessage.getStatusCode());
    }

    /**
     * Happy flow for updating a bill. The given bill data is converted into JSON format
     * and then passed in the request body. The added bill is retrieved and updated and then it's
     * new values are checked.
     */
    @Test
    public void testUpdateBillEndpoint_1() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account account = createAccount();
        Bill bill = createBillWithNoId();

        account.addBill(bill);
        accountService.addAccount(account);

        assertNotEquals(0,bill.getId());

        BillDTO billDTO = billService.getBillById(bill.getId());
        billDTO.setDueDate(dateFormat.parse("01/01/1998"));
        billDTO.setIssueDate(dateFormat.parse("01/01/1998"));

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpPut httpPut = new HttpPut("http://localhost:8182/bills");
            String billJson = objectMapper.writeValueAsString(billDTO);

            HttpEntity entity = new StringEntity(billJson);
            httpPut.setEntity(entity);
            httpPut.setHeader("Accept", "application/json");
            httpPut.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPut);
            String responseString = entityContentToString(response.getEntity());
            JsonResponseMessage jsonResponseMessage = objectMapper.readValue(responseString,JsonResponseMessage.class);

            assertEquals("Operation successful.", jsonResponseMessage.getMessage());
            assertEquals(200,response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            e.printStackTrace();
        }

        BillDTO updatedBillDTO = billService.getBillById(bill.getId());

        assertEquals(dateFormat.parse("01/01/1998"),updatedBillDTO.getIssueDate());
        assertEquals(dateFormat.parse("01/01/1998"),updatedBillDTO.getDueDate());
    }
    /**
     * Passing a null object to the PUT method. This prompts a "Error processing request. The bill entity cannot be null." error message
     * with the 400 status code.
     */
    @Test
    public void testUpdateBillEndpoint_2a() {

        JsonResponseMessage jsonResponseMessage = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPut httpPut = new HttpPut("http://localhost:8182/bills");
            String billJson = objectMapper.writeValueAsString(null);

            HttpEntity entity = new StringEntity(billJson);
            httpPut.setEntity(entity);
            httpPut.setHeader("Accept", "application/json");
            httpPut.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPut);
            String entityString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(entityString,JsonResponseMessage.class);

            assertEquals(400,response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("Error processing request. The bill entity cannot be null.", jsonResponseMessage.getMessage());
        assertEquals(400 , jsonResponseMessage.getStatusCode());
    }

    /**
     * Attempting to update a bill with null values for issueDate and dueDate.
     */
    @Test
    public void testUpdateBillEndpoint_2b() throws ParseException {

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        Bill bill = createBillWithNoId();

        account.addBill(bill);
        accountService.addAccount(account);

        assertNotEquals(0,bill.getId());

        BillDTO billDTO = billService.getBillById(bill.getId());
        billDTO.setDueDate(null);
        billDTO.setIssueDate(null);

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpPut httpPut = new HttpPut("http://localhost:8182/bills");
            String billJson = objectMapper.writeValueAsString(billDTO);

            HttpEntity entity = new StringEntity(billJson);
            httpPut.setEntity(entity);
            httpPut.setHeader("Accept", "application/json");
            httpPut.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPut);
            String responseString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(responseString,JsonResponseMessage.class);

            assertEquals(400,response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("Bill entity validation error.", jsonResponseMessage.getMessage());
        assertEquals(400 , jsonResponseMessage.getStatusCode());
    }

    /**
     * Attempting to update a bill with a non-existing account id.
     */
    @Test
    public void testUpdateBillEndpoint_2c() throws ParseException {

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        Bill bill = createBillWithNoId();

        account.addBill(bill);
        accountService.addAccount(account);

        assertNotEquals(0,bill.getId());

        BillDTO billDTO = billService.getBillById(bill.getId());
        billDTO.setAccountId(0);

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpPut httpPut = new HttpPut("http://localhost:8182/bills");
            String billJson = objectMapper.writeValueAsString(billDTO);

            HttpEntity entity = new StringEntity(billJson);
            httpPut.setEntity(entity);
            httpPut.setHeader("Accept", "application/json");
            httpPut.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPut);
            String responseString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(responseString,JsonResponseMessage.class);

            assertEquals(400,response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("Missing account id.", jsonResponseMessage.getMessage());
        assertEquals(400 , jsonResponseMessage.getStatusCode());
    }

    /**
     * Happy flow for grabbing the list of Amount | Month values.
     */
    @Test
    public void testGetMonthlyChargedAmount() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        List<MonthlyAmountDTO> monthlyAmountDTOList = null;
        Account account = createAccount();

        Bill bill1 = createBillWithNoId();
        bill1.setIssueDate(dateFormat.parse("31/10/1998"));
        bill1.addBillCharge(createBillChargeWithNoId());

        Bill bill2 = createBillWithNoId();
        bill2.addBillCharge(createBillChargeWithNoId());

        Bill bill3 = createBillWithNoId();
        bill3.addBillCharge(createBillChargeWithNoId());

        account.addBill(bill1);
        account.addBill(bill2);
        account.addBill(bill3);
        accountService.addAccount(account);

        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet("http://localhost:8182/analysis?op=monthly_amount");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String entityString = entityContentToString(entity);
            monthlyAmountDTOList = Arrays.asList(objectMapper.readValue(entityString, MonthlyAmountDTO[].class));

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(20,monthlyAmountDTOList.get(0).getAmount(),0);
        assertEquals("OCTOBER",monthlyAmountDTOList.get(0).getMonthName());
        assertEquals(40,monthlyAmountDTOList.get(1).getAmount(),0);
        assertEquals("DECEMBER",monthlyAmountDTOList.get(1).getMonthName());

    }

    private List<Bill> populateBillTable() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account account = createAccount();

        for (int i = 0 ; i < 3 ; i++){

            Bill bill = new Bill();
            bill.setIssueDate(dateFormat.parse("31/12/1998"));
            bill.setDueDate(dateFormat.parse("31/12/1998"));

            account.addBill(bill);
        }

        accountService.addAccount(account);

        return account.getBills();
    }

    private Account createAccount(){

        Account account = new Account();
        account.setFirstName("TestFirstName");
        account.setLastName("TestLastName");

        return account;
    }

    private Bill createBillWithNoId() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Bill bill = new Bill();
        bill.setIssueDate(dateFormat.parse("31/12/1998"));
        bill.setDueDate(dateFormat.parse("31/12/1998"));

        return bill;
    }

    private BillCharge createBillChargeWithNoId(){

        BillCharge billCharge = new BillCharge();
        billCharge.setChargeType("Test");
        billCharge.setTax(10f);
        billCharge.setAmount(10f);

        return billCharge;
    }

    private String entityContentToString(HttpEntity entity){
        String result = null;

        try{
            if(entity != null){

                InputStream instream = entity.getContent();

                byte[] bytes = IOUtils.toByteArray(instream);

                result = new String(bytes, "UTF-8");

                instream.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return result;
    }

    private boolean compareBillListValues(List<Bill> billList, List<BillDTO> billDTOList){

        if(billList.size() == billDTOList.size()){

            for (int i = 0; i < billDTOList.size(); i++){

                if(billList.get(i).getId() != billDTOList.get(i).getId() ||
                        !billList.get(i).getIssueDate().equals(billDTOList.get(i).getDueDate())||
                        !billList.get(i).getDueDate().equals(billDTOList.get(i).getDueDate())) {
                    return false;
                }
            }
        } else{
            return false;
        }

        return true;
    }
}
