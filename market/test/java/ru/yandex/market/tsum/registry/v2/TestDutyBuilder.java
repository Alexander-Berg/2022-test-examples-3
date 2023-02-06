package ru.yandex.market.tsum.registry.v2;

import ru.yandex.market.tsum.registry.v2.dao.model.Duty;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 03/08/2018
 */
public class TestDutyBuilder {

    private final Duty duty;

    public TestDutyBuilder() {
        duty = new Duty();
    }

    public Duty build() {
        return duty;
    }

    public TestDutyBuilder withRandomName() {
        duty.setName(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestDutyBuilder withName(String name) {
        duty.setName(name);
        return this;
    }

    public TestDutyBuilder withRandomDescription() {
        duty.setDescription(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestDutyBuilder withDescription(String description) {
        duty.setDescription(description);
        return this;
    }

    public TestDutyBuilder withServiceId(String serviceId) {
        duty.setServiceId(serviceId);
        return this;
    }

    public TestDutyBuilder withRandomCalendarId() {
        duty.setCalendarId(RandomFieldsGenerator.getRandomInt());
        return this;
    }

    public TestDutyBuilder withRandomGroupPhone() {
        duty.setGroupPhone(RandomFieldsGenerator.getRandomInt());
        return this;
    }
}
