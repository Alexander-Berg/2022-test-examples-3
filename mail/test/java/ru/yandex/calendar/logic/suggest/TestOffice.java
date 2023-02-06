package ru.yandex.calendar.logic.suggest;

import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.resource.ResourceInfo;

/**
* @author gutman
*/
public class TestOffice {

    private final Office office;
    private final ResourceInfo resource1;
    private final ResourceInfo resource2;
    private final ResourceInfo resource3;

    TestOffice(Office office, ResourceInfo resource1, ResourceInfo resource2, ResourceInfo resource3) {
        this.office = office;
        this.resource1 = resource1;
        this.resource2 = resource2;
        this.resource3 = resource3;
    }

    public ResourceInfo getResource3() {
        return resource3;
    }

    public ResourceInfo getResource2() {
        return resource2;
    }

    public ResourceInfo getResource1() {
        return resource1;
    }

    public Office getOffice() {
        return office;
    }

}
