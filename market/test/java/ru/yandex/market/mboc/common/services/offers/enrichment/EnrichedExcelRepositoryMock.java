package ru.yandex.market.mboc.common.services.offers.enrichment;

import java.time.LocalDateTime;
import java.util.List;

import ru.yandex.market.mbo.lightmapper.test.IntGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

/**
 * @author galaev@yandex-team.ru
 * @since 30/07/2018.
 */
public class EnrichedExcelRepositoryMock extends IntGenericMapperRepositoryMock<EnrichedExcel>
    implements EnrichedExcelRepository {

    public EnrichedExcelRepositoryMock() {
        super(EnrichedExcel::setActionId, EnrichedExcel::getActionId);
    }

    @Override
    public EnrichedExcel update(EnrichedExcel enrichedExcel) {
        return super.update(enrichedExcel);
    }

    @Override
    public List<EnrichedExcel> findOutdatedFiles() {
        return findWhere(enrichedExcel -> {
            LocalDateTime yesterday = DateTimeUtils.dateTimeNow().minusDays(1);
            return enrichedExcel.getCreated().isBefore(yesterday);
        });
    }
}
