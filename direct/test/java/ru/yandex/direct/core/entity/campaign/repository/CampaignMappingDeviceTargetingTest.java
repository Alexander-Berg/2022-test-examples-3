package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collection;
import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignDeviceTargeting;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.campaign.model.CampaignDeviceTargeting.ANDROID_PHONE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignDeviceTargeting.ANDROID_TABLET;
import static ru.yandex.direct.core.entity.campaign.model.CampaignDeviceTargeting.IPAD;
import static ru.yandex.direct.core.entity.campaign.model.CampaignDeviceTargeting.IPHONE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignDeviceTargeting.OTHER_DEVICES;


/**
 * Тест првоеряет успешность преобразования {@link EnumSet}'а в строковое представление для сохранения в {@code SET}
 */
@RunWith(Parameterized.class)
public class CampaignMappingDeviceTargetingTest {

    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(new Object[][]{
                {EnumSet.of(OTHER_DEVICES), "other_devices"},
                {EnumSet.of(OTHER_DEVICES, OTHER_DEVICES), "other_devices"},
                {EnumSet.of(IPHONE, IPAD), "iphone,ipad"},
                {EnumSet.of(OTHER_DEVICES, IPHONE), "other_devices,iphone"},
                {EnumSet.of(OTHER_DEVICES, IPHONE, IPAD), "other_devices,iphone,ipad"},
                {EnumSet.of(OTHER_DEVICES, IPHONE, IPAD, ANDROID_PHONE), "other_devices,iphone,ipad,android_phone"},
                {EnumSet.of(OTHER_DEVICES, IPHONE, IPAD, ANDROID_PHONE, ANDROID_TABLET),
                        "other_devices,iphone,ipad,android_phone,android_tablet"},
                {EnumSet.noneOf(CampaignDeviceTargeting.class), ""},
                {null, null}}
        );
    }

    private EnumSet<CampaignDeviceTargeting> modelDeviceTargeting;
    private String dbDeviceTargeting;

    public CampaignMappingDeviceTargetingTest(EnumSet<CampaignDeviceTargeting> modelDeviceTargeting,
                                              String dbDeviceTargeting) {
        this.modelDeviceTargeting = modelDeviceTargeting;
        this.dbDeviceTargeting = dbDeviceTargeting;
    }


    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                CampaignMappings.deviceTargetingToDb(modelDeviceTargeting),
                is(dbDeviceTargeting));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                CampaignMappings.deviceTargetingFromDb(dbDeviceTargeting),
                is(modelDeviceTargeting));
    }
}
