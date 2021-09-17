package net.gsdgroup.tests.account;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.gsdgroup.billing.business.AccountService;
import net.gsdgroup.billing.business.JsonResponseMessage;
import net.gsdgroup.billing.entity.Account;
import net.gsdgroup.billing.entity.Bill;
import net.gsdgroup.billing.exceptions.ServiceException;
import net.gsdgroup.billing.webservice.accountDTO.AccountDTO;
import net.gsdgroup.billing.webservice.accountDTO.SimpleAccountDTO;
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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.restlet.Component;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//TODO javadoc

/**
 * Test class for the web service component.
 */
public class AccountWebServiceTests {

    private static AccountService accountService;
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
        sessionFactory = context.getBean(SessionFactory.class);
        objectMapper = new ObjectMapper();

    }

    @Test
    public void assertServicesNotNull(){

        assertNotNull(accountService);
        assertNotNull(sessionFactory);
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

    private List<Account> populateAccountTable(){

        List<Account> accounts = new ArrayList<>();

        for (int i = 0; i < 3; i++){

            Account account = new Account();
            account.setFirstName("TestFirstName"+i);
            account.setLastName("TestLastName"+i);

            accountService.addAccount(account);
            accounts.add(account);
        }
        return accounts;
    }

    /**
     * Happy flow for accessing the endpoint to get all of the accounts.
     */
    @Test
    public void testGetAllAccountsEndpoint(){

        List<Account> accountList = populateAccountTable();
        List<AccountDTO> accountDTOList = null;

        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet("http://localhost:8182/accounts");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String entityString = entityContentToString(entity);
            accountDTOList = Arrays.asList(objectMapper.readValue(entityString,AccountDTO[].class));

        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean listMatch = compareListValues(accountList,accountDTOList);

        assertEquals(3,accountDTOList.size());
        assertEquals(true,listMatch);
    }

    private boolean compareListValues(List<Account> accountList, List<AccountDTO> accountDTOList){

        if(accountList.size() == accountDTOList.size()){

            for (int i = 0; i < accountList.size(); i++){

                if(accountList.get(i).getId() != accountDTOList.get(i).getId() ||
                        !accountList.get(i).getFirstName().equals(accountDTOList.get(i).getFirstName()) ||
                        !accountList.get(i).getLastName().equals(accountDTOList.get(i).getLastName())){
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Happy flow for getting one account using an id given as parameter in the URI.
     */
    @Test
    public void testGetAccountByIdEndpoint_1(){

        Account account = createAccount();
        AccountDTO accountDTO = null;
        int id = accountService.addAccount(account);

        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet("http://localhost:8182/accounts/"+id);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            accountDTO = objectMapper.readValue(entityContentToString(entity),AccountDTO.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(accountDTO);
        assertEquals(id,accountDTO.getId());
        assertEquals("TestFirstName",accountDTO.getFirstName());
        assertEquals("TestLastName",accountDTO.getLastName());
    }

    /**
     * The id given as parameter no longer exists or doesn't exist in the database. This prompts the "No result." error message
     * with the 404 status code.
     */
    @Test
    public void testGetAccountByIdEndpoint_2a(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        assertNotEquals(0,id);

        JsonResponseMessage jsonResponseMessage = null;
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet("http://localhost:8182/accounts/-1");
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
    public void testGetAccountByIdEndpoint_2b(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        assertNotEquals(0,id);

        JsonResponseMessage jsonResponseMessage = null;
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet("http://localhost:8182/accounts/abc");
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
     * Happy flow for adding an account. The given account data is converted into JSON format
     * and then passed in the request body.
     */
    @Test
    public void testAddAccountEndpoint_1(){

        int accountId = 0;
        SimpleAccountDTO simpleAccountDTO = new SimpleAccountDTO();
        simpleAccountDTO.setFirstName("TestFirstName");
        simpleAccountDTO.setLastName("TestLastName");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost("http://localhost:8182/accounts");
            String accountJson = objectMapper.writeValueAsString(simpleAccountDTO);

            HttpEntity entity = new StringEntity(accountJson);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);

            assertEquals(200,response.getStatusLine().getStatusCode());

            String entityString = entityContentToString(response.getEntity());
            JsonResponseMessage jsonResponseMessage = objectMapper.readValue(entityString,JsonResponseMessage.class);
            accountId = Integer.parseInt(jsonResponseMessage.getMessage());

        } catch (IOException e) {
            fail("Not expected.");
        }

        assertNotEquals(0, accountId);

        AccountDTO accountDTO = accountService.getAccountById(accountId);

        assertEquals(accountId, accountDTO.getId());
        assertEquals("TestFirstName", accountDTO.getFirstName());
        assertEquals("TestLastName", accountDTO.getLastName());
    }

    /**
     * Passing a null object to the POST method. This prompts the "Error processing request. The account entity cannot be null."
     * error message with the 400 status code.
     */
    @Test
    public void testAddAccountEndpoint_2a(){

        JsonResponseMessage jsonResponseMessage = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost("http://localhost:8182/accounts");
            String accountJson = objectMapper.writeValueAsString(null);

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

        assertEquals("Error processing request. The account entity cannot be null.", jsonResponseMessage.getMessage());
        assertEquals(400, jsonResponseMessage.getStatusCode());
    }

    /**
     * Passing an object with null values to the POST method. This prompts the "Entity validation error."
     * error message with the 400 status code.
     */
    @Test
    public void testAddAccountEndpoint_2b(){

        JsonResponseMessage jsonResponseMessage = null;
        SimpleAccountDTO simpleAccountDTO = new SimpleAccountDTO();
        simpleAccountDTO.setFirstName(null);
        simpleAccountDTO.setLastName(null);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost("http://localhost:8182/accounts");
            String accountJson = objectMapper.writeValueAsString(simpleAccountDTO);

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

        assertEquals("Account entity validation error.", jsonResponseMessage.getMessage());
        assertEquals(400, jsonResponseMessage.getStatusCode());
    }

    /**
     * Happy flow for updating an account. The given account data is converted into JSON format
     * and then passed in the request body. The added account is retrieved and updated and then it's
     * new values are checked.
     */
    @Test
    public void testAccountUpdateEndpoint_1(){

        Account account = createAccount();

        int id = accountService.addAccount(account);

        AccountDTO accountDTO = accountService.getAccountById(id);
        accountDTO.setFirstName("TestFirstNameUpdated");
        accountDTO.setLastName("TestLastNameUpdated");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPut httpPut = new HttpPut("http://localhost:8182/accounts");//TODO endpoint change
            String accountJson = objectMapper.writeValueAsString(accountDTO);

            HttpEntity entity = new StringEntity(accountJson);
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

        AccountDTO updatedAccountDTO = accountService.getAccountById(id);

        assertEquals("TestFirstNameUpdated",updatedAccountDTO.getFirstName());
        assertEquals("TestLastNameUpdated",updatedAccountDTO.getLastName());
    }

    /**
     * Passing a null object to the PUT method. This prompts a "Error processing request. The account entity cannot be null." error message
     * with the 400 status code.
     */
    @Test
    public void testAccountUpdateEndpoint_2a(){

        JsonResponseMessage jsonResponseMessage = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPut httpPut = new HttpPut("http://localhost:8182/accounts");
            String accountJson = objectMapper.writeValueAsString(null);

            HttpEntity entity = new StringEntity(accountJson);
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

        assertEquals("Error processing request. The account entity cannot be null.", jsonResponseMessage.getMessage());
        assertEquals(400 , jsonResponseMessage.getStatusCode());
    }

    /**
     * Attempting to update an account with a null value on the firstName property.
     */
    @Test
    public void testAccountUpdateEndpoint_2b(){

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        int id = accountService.addAccount(account);

        AccountDTO accountDTO = accountService.getAccountById(id);
        accountDTO.setFirstName(null);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPut httpPut = new HttpPut("http://localhost:8182/accounts");
            String accountJson = objectMapper.writeValueAsString(accountDTO);

            HttpEntity entity = new StringEntity(accountJson);
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

        assertEquals("Account entity validation error.", jsonResponseMessage.getMessage());
        assertEquals(400 , jsonResponseMessage.getStatusCode());
    }

    /**
     * Attempting to delete an account with a null value on lastName property.
     */
    @Test
    public void testAccountUpdateEndpoint_2c(){

        JsonResponseMessage jsonResponseMessage = null;
        Account account = createAccount();
        int id = accountService.addAccount(account);

        AccountDTO accountDTO = accountService.getAccountById(id);
        accountDTO.setLastName(null);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPut httpPut = new HttpPut("http://localhost:8182/accounts");
            String accountJson = objectMapper.writeValueAsString(accountDTO);

            HttpEntity entity = new StringEntity(accountJson);
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

        assertEquals("Account entity validation error.", jsonResponseMessage.getMessage());
        assertEquals(400 , jsonResponseMessage.getStatusCode());
    }

    /**
     * Happy flow for deleting an account. The account id is given as parameter.
     * The expected result is the record no longer being in the database.
     */
    @Test
    public void testAccountDeleteEndpoint_1(){

        Account account = createAccount();
        int id = accountService.addAccount(account);
        JsonResponseMessage jsonResponseMessage = null;

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpDelete httpDelete = new HttpDelete("http://localhost:8182/accounts/"+id);
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
            accountService.getAccountById(id);

        } catch (ServiceException e){
            assertEquals("No result.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Attempting to delete an account without giving an id as parameter. This operation requires an id. This prompts
     * an "Missing or invalid parameter." error message with the 400 status code.
     */
    @Test
    public void testAccountDeleteEndpoint_2a(){

        Account account = createAccount();
        int id = accountService.addAccount(account);
        JsonResponseMessage jsonResponseMessage = null;

        assertNotEquals(0,id);

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpDelete httpDelete = new HttpDelete("http://localhost:8182/accounts");
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
     * Attempting to delete an account without giving a valid id as parameter. The account cannot be found by the given id, to be deleted. This prompts
     * an "Account not found." error message with the 404 status code.
     */
    @Test
    public void testAccountDeleteEndpoint_2b(){

        Account account = createAccount();
        int id = accountService.addAccount(account);
        JsonResponseMessage jsonResponseMessage = null;

        assertNotEquals(0,id);

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){

            HttpDelete httpDelete = new HttpDelete("http://localhost:8182/accounts/-1");
            CloseableHttpResponse response = httpClient.execute(httpDelete);
            String responseString = entityContentToString(response.getEntity());
            jsonResponseMessage = objectMapper.readValue(responseString,JsonResponseMessage.class);

            assertEquals(404,response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("Account not found.", jsonResponseMessage.getMessage());
        assertEquals(404, jsonResponseMessage.getStatusCode());
    }

    /**
     * Getting all the accounts who have overdue bills. This method populates the table with
     * normal accounts and overdue ones.
     * @throws ParseException
     */
    @Test
    public void testGetAllAccountsWithOverdueBills() throws ParseException {

        populateAccountTable();
        List<Account> accountList = new ArrayList<>();

        Account accountWithOverdueBills1 = createAccountWithOverdueBills();
        accountService.addAccount(accountWithOverdueBills1);
        accountList.add(accountWithOverdueBills1);

        Account accountWithOverdueBills2 = createAccountWithOverdueBills();
        accountService.addAccount(accountWithOverdueBills2);
        accountList.add(accountWithOverdueBills2);

        List<AccountDTO> overdueAccountDTOList = null;

        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet("http://localhost:8182/analysis/accounts?op=overdue_bills");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String entityString = entityContentToString(entity);
            overdueAccountDTOList = Arrays.asList(objectMapper.readValue(entityString,AccountDTO[].class));

        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean listMatch = compareListValues(accountList,overdueAccountDTOList);

        assertEquals(2, overdueAccountDTOList.size());
        assertEquals(true,listMatch);
    }

    private Account createAccountWithOverdueBills() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Account account = createAccount();
        Bill bill = new Bill();

        bill.setIssueDate(dateFormat.parse("2021-03-15 23:59:59"));
        bill.setDueDate(dateFormat.parse("2021-03-15 23:59:59"));
        account.addBill(bill);

        return account;
    }

    private Account createAccount(){
        Account account = new Account();
        account.setFirstName("TestFirstName");
        account.setLastName("TestLastName");

        return account;
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
}
