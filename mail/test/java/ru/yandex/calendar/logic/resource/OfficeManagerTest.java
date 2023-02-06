package ru.yandex.calendar.logic.resource;

import lombok.val;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.geobase.GeobaseIds;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.ip.InternetDomainName;
import ru.yandex.misc.test.Assert;

import static org.assertj.core.api.Assertions.assertThat;

public class OfficeManagerTest extends AbstractConfTest {
    @Autowired
    private OfficeManager officeManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;

    @Test
    public void getOfficesByStaffIds() {
        val domain = new InternetDomainName("yandex");
        Long officeId1 = testManager.createOffice(domain, "Winterfell").getStaffId().get();
        Long officeId2 = testManager.createOffice(domain, "Riverlands").getStaffId().get();
        Long officeId3 = testManager.createOffice(domain, "Red Keep").getStaffId().get();

        ListF<Office> result = officeManager.getOfficesByStaffIds(Cf.list(officeId1, officeId2, officeId3));

        Assert.isTrue(result.size() == 3);
        Assert.isTrue(result.exists(o -> o.getStaffId().get().equals(officeId1)));
        Assert.isTrue(result.exists(o -> o.getStaffId().get().equals(officeId2)));
        Assert.isTrue(result.exists(o -> o.getStaffId().get().equals(officeId3)));
    }

    @Test
    public void getCountryIdByOfficeName() {
        assertThat(OfficeManager.getCountryIdByCityName("")).isEqualTo(GeobaseIds.RUSSIA);
        assertThat(OfficeManager.getCountryIdByCityName("  ")).isEqualTo(GeobaseIds.RUSSIA);
        assertThat(OfficeManager.getCountryIdByCityName("Украина, Симферополь")).isEqualTo(GeobaseIds.CRIMEA);
        assertThat(OfficeManager.getCountryIdByCityName("Украина, Одесса")).isEqualTo(GeobaseIds.UKRAINE);
        assertThat(OfficeManager.getCountryIdByCityName("США, Калифорния")).isEqualTo(GeobaseIds.USA);
        assertThat(OfficeManager.getCountryIdByCityName("Екатеринбург")).isEqualTo(GeobaseIds.RUSSIA);
        assertThat(OfficeManager.getCountryIdByCityName("Новосибирск")).isEqualTo(GeobaseIds.RUSSIA);
        assertThat(OfficeManager.getCountryIdByCityName("Россия, Нижний Новгород")).isEqualTo(GeobaseIds.RUSSIA);
        assertThat(OfficeManager.getCountryIdByCityName("Беларусь, Минск")).isEqualTo(GeobaseIds.BELARUS);
        assertThat(OfficeManager.getCountryIdByCityName("Турция, Стамбул")).isEqualTo(GeobaseIds.TURKEY);
    }

    @Test
    public void yandexMoneyUserShouldSelectTableOfficeIfThereIsNoActiveOne() {
        val tableId = Option.of(42L);
        val uid = new PassportUid(13666);

        userManager.makeYaMoneyUserForTest(uid);

        assertThat(officeManager.chooseActiveOfficeId(Option.empty(), tableId, uid)).isEqualTo(tableId);
    }

    @Test
    public void yandexInternalUserShouldSelectNoOfficeIfThereIsNoActiveOne() {
        val tableId = Option.of(42L);
        val uid = new PassportUid(13666);

        TestManager.createRandomYaTeamUser(uid.getUid());

        assertThat(officeManager.chooseActiveOfficeId(Option.empty(), tableId, uid).toOptional()).isEmpty();
    }

    @Test
    public void yandexMoneyUserShouldSelectLastActiveOfficeIfThereIsAnyNonKR() {
        val domain = new InternetDomainName("gameofthrones");
        val officeId = Option.of(testManager.createOffice(domain, "Winterfell").getId());
        val tableId = Option.of(42L);
        val uid = new PassportUid(13666);

        userManager.makeYaMoneyUserForTest(uid);

        assertThat(officeManager.chooseActiveOfficeId(officeId, tableId, uid)).isEqualTo(officeId);
    }

    @Test
    public void yandexInternalUserShouldSelectLastActiveOfficeIfThereIsAnyNonKR() {
        val domain = new InternetDomainName("yandex");
        val officeId = testManager.createOffice(domain, "Winterfell").getId();
        val tableId = Option.of(42L);
        val uid = new PassportUid(13666);

        TestManager.createRandomYaTeamUser(uid.getUid());

        assertThat(officeManager.chooseActiveOfficeId(officeId, tableId, uid)).isEqualTo(officeId);
    }

    @Test
    public void yandexInternalUserShouldSelectTableOfficeIfActiveOfficeIsInKRAndTableOfficeInKROrAurora() {
        val domain = new InternetDomainName("yandex");
        val uid = new PassportUid(13666);
        val spy = Mockito.spy(officeManager);
        val office = new Office();
        val tableOffice = new Office();

        office.setId(testManager.createOffice(domain, "Winterfell").getId());
        office.setCenterId(OfficeManager.KR_OFFICE_CENTER_IDS.firstO());
        Mockito.doReturn(office).when(spy).getOfficeById(office.getId());

        tableOffice.setId(42L);
        tableOffice.setCenterId(OfficeManager.AURORA_CENTER_ID);
        Mockito.doReturn(office).when(spy).getOfficeById(tableOffice.getId());

        TestManager.createRandomYaTeamUser(uid.getUid());

        assertThat(spy.chooseActiveOfficeId(office.getId(), Option.of(tableOffice.getId()), uid)).isEqualTo(tableOffice.getId());
    }

    @Test
    public void yandexInternalUserShouldSelectMorozovOfficeIfActiveOfficeIsInKRAndTableOfficeIsEmpty() {
        val domain = new InternetDomainName("yandex");
        val uid = new PassportUid(13666);
        val spy = Mockito.spy(officeManager);
        val office = new Office();
        val defaultOffice = new Office();

        office.setId(testManager.createOffice(domain, "Winterfell").getId());
        office.setCenterId(OfficeManager.KR_OFFICE_CENTER_IDS.firstO());
        defaultOffice.setId(42L);
        Mockito.doReturn(Option.of(defaultOffice)).when(spy).getOfficeByCenterId(OfficeManager.MOROZOV_CENTER_ID);
        Mockito.doReturn(office).when(spy).getOfficeById(office.getId());

        TestManager.createRandomYaTeamUser(uid.getUid());

        assertThat(spy.chooseActiveOfficeId(office.getId(), Option.empty(), uid)).isEqualTo(defaultOffice.getId());
    }
}
