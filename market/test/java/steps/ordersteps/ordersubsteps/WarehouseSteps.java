package steps.ordersteps.ordersubsteps;

import ru.yandex.market.delivery.entities.common.Warehouse;

public class WarehouseSteps {

    private WarehouseSteps() {
        throw new UnsupportedOperationException();
    }

    public static Warehouse getWarehouse() {
        Warehouse warehouse = new Warehouse();

        warehouse.setId(ResourceIdSteps.getResourceId());
        warehouse.setAddress(LocationSteps.getLocation());
        warehouse.setSchedule(WorkTimeSteps.getWorkTimeArray());
        warehouse.setContact(PersonSteps.getPerson());
        warehouse.setPhones(PhoneSteps.getPhoneList());
        warehouse.setInstruction("instruction");

        return warehouse;
    }
}
