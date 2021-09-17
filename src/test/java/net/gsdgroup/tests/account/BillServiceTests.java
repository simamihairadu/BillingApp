package net.gsdgroup.tests.account;

import net.gsdgroup.billing.business.AccountService;
import net.gsdgroup.billing.business.BillService;
import net.gsdgroup.billing.entity.Account;
import net.gsdgroup.billing.entity.Bill;
import net.gsdgroup.billing.entity.BillCharge;
import net.gsdgroup.billing.exceptions.ServiceException;
import net.gsdgroup.billing.webservice.accountDTO.AccountDTO;
import net.gsdgroup.billing.webservice.billDTO.BillChargeDTO;
import net.gsdgroup.billing.webservice.billDTO.BillDTO;
import net.gsdgroup.billing.webservice.billDTO.MonthlyAmountDTO;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Service test class for the bill business component
 */
public class BillServiceTests {

    private static BillService billService;
    private static SessionFactory sessionFactory;
    private static AccountService accountService;

    /**
     * Starts the Spring context. And fetches all the beans necessary.
     * @throws Exception
     */
    @BeforeClass
    public static void testInit() throws Exception {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        billService = context.getBean(BillService.class);
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
        assertNotNull(billService);
        assertNotNull(sessionFactory);
    }

    /**
     * Tests the BillDTO creation. A bill requires an account in order to be created.
     * The method creates an account and attaches it to the billDTO.
     * @throws ParseException
     */
    @Test
    public void testBillDTOBuild() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        Account account = createAccount();
        account.setId(1);

        Bill bill = createBill();
        bill.setAccount(account);

        BillCharge billCharge = createBillCharge();
        bill.addBillCharge(billCharge);

        BillDTO billDTO = billService.buildBillDTO(bill);

        assertEquals(1,billDTO.getId());
        assertEquals(dateFormat.parse("31/12/1998"),billDTO.getIssueDate());
        assertEquals(dateFormat.parse("31/12/1998"),billDTO.getDueDate());
        assertEquals(1,billDTO.getBillCharges().size());

        assertEquals(1, billDTO.getAccount().getId());
        assertEquals("TestFirstName", billDTO.getAccount().getFirstName());
        assertEquals("TestLastName", billDTO.getAccount().getLastName());

        BillChargeDTO billChargeDTO = billDTO.getBillCharges().get(0);

        assertEquals(1,billChargeDTO.getId());
        assertEquals("Test",billChargeDTO.getChargeType());
        assertEquals(10f,billChargeDTO.getTax(),0.0f);
        assertEquals(10f,billChargeDTO.getAmount(),0.0f);
        assertEquals(billDTO, billChargeDTO.getBill());
    }

    /**
     * Building an bill entity using an existing account.
     * @throws ParseException
     */
    @Test
    public void testBillEntityBuild() throws ParseException {

        Account accountEntity = createAccount();
        int id = accountService.addAccount(accountEntity);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        BillDTO billDTO = new BillDTO();
        billDTO.setId(1);
        billDTO.setIssueDate(dateFormat.parse("31/12/1998"));
        billDTO.setDueDate(dateFormat.parse("31/12/1998"));
        AccountDTO accountDTO = accountService.buildAccountDto(accountEntity);
        billDTO.setAccount(accountDTO);
        billDTO.setAccountId(accountDTO.getId());

        Bill billEntity = billService.buildBillEntity(billDTO);

        assertEquals(1, billEntity.getId());
        assertEquals(dateFormat.parse("31/12/1998"),billEntity.getIssueDate());
        assertEquals(dateFormat.parse("31/12/1998"),billEntity.getDueDate());

        assertEquals(id,billEntity.getAccount().getId());
        assertEquals("TestFirstName", billEntity.getAccount().getFirstName());
        assertEquals("TestLastName", billEntity.getAccount().getLastName());
    }

    /**
     * Happy flow for getting a list with all the bills.
     * @throws ParseException
     */
    @Test
    public void testGetAllBills() throws ParseException {

        List<Bill> billList = populateBillTable();
        List<BillDTO> billDTOList = billService.getAllBills();

        boolean listMatch = compareBillListValues(billList,billDTOList);

        assertEquals(billList.size(), billDTOList.size());
        assertEquals(true, listMatch);
    }

    /**
     * Happy flow for getting a bill by using an id. The given id is valid.
     * @throws ParseException
     */
    @Test
    public void testGetBillById_1() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account account = createAccount();
        Bill bill = createBillWithNoId();

        account.addBill(bill);
        accountService.addAccount(account);

        BillDTO billDTO = billService.getBillById(bill.getId());

        assertNotEquals(0, bill.getId());
        assertEquals(bill.getId(), billDTO.getId());
        assertEquals(dateFormat.parse("31/12/1998"),billDTO.getIssueDate());
        assertEquals(dateFormat.parse("31/12/1998"),billDTO.getDueDate());
    }

    /**
     * Attempting to get a bill by using an id that cannot be found.
     * @throws ParseException
     */
    @Test
    public void testGetBillById_2() throws ParseException {

        Account account = createAccount();
        accountService.addAccount(account);
        Bill bill = createBillWithNoId();
        bill.setAccount(account);
        int billId = billService.addBill(bill);

        assertNotEquals(0, billId);

        boolean exceptionThrown = false;

        try{
            billService.getBillById(-1);
        } catch (ServiceException e){
            assertEquals("No result.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Happy flow for adding a bill. This method uses an already created account as reference to satisfy
     * the foreign key constraint when adding the bill.
     * @throws ParseException
     */
    @Test
    public void testAddBill_1() throws ParseException {

        Account account = createAccount();
        accountService.addAccount(account);

        Bill bill = createBillWithNoId();
        bill.setAccount(account);

        int billId = billService.addBill(bill);

        assertNotEquals(0, billId);
    }

    /**
     * Attempting to add a null bill entity.
     */
    @Test
    public void testAddBill_2a() {

        boolean exceptionThrown = false;

        try{
            billService.addBill(null);
        } catch (ServiceException e){
            assertEquals("Error processing request. The bill entity cannot be null.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Bill entity is initialized but its fields are null. This fails the validation.
     */
    @Test
    public void testAddBill_2b() {

        Bill bill = new Bill();
        bill.setIssueDate(null);
        bill.setDueDate(null);
        bill.setAccount(null);

        int billId = 0;
        boolean exceptionThrown = false;

        try{
            billService.addBill(bill);
        } catch (ServiceException e){
            assertEquals("Bill entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }

        assertEquals(0, billId);
    }

    /**
     * Bill entity is initialized but only its issueDate property is not null. This fails the validation.
     * @throws ParseException
     */
    @Test
    public void testAddBill_2c() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        Bill bill = new Bill();
        bill.setIssueDate(dateFormat.parse("31/12/1998"));
        bill.setDueDate(null);
        bill.setAccount(null);

        int billId = 0;
        boolean exceptionThrown = false;

        try{
            billService.addBill(bill);
        } catch (ServiceException e){
            assertEquals("Bill entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }

        assertEquals(0, billId);
    }

    /**
     * Bill entity is initialized but only its dueDate property is not null. This fails the validation.
     * @throws ParseException
     */
    @Test
    public void testAddBill_2d() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        Bill bill = new Bill();
        bill.setIssueDate(null);
        bill.setDueDate(dateFormat.parse("31/12/1998"));
        bill.setAccount(null);

        int billId = 0;
        boolean exceptionThrown = false;

        try{
            billService.addBill(bill);
        } catch (ServiceException e){
            assertEquals("Bill entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }

        assertEquals(0, billId);
    }

    /**
     * Bill entity is initialized but only its account property is not null. This fails the validation.
     */
    @Test
    public void testAddBill_2e() {

        Account account = createAccount();
        accountService.addAccount(account);

        Bill bill = new Bill();
        bill.setIssueDate(null);
        bill.setDueDate(null);
        bill.setAccount(account);

        int billId = 0;
        boolean exceptionThrown = false;

        try{
            billService.addBill(bill);
        } catch (ServiceException e){
            assertEquals("Bill entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }

        assertEquals(0, billId);
    }

    /**
     * Happy flow for deleting a bill. The bill to be deleted is identified by the id passed to the method.
     * @throws ParseException
     */
    @Test
    public void testDeleteBill_1() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account account = createAccount();
        accountService.addAccount(account);

        Bill bill = createBillWithNoId();
        bill.setAccount(account);

        int billId = billService.addBill(bill);
        BillDTO fetchedBill = billService.getBillById(billId);

        assertEquals(billId, fetchedBill.getId());
        assertEquals(dateFormat.parse("31/12/1998"),fetchedBill.getIssueDate());
        assertEquals(dateFormat.parse("31/12/1998"),fetchedBill.getDueDate());

        billService.deleteBill(billId);

        boolean exceptionThrown = false;

        try {
            billService.getBillById(billId);

        } catch (ServiceException e) {
            assertEquals("No result.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown) {
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Attempting to delete account without a valid id. The given id cannot be found.
     * @throws ParseException
     */
    @Test
    public void testDeleteBill_2() throws ParseException {

        Account account = createAccount();
        accountService.addAccount(account);

        Bill bill = createBillWithNoId();
        bill.setAccount(account);
        int billId = billService.addBill(bill);

        assertNotEquals(0, billId);

        boolean exceptionThrown = false;

        try{
            billService.deleteBill(-1);
        } catch (ServiceException e){
            assertEquals("Bill not found.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Happy flow for updating a bill. The bill is retrieved from the database and it's values are updated.
     * @throws ParseException
     */
    @Test
    public void testUpdateBill_1() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account account = createAccount();
        accountService.addAccount(account);

        Bill bill = createBillWithNoId();
        bill.setAccount(account);

        int billId = billService.addBill(bill);
        BillDTO billDTO = billService.getBillById(billId);
        billDTO.setIssueDate(dateFormat.parse("11/03/2020"));
        billDTO.setDueDate(dateFormat.parse("11/03/2020"));

        Bill billEntity = billService.buildBillEntity(billDTO);
        billService.updateBill(billEntity);

        BillDTO updatedBillDTO = billService.getBillById(billId);

        assertEquals(dateFormat.parse("11/03/2020"),updatedBillDTO.getIssueDate());
        assertEquals(dateFormat.parse("11/03/2020"),updatedBillDTO.getDueDate());
    }

    /**
     * Attempting to update a bill with both date values null.
     * @throws ParseException
     */
    @Test
    public void testUpdateBill_2a() throws ParseException {

        Account account = createAccount();
        accountService.addAccount(account);

        Bill bill = createBillWithNoId();
        bill.setAccount(account);

        int billId = billService.addBill(bill);
        BillDTO billDTO = billService.getBillById(billId);
        billDTO.setIssueDate(null);
        billDTO.setDueDate(null);

        Bill billEntity = billService.buildBillEntity(billDTO);
        boolean exceptionThrown = false;

        try{
            billService.updateBill(billEntity);
        } catch (ServiceException e){
            assertEquals("Bill entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Attempt to update a bill setting the issueDate property as null.
     * @throws ParseException
     */
    @Test
    public void testUpdateBill_2b() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account account = createAccount();
        accountService.addAccount(account);

        Bill bill = createBillWithNoId();
        bill.setAccount(account);

        int billId = billService.addBill(bill);
        BillDTO billDTO = billService.getBillById(billId);
        billDTO.setIssueDate(null);
        billDTO.setDueDate(dateFormat.parse("11/03/2020"));

        Bill billEntity = billService.buildBillEntity(billDTO);
        boolean exceptionThrown = false;

        try{
            billService.updateBill(billEntity);
        } catch (ServiceException e){
            assertEquals("Bill entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Attempt to update a bill setting the dueDate property as null.
     * @throws ParseException
     */
    @Test
    public void testUpdateBill_2c() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Account account = createAccount();
        accountService.addAccount(account);

        Bill bill = createBillWithNoId();
        bill.setAccount(account);

        int billId = billService.addBill(bill);
        BillDTO billDTO = billService.getBillById(billId);
        billDTO.setIssueDate(dateFormat.parse("11/03/2020"));
        billDTO.setDueDate(null);

        Bill billEntity = billService.buildBillEntity(billDTO);
        boolean exceptionThrown = false;

        try{
            billService.updateBill(billEntity);
        } catch (ServiceException e){
            assertEquals("Bill entity validation error.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Attempting to update a null bill.
     * @throws ParseException
     */
    @Test
    public void testUpdateBill_2d() {

        boolean exceptionThrown = false;

        try{
            billService.updateBill(null);
        } catch (ServiceException e){
            assertEquals("Error processing request. The bill entity cannot be null.", e.getMessage());
            exceptionThrown = true;
        }

        if(!exceptionThrown){
            fail("Expecting ServiceException.");
        }
    }

    /**
     * Happy flow for getting the total charged each month.
     * @throws ParseException
     */
    @Test
    public void testGetChargedAmountEachMonth_1() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
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

        List<MonthlyAmountDTO> monthlyAmountDTOList = billService.getTotalChargedEachMonth();

        assertEquals(20,monthlyAmountDTOList.get(0).getAmount(),0);
        assertEquals("OCTOBER",monthlyAmountDTOList.get(0).getMonthName());
        assertEquals(40,monthlyAmountDTOList.get(1).getAmount(),0);
        assertEquals("DECEMBER",monthlyAmountDTOList.get(1).getMonthName());
    }

    /**
     * Getting charge amount when there are no charges on any of the bills.
     * @throws ParseException
     */
    @Test
    public void testGetChargedAmountEachMonth_2() throws ParseException {

        Account account = createAccount();

        Bill bill1 = createBillWithNoId();
        Bill bill2 = createBillWithNoId();

        account.addBill(bill1);
        account.addBill(bill2);
        accountService.addAccount(account);

        List<MonthlyAmountDTO> monthlyAmountDTOList = billService.getTotalChargedEachMonth();

        assertEquals(0, monthlyAmountDTOList.size());
    }

    private BillCharge createBillCharge(){

        BillCharge billCharge = new BillCharge();
        billCharge.setId(1);
        billCharge.setChargeType("Test");
        billCharge.setTax(10f);
        billCharge.setAmount(10f);

        return billCharge;
    }

    private BillCharge createBillChargeWithNoId(){

        BillCharge billCharge = new BillCharge();
        billCharge.setChargeType("Test");
        billCharge.setTax(10f);
        billCharge.setAmount(10f);

        return billCharge;
    }

    private Bill createBill() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Bill bill = new Bill();
        bill.setId(1);
        bill.setIssueDate(dateFormat.parse("31/12/1998"));
        bill.setDueDate(dateFormat.parse("31/12/1998"));

        return bill;
    }

    private Bill createBillWithNoId() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Bill bill = new Bill();
        bill.setIssueDate(dateFormat.parse("31/12/1998"));
        bill.setDueDate(dateFormat.parse("31/12/1998"));

        return bill;
    }

    private Account createAccount(){

        Account account = new Account();
        account.setFirstName("TestFirstName");
        account.setLastName("TestLastName");

        return account;
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
