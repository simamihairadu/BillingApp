package net.gsdgroup.billing.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.restlet.resource.Status;

@Status(value = 500, serialize = true)
@JsonIgnoreProperties({ "stackTrace","suppressed","localizedMessage","cause"})
public class ServiceException extends RuntimeException {

    public ServiceException(String message){
        super(message);
    }
}
