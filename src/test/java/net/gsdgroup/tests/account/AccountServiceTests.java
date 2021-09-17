package net.gsdgroup.tests.account;

import net.gsdgroup.billing.business.AccountService;
import net.gsdgroup.billing.entity.Account;
import net.gsdgroup.billing.entity.Bill;
import net.gsdgroup.billing.entity.BillCharge;
import net.gsdgroup.billing.exceptions.ServiceException;
import net.gsdgroup.billing.webservice.accountDTO.AccountDTO;
import net.gsdgroup.billing.webservice.accountDTO.SimpleAccountDTO;
import net.gsdgroup.billing.webservice.billDTO.BillChargeDTO;
import net.gsdgroup.billing.webservice.billDTO.BillDTO;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.persistence.NoResultException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

//TODO Javadoc and comments.
/**
 * Service test class for the account business component.
 */

public class AccountServiceTests {

    private static AccountService accountService;
    private static SessionFactory sessionFactory;

    /**
     * Starts the Spring context. And fetches all the beans necessary.
     */
    @BeforeClass
    public static void testInit() {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        accountService = context.getBean(AccountService.class);
        sessionFactory = context.getBean(SessionFactory.class);
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
     * Checks if the beans have been initialized.
     */
    @Test
    public void assertServicesNotNull(){

        assertNotNull(accountService);
        assertNotNull(sessionFactory);
    }

    /**
     * Tests the AccountDTO creation. It first builds the accountEntity used as reference in building the DTO.
     * Then each value from the created DTO is tested to match the one in the entity.
     * @throws ParseException
     */
    @Test
    public void testAccountDTOBuild() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account accountEntity = createAccount();
        accountEntity.setId(1);
        accountEntity.setBills(createBills(accountEntity));

        AccountDTO accountDTO = accountService.buildAccountDto(accountEntity);

        assertEquals(1,accountDTO.getId());
        assertEquals("TestFirstName", accountDTO.getFirstName());
        assertEquals("TestLastName", accountDTO.getLastName());
        assertEquals(1,accountDTO.getBills().size());

        BillDTO billDTO = accountDTO.getBills().get(0);

        assertEquals(1,billDTO.getId());
        assertEquals(dateFormat.parse("31/12/1998"),billDTO.getIssueDate());
        assertEquals(dateFormat.parse("31/12/1998"),billDTO.getDueDate());
        assertEquals(1,billDTO.getBillCharges().size());
        assertEquals(accountDTO, billDTO.getAccount());

        BillChargeDTO billChargeDTO = billDTO.getBillCharges().get(0);

        assertEquals(1,billChargeDTO.getId());
        assertEquals("Test",billChargeDTO.getChargeType());
        assertEquals(10f,billChargeDTO.getTax(),0.0f);
        assertEquals(10f,billChargeDTO.getAmount(),0.0f);
        assertEquals(billDTO, billChargeDTO.getBill());
    }

    /**
     * Tests the Account entity creation. This test uses a SimpleAccountDTO as parameter for the build method.
     * This creates an entity without bills and bill charges.
     */
    @Test
    public void testAccountEntityBuild_1a(){

        SimpleAccountDTO simpleAccountDTO = new SimpleAccountDTO();
        simpleAccountDTO.setId(1);
        simpleAccountDTO.setFirstName("TestFirstName");
        simpleAccountDTO.setLastName("TestLastName");

        Account account = accountService.buildAccountEntity(simpleAccountDTO);

        assertEquals(1,account.getId());
        assertEquals("TestFirstName",account.getFirstName());
        assertEquals("TestLastName", account.getLastName());
    }

    /**
     * Tests the Account entity creation. This test uses a AccountDTO as parameter for the build method.
     * This creates an entity with bills and bill charges. This is a complete entity with no missing data.
     */
    @Test
    public void testAccountEntityBuild_1b() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId(1);
        accountDTO.setFirstName("TestFirstName");
        accountDTO.setLastName("TestLastName");
        accountDTO.setBills(createBillsDTO(accountDTO));

        Account accountEntity = accountService.buildAccountEntity(accountDTO);

        assertEquals(1,accountEntity.getId());
        assertEquals("TestFirstName", accountEntity.getFirstName());
        assertEquals("TestLastName", accountEntity.getLastName());
        assertEquals(1,accountEntity.getBills().size());

        Bill billEntity = accountEntity.getBills().get(0);

        assertEquals(1,billEntity.getId());
        assertEquals(dateFormat.parse("31/12/1998"),billEntity.getIssueDate());
        assertEquals(dateFormat.parse("31/12/1998"),billEntity.getDueDate());
        assertEquals(1,billEntity.getBillCharges().size());
        assertEquals(accountEntity, billEntity.getAccount());

        BillCharge billChargeEntity = billEntity.getBillCharges().get(0);

        assertEquals(1,billChargeEntity.getId());
        assertEquals("Test",billChargeEntity.getChargeType());
        assertEquals(10f,billChargeEntity.getTax(),0.0f);
        assertEquals(10f,billChargeEntity.getAmount(),0.0f);
        assertEquals(billEntity, billChargeEntity.getBill());

    }

    /**
     * Create and add a valid account. Happy flow for account creation,
     * resulting in a record in the database with a valid id.
     */
    @Test
    public void testAddSimpleAccount_1(){

        Account account = createAccount();

        assertEquals(0,account.getId());

        int id = accountService.addAccount(account);
        //TODO check returned account values
        assertNotEquals(0, id);
    }

    /**
     * Account parameter for adding an account is null.
     * This will throw a ServiceException.
     */
    @Test
    public void testAddSimpleAccount_2a(){

        int id = 0;
        boolean exceptionThrown = false;

        try{
            id = accountService.addAccount(null);

        } catch (ServiceException e){
            assertEquals("Error processing request. The account entity cannot be null.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }

        assertEquals(0, id);
    }

    /**
     * Account is initialized but the non-null values are null. This will throw a ServiceException
     */
    @Test
    public void testAddSimpleAccount_2b(){

        Account account = new Account();
        account.setFirstName(null);
        account.setLastName(null);

        assertEquals(0,account.getId());
        boolean exceptionThrown = false;

        try{
            accountService.addAccount(account);

        } catch (ServiceException e){
            assertEquals("Account entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Account is initialized but the lastName property is assigned as null . This will throw a ServiceException
     */
    @Test
    public void testAddSimpleAccount_2c(){

        Account account = new Account();
        account.setFirstName("TestFirstName");
        account.setLastName(null);

        assertEquals(0,account.getId());

        boolean exceptionThrown = false;

        try{
            accountService.addAccount(account);

        } catch (ServiceException e){
            assertEquals("Account entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Account is initialized but the firstName property is assigned as null . This will throw a ServiceException
     */
    @Test
    public void testAddSimpleAccount_2d(){

        Account account = new Account();
        account.setFirstName(null);
        account.setLastName("TestLastName");

        assertEquals(0,account.getId());

        boolean exceptionThrown = false;

        try{
            accountService.addAccount(account);

        } catch (ServiceException e){
            assertEquals("Account entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Adding an account with a predefined id. This violates the auto-increment rule set for id. Throws ServiceException.
     */
    @Test(expected = ServiceException.class)
    public void testAddSimpleAccount_3(){

        Account account = createAccount();
        account.setId(3);//TODO find out why this doesn't throw exception.

        accountService.addAccount(account);

    }

    /**
     * Happy flow for account deletion. After the account is deleted it will attempt to retrieve it.
     * This operation will fail resulting in a thrown ServiceException. This is the expected result
     * due to the account no longer existing in the database.
     */
    @Test
    public void testDeleteAccount_1(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        AccountDTO fetchedAccount = accountService.getAccountById(id);
        assertEquals(id, fetchedAccount.getId());
        assertEquals("TestFirstName", fetchedAccount.getFirstName());
        assertEquals("TestLastName", fetchedAccount.getLastName());

        accountService.deleteAccount(id);

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
     * Attempts to delete an account with an invalid id.
     */
    @Test
    public void testDeleteAccount_2(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        AccountDTO fetchedAccount = accountService.getAccountById(id);
        assertEquals(id, fetchedAccount.getId());
        assertEquals("TestFirstName", fetchedAccount.getFirstName());
        assertEquals("TestLastName", fetchedAccount.getLastName());

        boolean exceptionThrown = false;

        try{
            accountService.deleteAccount(-1);

        } catch (ServiceException e){
            assertEquals("Account not found.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Adds a number of accounts (3 in this case) and tests if the number of accounts
     * retrieved matches.
     */
    @Test
    public void testGetAllAccount(){

        List<Account> accountList = populateAccountTable();
        List<AccountDTO> accountDTOList = accountService.getAllAccounts();

        boolean listMatch = compareAccountListValues(accountList,accountDTOList);

        assertEquals(3,accountDTOList.size());
        assertEquals(true,listMatch);
    }

    /**
     * Happy flow for updating an account. For a retrieved account a value is updated.
     *
     */
    @Test
    public void testUpdateAccount_1(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        AccountDTO accountDTO = accountService.getAccountById(id);
        accountDTO.setLastName("TestLastNameUpdated");

        Account accountEntity = accountService.buildAccountEntity(accountDTO);
        accountService.updateAccount(accountEntity);

        AccountDTO updatedAccountDTO = accountService.getAccountById(id);

        assertEquals("TestLastNameUpdated", updatedAccountDTO.getLastName());
    }

    /**
     * Attempting to update an account, setting it's properties as null values. This operation fails due to a ServiceException.
     */
    @Test
    public void testUpdateAccount_2a(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        AccountDTO accountDTO = accountService.getAccountById(id);
        accountDTO.setLastName(null);
        accountDTO.setFirstName(null);

        Account accountEntity = accountService.buildAccountEntity(accountDTO);
        boolean exceptionThrown = false;

        try{
            accountService.updateAccount(accountEntity);
        } catch (ServiceException e){
            assertEquals("Account entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Attempting to update an account with a null value on the lastName property. This operation fails due to a ServiceException.
     */
    @Test
    public void testUpdateAccount_2b(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        AccountDTO accountDTO = accountService.getAccountById(id);
        accountDTO.setLastName(null);

        Account accountEntity = accountService.buildAccountEntity(accountDTO);
        boolean exceptionThrown = false;

        try{
            accountService.updateAccount(accountEntity);
        } catch (ServiceException e){
            assertEquals("Account entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Attempting to update an account with a null value on the firstName property. This operation fails due to a ServiceException.
     */
    @Test
    public void testUpdateAccount_2c(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        AccountDTO accountDTO = accountService.getAccountById(id);
        accountDTO.setFirstName(null);

        Account accountEntity = accountService.buildAccountEntity(accountDTO);
        boolean exceptionThrown = false;

        try{
            accountService.updateAccount(accountEntity);
        } catch (ServiceException e){
            assertEquals("Account entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Attemps to execute the update method on a null object.
     */
    @Test
    public void testUpdateAccount_2d(){

        boolean exceptionThrown = false;

        try{
            accountService.updateAccount(null);
        } catch (ServiceException e){
            assertEquals("Error processing request. The account entity cannot be null.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }

    }

    /**
     * Get an account by an id. The expected result is an AccountDTO with all the data
     * regarding the account.
     */
    @Test
    public void testGetAccountById_1(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        AccountDTO accountDTO = accountService.getAccountById(id);

        assertNotNull(accountDTO);
        assertEquals(id, accountDTO.getId());
        assertEquals("TestFirstName", accountDTO.getFirstName());
        assertEquals("TestLastName", accountDTO.getLastName());
    }

    /**
     * Get an account by an invalid id. In this case the given id cannot be found.
     */
    @Test
    public void testGetAccountById_2(){

        Account account = createAccount();
        int id = accountService.addAccount(account);

        assertNotEquals(0, id);

        boolean exceptionThrown = false;

        try{
            accountService.getAccountById(-1);
        } catch (ServiceException e){
            assertEquals("No result.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail();
        }
    }

    /**
     * Happy flow for getting the accounts with overdue bills service. The account list contains 4 accounts in which 2 have overdue bills.
     * @throws ParseException
     */
    @Test
    public void testGetAccountsWithOverdueBills_1() throws ParseException {

        Account accountWithOverdueBills1 = createAccountWithOverdueBills();
        Account accountWithOverdueBills2 = createAccountWithOverdueBills();
        Account account1 = createAccount();
        Account account2 = createAccount();

        accountService.addAccount(account1);
        accountService.addAccount(account2);
        accountService.addAccount(accountWithOverdueBills1);
        accountService.addAccount(accountWithOverdueBills2);

        List<AccountDTO> accountDTOList = accountService.getAccountsWithOverdueBills();

        assertEquals(2,accountDTOList.size());
    }

    /**
     * Tests the value from the object to assure the accurate result.
     * @throws ParseException
     */
    @Test
    public void testGetAccountsWithOverdueBills_2a() throws ParseException {

        Account accountWithOverdueBills = createAccountWithOverdueBills();

        accountService.addAccount(accountWithOverdueBills);
        List<AccountDTO> accountDTOList = accountService.getAccountsWithOverdueBills();

        AccountDTO accountDTO = accountDTOList.get(0);
        BillDTO billDTO = accountDTO.getBills().get(0);
        Date currentDate = new Date();

        assertTrue(billDTO.getDueDate().compareTo(currentDate) < 0);
    }

    /**
     * Checking if the method falsely finds accounts when there are no accounts with overdue bills.
     */
    @Test
    public void testGetAccountsWithOverdueBills_2b(){

        for (int i = 0; i < 3 ; i++){

            Account account = createAccount();
            accountService.addAccount(account);
        }

        List<AccountDTO> accountDTOList = accountService.getAccountsWithOverdueBills();

        assertEquals(0,accountDTOList.size());
    }

    private Account createAccount(){

        Account account = new Account();
        account.setFirstName("TestFirstName");
        account.setLastName("TestLastName");

        return account;
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

    private List<Bill> createBills(Account account) throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Bill bill = new Bill();
        bill.setId(1);
        bill.setIssueDate(dateFormat.parse("31/12/1998"));
        bill.setDueDate(dateFormat.parse("31/12/1998"));
        bill.setAccount(account);

        bill.setBillCharges(createBillCharges(bill));

        List<Bill> bills = new ArrayList<>();
        bills.add(bill);

        return bills;
    }

    private List<BillCharge> createBillCharges(Bill bill) {

        BillCharge billCharge = new BillCharge();
        billCharge.setId(1);
        billCharge.setChargeType("Test");
        billCharge.setTax(10f);
        billCharge.setAmount(10f);
        billCharge.setBill(bill);

        List<BillCharge> billCharges = new ArrayList<>();
        billCharges.add(billCharge);

        return billCharges;
    }

    private List<BillDTO> createBillsDTO (AccountDTO accountDTO) throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        BillDTO billDTO = new BillDTO();
        billDTO.setId(1);
        billDTO.setIssueDate(dateFormat.parse("31/12/1998"));
        billDTO.setDueDate(dateFormat.parse("31/12/1998"));
        billDTO.setAccount(accountDTO);

        billDTO.setBillCharges(createBillChargesDTO(billDTO));

        List<BillDTO> billsDTO = new ArrayList<>();
        billsDTO.add(billDTO);

        return billsDTO;
    }

    private List<BillChargeDTO> createBillChargesDTO(BillDTO billDTO){

        BillChargeDTO billChargeDTO = new BillChargeDTO();
        billChargeDTO.setId(1);
        billChargeDTO.setChargeType("Test");
        billChargeDTO.setTax(10f);
        billChargeDTO.setAmount(10f);
        billChargeDTO.setBill(billDTO);

        List<BillChargeDTO> billChargesDTO = new ArrayList<>();
        billChargesDTO.add(billChargeDTO);

        return billChargesDTO;
    }

    private boolean compareAccountListValues(List<Account> accountList, List<AccountDTO> accountDTOList){

        if( accountList.size() == accountDTOList.size()){

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
}
