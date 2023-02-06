package ru.yandex.market.mbi.tariffs.matcher;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.hamcrest.Matcher;

import ru.yandex.market.mbi.tariffs.model.ModelType;
import ru.yandex.market.mbi.tariffs.model.Partner;
import ru.yandex.market.mbi.tariffs.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.model.TariffDTO;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчеры для {@link ru.yandex.market.mbi.tariffs.model.TariffDTO}
 */
public class TariffDTOMatcher {
    private TariffDTOMatcher() {
        throw new UnsupportedOperationException();
    }

    public static Matcher<TariffDTO> hasId(long expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getId, expectedValue, "id")
                .build();
    }

    public static Matcher<TariffDTO> hasDraftId(Long expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
            .add(TariffDTO::getDraftId, expectedValue, "draftId")
            .build();
    }

    public static Matcher<TariffDTO> hasModelType(ModelType expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
            .add(TariffDTO::getModelType, expectedValue, "modelType")
            .build();
    }

    public static Matcher<TariffDTO> hasServiceType(ServiceTypeEnum expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getServiceType, expectedValue, "serviceType")
                .build();
    }

    public static Matcher<TariffDTO> hasDateFrom(LocalDate expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getDateFrom, expectedValue, "dateFrom")
                .build();
    }

    public static Matcher<TariffDTO> hasDateTo(LocalDate expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getDateTo, expectedValue, "dateTo")
                .build();
    }

    public static Matcher<TariffDTO> hasTags(List<String> expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getTags, expectedValue, "tags")
                .build();
    }

    public static Matcher<TariffDTO> hasPartner(Partner expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getPartner, expectedValue, "partner")
                .build();
    }

    public static Matcher<TariffDTO> hasUpdatedTime(OffsetDateTime expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getUpdatedTime, expectedValue, "updatedTime")
                .build();
    }

    public static Matcher<TariffDTO> hasUpdatedBy(String expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getUpdatedBy, expectedValue, "updatedBy")
                .build();
    }

    public static Matcher<TariffDTO> hasIsActive(boolean expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getIsActive, expectedValue, "isActive")
                .build();
    }

    public static Matcher<TariffDTO> hasApprovalTicket(String expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getApprovalTicketId, expectedValue, "isActive")
                .build();
    }

    public static Matcher<TariffDTO> hasMeta(List<Object> expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(TariffDTO::getMeta, expectedValue, "meta")
                .build();
    }

    public static Matcher<TariffDTO> hasMetaSize(int expectedValue) {
        return MbiMatchers.<TariffDTO>newAllOfBuilder()
                .add(tariff -> tariff.getMeta().size(), expectedValue, "metaSize")
                .build();
    }
}
