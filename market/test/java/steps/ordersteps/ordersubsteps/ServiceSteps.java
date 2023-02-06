package steps.ordersteps.ordersubsteps;

import java.util.Arrays;
import java.util.List;

import ru.yandex.market.delivery.entities.common.Service;

public class ServiceSteps {

    private ServiceSteps() {
        throw new UnsupportedOperationException();
    }

    public static Service getService() {
        return new Service();
    }

    public static List<Service> getServiceList() {
        return Arrays.asList(getService(), getService());
    }
}
