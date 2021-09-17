import net.gsdgroup.billing.business.AccountService;
import net.gsdgroup.billing.business.BillService;
import net.gsdgroup.billing.entity.Account;
import net.gsdgroup.billing.entity.Bill;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.resource.ServerResource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.time.LocalDateTime;

public class Main extends Application {
    public static void main(String[] args) throws Exception {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        ((Component) context.getBean("top")).start();
    }
}
