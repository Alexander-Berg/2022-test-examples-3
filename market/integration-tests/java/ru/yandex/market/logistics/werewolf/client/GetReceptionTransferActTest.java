package ru.yandex.market.logistics.werewolf.client;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;

@DisplayName("Генерация АПП")
class GetReceptionTransferActTest extends AbstractGetActTest {

    private static final String PATH = "document/receptionTransferAct/generate";

    @Override
    protected byte[] executeQuery(RtaOrdersData data, DocumentFormat format) {
        return wwClient.generateReceptionTransferAct(data, format);
    }

    @Nonnull
    @Override
    protected String getPath() {
        return PATH;
    }
}
