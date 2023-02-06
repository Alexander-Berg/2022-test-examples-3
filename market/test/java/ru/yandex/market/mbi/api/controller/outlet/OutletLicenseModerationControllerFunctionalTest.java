package ru.yandex.market.mbi.api.controller.outlet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.StringUtils;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.outlet.OutletLicense;
import ru.yandex.market.core.outlet.OutletLicenseType;
import ru.yandex.market.core.outlet.license.OutletLicenseService;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.mbi.api.client.entity.outlets.license.OutletLicenseDTO;
import ru.yandex.market.mbi.api.client.entity.outlets.license.OutletLicenseModerationResultDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.api.converter.OutletLicenseConverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты для {@link ru.yandex.market.mbi.api.controller.OutletLicenseModerationController}.
 *
 * @author Vladislav Bauer
 */
class OutletLicenseModerationControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private OutletLicenseService outletLicenseService;

    @Autowired
    private NotificationService notificationService;

    @Test
    @DbUnitDataSet
    @DisplayName("Проверить отсутствие лицензий для модерации")
    void testListOutletLicensesForModerationEmpty() {
        checkNoLicensesForModeration();
    }

    @Test
    @DbUnitDataSet(before = "OutletLicenseModerationControllerFunctionalTest.before.csv")
    @DisplayName("Проверить выдачу списка лицензий для модерации")
    void testListOutletLicensesForModeration() {
        final OutletLicenseDTO license = getOneLicenseForModeration();
        assertThat(license.getId(), equalTo(3L));
        assertThat(license.getOutletId(), equalTo(103L));
        assertThat(license.getNumber(), equalTo("333"));
        assertThat(license.getType(), equalTo(OutletLicenseType.ALCOHOL));
        assertThat(license.getCheckStatus(), equalTo(ParamCheckStatus.NEW));
        assertThat(license.getIssueDate(), notNullValue());
        assertThat(license.getExpiryDate(), notNullValue());
        assertThat(license.getUpdatedAt(), notNullValue());
    }

    @Test
    @DbUnitDataSet(before = "OutletLicenseModerationControllerFunctionalTest.before.csv")
    @DisplayName("Проверить успешную модерацию лицензии для точки")
    void testModerateLicensesSuccess() {
        final OutletLicenseDTO license = getOneLicenseForModeration();
        assertThat(license.getCheckStatus(), equalTo(ParamCheckStatus.NEW));

        // Отправить результат модерации
        mbiApiClient.moderateOutletLicenses(Collections.singletonList(
                OutletLicenseModerationResultDTO.createSuccess(
                        license.getId(),
                        license.getUpdatedAt()
                )
        ));

        // Проверить статус лицензии и комментарий асессора
        checkNoLicensesForModeration();
        checkLicense(license.getId(), ParamCheckStatus.SUCCESS, StringUtils.EMPTY);
    }

    @Test
    @DbUnitDataSet(before = "OutletLicenseModerationControllerFunctionalTest.before.csv")
    @DisplayName("Проверить, что нотификации шлются только для зафиксированных модераций")
    void testModerateLicensesNotification() {
        final List<OutletLicenseDTO> licenses = mbiApiClient.listOutletLicensesForModeration();
        final List<OutletLicenseDTO> failed = outletLicenseService.findByStatus(ParamCheckStatus.FAIL)
                .stream()
                .map(OutletLicenseConverter::convertToDTO)
                .collect(Collectors.toList());

        int goodLicensesCount = licenses.size();

        licenses.addAll(failed);

        List<OutletLicenseModerationResultDTO> collect = licenses.stream().map(license -> OutletLicenseModerationResultDTO.createSuccess(
                license.getId(), license.getUpdatedAt())).collect(Collectors.toList());

        mbiApiClient.moderateOutletLicenses(collect);
        checkNoLicensesForModeration();

        Mockito.verify(notificationService, Mockito.times(goodLicensesCount))
                .send(Mockito.eq(1556179436),
                        Mockito.eq(774L),
                        Mockito.argThat(arg -> arg.contains(new NamedContainer("outlet-name", "Лучший аутлет в Галактике")))
                );
    }

    @Test
    @DbUnitDataSet(before = "OutletLicenseModerationControllerFunctionalTest.before.csv")
    @DisplayName("Проверить нотификации для зафэйленных модераций")
    void testModerateLicensesFailNotification() {
        final List<OutletLicenseDTO> licenses = mbiApiClient.listOutletLicensesForModeration();
        String comment = "Не самый лучший аутлет в Галактике";

        List<OutletLicenseModerationResultDTO> collect = licenses.stream().map(license -> OutletLicenseModerationResultDTO.createFail(
                license.getId(), license.getUpdatedAt(), comment)).collect(Collectors.toList());

        mbiApiClient.moderateOutletLicenses(collect);
        checkNoLicensesForModeration();

        Mockito.verify(notificationService, Mockito.times(licenses.size()))
                .send(Mockito.eq(1556269991),
                        Mockito.eq(774L),
                        Mockito.argThat(arg -> arg.contains(new NamedContainer("outlet-name", "Лучший аутлет в Галактике"))
                                && arg.contains(new NamedContainer("comment", comment))));
    }

    @Test
    @DbUnitDataSet(before = "OutletLicenseModerationControllerFunctionalTest.before.csv")
    @DisplayName("Проверить не успешную модерацию лицензии для точки")
    void testModerateLicensesFail() {
        final OutletLicenseDTO license = getOneLicenseForModeration();
        assertThat(license.getCheckStatus(), equalTo(ParamCheckStatus.NEW));

        // Отправить результат модерации
        final String comment = "You have errors in alcohol license";
        mbiApiClient.moderateOutletLicenses(Collections.singletonList(
                OutletLicenseModerationResultDTO.createFail(
                        license.getId(),
                        license.getUpdatedAt(),
                        comment
                )
        ));

        // Проверить статус лицензии и комментарий асессора
        checkNoLicensesForModeration();
        checkLicense(license.getId(), ParamCheckStatus.FAIL, comment);
    }

    @Test
    @DbUnitDataSet(before = "OutletLicenseModerationControllerFunctionalTest.before.csv")
    @DisplayName("Проверить запрет модерации лицензии для точки в статусе отличном от NEW")
    void testModerateLicensesWithWrongStatus() {
        // Получить список лицензий в статусе FAIL
        final List<OutletLicenseDTO> licenses = outletLicenseService.findByStatus(ParamCheckStatus.FAIL)
                .stream()
                .map(OutletLicenseConverter::convertToDTO)
                .collect(Collectors.toList());

        // Отправить результат модерации
        final OutletLicenseDTO license = licenses.iterator().next();
        mbiApiClient.moderateOutletLicenses(Collections.singletonList(
                OutletLicenseModerationResultDTO.createSuccess(
                        license.getId(),
                        license.getUpdatedAt()
                )
        ));

        // Проверить что фиксация модерации отклонена, потому что статус лицензии не NEW
        checkLicense(license.getId(), ParamCheckStatus.FAIL, "Very bad license");
    }

    @Test
    @DbUnitDataSet(before = "OutletLicenseModerationControllerFunctionalTest.before.csv")
    @DisplayName("Проверить запрет модерации лицензии для точки, если сработал optimistic lock по updatedAt")
    void testModerateLicensesWithWrongUpdatedAt() {
        final OutletLicenseDTO license = getOneLicenseForModeration();

        // Отправить результат модерации
        mbiApiClient.moderateOutletLicenses(Collections.singletonList(
                OutletLicenseModerationResultDTO.createFail(
                        license.getId(),
                        DateUtil.addDay(license.getUpdatedAt(), -1),
                        "What the hell are you?"
                )
        ));

        // Проверить что фиксация модерации отклонена, потому что не совпал updatedAt
        assertThat(getOneLicenseForModeration().getId(), equalTo(license.getId()));
    }


    /**
     * Проверить что фиксация модерации прошла успешно, больше модерировать нечего.
     */
    private void checkNoLicensesForModeration() {
        final List<OutletLicenseDTO> licenses = mbiApiClient.listOutletLicensesForModeration();
        assertThat(licenses, empty());
    }

    /**
     * @return Получить одну единственную лицензию для модерации.
     */
    private OutletLicenseDTO getOneLicenseForModeration() {
        final List<OutletLicenseDTO> licenses = mbiApiClient.listOutletLicensesForModeration();
        assertThat(licenses, hasSize(1));

        return licenses.iterator().next();
    }

    private void checkLicense(final long licenseId, final ParamCheckStatus status, final String comment) {
        final Collection<OutletLicense> licenses = outletLicenseService.findByStatus(status);
        OutletLicense license = CollectionUtils.first(
                licenses,
                item -> Objects.equals(item.getId(), licenseId),
                null
        );
        assertThat(license.getCheckStatus(), equalTo(status));
        assertThat(license.getCheckComment(), equalTo(comment));
    }

}
