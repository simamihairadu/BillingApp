package net.gsdgroup.billing.business;

import net.gsdgroup.billing.dao.BillRepository;
import net.gsdgroup.billing.entity.Account;
import net.gsdgroup.billing.entity.Bill;
import net.gsdgroup.billing.entity.BillCharge;
import net.gsdgroup.billing.exceptions.ServiceException;
import net.gsdgroup.billing.webservice.accountDTO.AccountDTO;
import net.gsdgroup.billing.webservice.billDTO.BillChargeDTO;
import net.gsdgroup.billing.webservice.billDTO.BillDTO;
import net.gsdgroup.billing.webservice.billDTO.MonthlyAmountDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class that handles basic Bill functionality.
 */
@Service
public class BillService {

    @Autowired
    private BillRepository billRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private DataValidator dataValidator;

    @Transactional
    public int addBill(Bill bill) {

        int billId = 0;

        if(bill == null){
            throw new ServiceException("Error processing request. The bill entity cannot be null.");
        }

        if(dataValidator.validateBillData(bill)){
            billId = billRepository.add(bill);

        } else {
            throw new ServiceException("Bill entity validation error.");
        }
        return billId;
    }

    @Transactional
    public void deleteBill(int billId) {

        Bill bill = billRepository.getById(Bill.class, billId);

        if(bill == null){
            throw new ServiceException("Bill not found.");

        } else {
            billRepository.delete(bill);
        }
    }

    @Transactional
    public void updateBill(Bill bill) {

        if(bill == null){
            throw new ServiceException("Error processing request. The bill entity cannot be null.");
        }

        if(dataValidator.validateBillData(bill)) {
            billRepository.update(bill);

        } else {
            throw new ServiceException("Bill entity validation error.");
        }
    }

    @Transactional
    public BillDTO getBillById(int id) {

        try {
            BillDTO billDTO = buildBillDTO(billRepository.getById(Bill.class, id));
            return billDTO;

        } catch (NullPointerException e){
            throw new ServiceException("No result.");
        }
    }

    @Transactional
    public List<BillDTO> getAllBills(){

        List<BillDTO> billDTOList = new ArrayList<>();
        //TODO refactor getall
        for (Bill bill : billRepository.getAll(Bill.class)){
            BillDTO billDTO = buildBillDTO(bill);
            billDTOList.add(billDTO);
        }

        return billDTOList;
    }

    @Transactional
    public List<MonthlyAmountDTO> getTotalChargedEachMonth(){

        List<MonthlyAmountDTO> monthlyAmountDTOList = new ArrayList<>();

        for (Object[] objects : billRepository.getTotalChargedEachMonth()){

            Double amount = (double)objects[0];
            String monthName = Month.of((int)objects[1]).name();
            MonthlyAmountDTO monthlyAmountDTO = new MonthlyAmountDTO(amount, monthName);

            monthlyAmountDTOList.add(monthlyAmountDTO);
        }

        return monthlyAmountDTOList;
    }

    @Transactional
    public Bill buildBillEntity(BillDTO billDTO){

        Bill billEntity = new Bill();

        try {
            if (billDTO.getId() != 0) {
                billEntity.setId(billDTO.getId());
            }

            billEntity.setIssueDate(billDTO.getIssueDate());
            billEntity.setDueDate(billDTO.getDueDate());

            if(billDTO.getAccountId() == 0){

                throw new ServiceException("Missing account id.");
            }

            AccountDTO accountDTO = accountService.getAccountById(billDTO.getAccountId());
            Account account = accountService.buildAccountEntity(accountDTO);
            billEntity.setAccount(account);

            for (BillChargeDTO billChargeDTO : billDTO.getBillCharges()){

                BillCharge billCharge = new BillCharge();
                billCharge.setId(billChargeDTO.getId());
                billCharge.setChargeType(billChargeDTO.getChargeType());
                billCharge.setAmount(billChargeDTO.getAmount());
                billCharge.setTax(billChargeDTO.getTax());

                billEntity.addBillCharge(billCharge);
            }

        } catch (NullPointerException e){

            throw new ServiceException("Error processing request. The bill entity cannot be null.");
        }

        return billEntity;
    }

    public BillDTO buildBillDTO (Bill billEntity){

        BillDTO billDTO = new BillDTO();
        AccountDTO accountDTO = accountService.buildAccountDto(billEntity.getAccount());

        billDTO.setId(billEntity.getId());
        billDTO.setIssueDate(billEntity.getIssueDate());
        billDTO.setDueDate(billEntity.getDueDate());
        billDTO.setAccount(accountDTO);
        billDTO.setAccountId(billEntity.getAccount().getId());

        for (BillCharge billCharge : billEntity.getBillCharges()){

            BillChargeDTO billChargeDto = new BillChargeDTO();
            billChargeDto.setId(billCharge.getId());
            billChargeDto.setChargeType(billCharge.getChargeType());
            billChargeDto.setAmount(billCharge.getAmount());
            billChargeDto.setTax(billCharge.getTax());
            billChargeDto.setBill(billDTO);

            billDTO.getBillCharges().add(billChargeDto);
        }

        return billDTO;
    }
}
