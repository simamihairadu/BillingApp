package net.gsdgroup.billing.webservice.billDTO;

public class MonthlyAmountDTO {

    private double amount;
    private String monthName;

    public MonthlyAmountDTO() {}

    public MonthlyAmountDTO(double amount, String monthName) {
        this.amount = amount;
        this.monthName = monthName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }
}
