package ru.yandex.market.tpl.core.domain.receipt.test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.receipt.FiscalDataDto;
import ru.yandex.market.tpl.api.model.receipt.FiscalTaxSumDto;
import ru.yandex.market.tpl.core.domain.receipt.CreateFiscalDataRequest;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptData;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptDtoMapper;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptProcessor;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptService;

/**
 * @author kukabara
 */
@Service
@RequiredArgsConstructor
public class TestReceiptProcessor implements ReceiptProcessor {

    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();

    private final ReceiptService receiptService;
    private final ReceiptDtoMapper receiptDtoMapper;
    private final Clock clock;

    @Override
    @Transactional
    public boolean registerCheque(ReceiptData receiptData, boolean retry) {
        receiptService.createFiscalData(new CreateFiscalDataRequest(
                generateFiscalDataDto(receiptData),
                Instant.now(clock),
                receiptData.getId()
        ));
        return true;
    }

    @Override
    @Transactional
    public boolean registerCheque(ReceiptData receiptData) {
        return registerCheque(receiptData, false);
    }

    private FiscalDataDto generateFiscalDataDto(ReceiptData receiptData) {
        BigDecimal total = StreamEx.of(receiptData.getCardAmount(),
                receiptData.getCashAmount(),
                receiptData.getPrepaymentAmount()).
                filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String fp = String.valueOf(RND.nextInt(100000));
        String fd = String.valueOf(RND.nextInt(100000));
        return new FiscalDataDto(
                receiptDtoMapper.mapToDto(receiptData.getServiceClient()),
                "2398423",
                "34923",
                "3294872",
                fp,
                fd,
                LocalDateTime.now(clock),
                "3242343",
                "49785345",
                "Яндекс.ОФД",
                String.format("https://ofd.yandex.ru/vaucher/0003730832008827/%s/%s", fp, fd),
                true,
                new FiscalTaxSumDto(
                        null,
                        null,
                        null,
                        BigDecimal.valueOf(18L),
                        null
                ),
                total,
                "Льва Толстого, 16",
                "Льва Толстого, 17"
        );
    }

}
