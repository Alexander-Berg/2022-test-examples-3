package ru.yandex.market.mbi.tariffs.matcher;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.hamcrest.Matcher;

import ru.yandex.market.mbi.tariffs.model.DraftDTO;
import ru.yandex.market.mbi.tariffs.model.ModelType;
import ru.yandex.market.mbi.tariffs.model.Partner;
import ru.yandex.market.mbi.tariffs.model.ServiceTypeEnum;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчер для {@link ru.yandex.market.mbi.tariffs.model.DraftDTO}
 */
public final class DraftDTOMatcher {
    private DraftDTOMatcher() {
        throw new UnsupportedOperationException();
    }

    public static Matcher<DraftDTO> hasId(long expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getId, expectedValue, "id")
            .build();
    }

    public static Matcher<DraftDTO> hasServiceType(ServiceTypeEnum expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getServiceType, expectedValue, "serviceType")
            .build();
    }

    public static Matcher<DraftDTO> hasModelType(ModelType expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getModelType, expectedValue, "modelType")
            .build();
    }

    public static Matcher<DraftDTO> hasDateFrom(LocalDate expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getDateFrom, expectedValue, "dateFrom")
            .build();
    }

    public static Matcher<DraftDTO> hasDateTo(LocalDate expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getDateTo, expectedValue, "dateTo")
            .build();
    }

    public static Matcher<DraftDTO> hasTags(List<String> expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getTags, expectedValue, "tags")
            .build();
    }

    public static Matcher<DraftDTO> hasPartner(Partner expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getPartner, expectedValue, "partner")
            .build();
    }

    public static Matcher<DraftDTO> hasUpdatedTime(OffsetDateTime expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getUpdatedTime, expectedValue, "updatedTime")
            .build();
    }

    public static Matcher<DraftDTO> hasUpdatedBy(String expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getUpdatedBy, expectedValue, "updatedBy")
            .build();
    }

    public static Matcher<DraftDTO> hasMeta(List<Object> expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getMeta, expectedValue, "meta")
            .build();
    }

    public static Matcher<DraftDTO> hasMetaSize(int expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
                .add(draft -> draft.getMeta().size(), expectedValue, "metaSize")
                .build();
    }

    public static Matcher<DraftDTO> hasApprovalTicket(String expectedValue) {
        return MbiMatchers.<DraftDTO>newAllOfBuilder()
            .add(DraftDTO::getApprovalTicketId, expectedValue, "approvalTicket")
            .build();
    }
}
