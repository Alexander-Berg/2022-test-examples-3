package steps.ordersteps.ordersubsteps;

import ru.yandex.market.delivery.entities.common.ResourceId;

public class ResourceIdSteps {

    private ResourceIdSteps() {
        throw new UnsupportedOperationException();
    }

    public static ResourceId getResourceId() {
        ResourceId resourceId = new ResourceId();

        resourceId.setDeliveryId("123");
        resourceId.setYandexId("666");

        return resourceId;
    }
}
