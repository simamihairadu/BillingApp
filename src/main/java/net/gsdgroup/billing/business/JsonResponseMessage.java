package net.gsdgroup.billing.business;

/**
 * The default endpoint Json response.
 */
public class JsonResponseMessage {
    private String message;
    private int statusCode;

    public JsonResponseMessage(String message){
        this.message = message;
    }

    public JsonResponseMessage(String message, int statusCode){
        this.message = message;
        this.statusCode = statusCode;
    }
    public JsonResponseMessage() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
