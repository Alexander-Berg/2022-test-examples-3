package ru.yandex.market.tpl.core.service.sqs;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressCancelReason;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressModificationSource;
import ru.yandex.market.logistics.les.lrm.LrmReturnAtClientAddressCancelledEvent;

@UtilityClass
public class LrmReturnAtClientAddressCancelEventGenerateService {

    public LrmReturnAtClientAddressCancelledEvent generateEvent(
            LrmReturnAtClientAdressCancelEventGenerateParam param
    ) {
        return new LrmReturnAtClientAddressCancelledEvent(
                param.getReturnId(),
                param.getReturnExternalId(),
                param.getModificationSource(),
                param.getCancelReason());
    }

    @Getter
    @Builder(toBuilder = true)
    public static final class LrmReturnAtClientAdressCancelEventGenerateParam {

        @Builder.Default
        private Long returnId = 1234L;

        @Builder.Default
        private String returnExternalId = UUID.randomUUID().toString();

        @Builder.Default
        private TplReturnAtClientAddressCancelReason cancelReason = TplReturnAtClientAddressCancelReason.CLIENT_REFUSED;

        @Builder.Default
        private TplReturnAtClientAddressModificationSource modificationSource =
                TplReturnAtClientAddressModificationSource.CLIENT;
    }
}
