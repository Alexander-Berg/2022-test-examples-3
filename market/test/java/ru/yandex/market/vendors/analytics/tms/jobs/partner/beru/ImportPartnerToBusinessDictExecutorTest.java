package ru.yandex.market.vendors.analytics.tms.jobs.partner.beru;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableReader;
import ru.yandex.market.vendors.analytics.tms.yt.model.PartnerBusinessLinkDTO;

import static org.mockito.Mockito.when;

public class ImportPartnerToBusinessDictExecutorTest extends FunctionalTest {

    @Autowired
    private YtTableReader<PartnerBusinessLinkDTO> partnerBusinessDictYtReader;

    @Autowired
    private ImportPartnerToBusinessDictExecutor importPartnerExecutor;

    @Test
    @DbUnitDataSet(before = "ImportPartnerToBusinessDict.before.csv", after = "ImportPartnerToBusinessDict.after.csv")
    @ClickhouseDbUnitDataSet(before = "ImportPartnerToBusinessDictClickhouse.before.csv")
    void importPartnerToBusinessDict() {
        when(partnerBusinessDictYtReader.loadInfoFromYtTable()).thenReturn(
                ImmutableList.of(
                        new PartnerBusinessLinkDTO(1L,10L, 100L),
                        new PartnerBusinessLinkDTO(2L,20L, 200L)
                )
        );
        importPartnerExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "ImportPartnerToBusinessDict.before.csv", after = "ImportPartnerToBusinessDict.after.csv")
    @ClickhouseDbUnitDataSet(before = "ImportPartnerToBusinessDictClickhouse.before.csv")
    void importPartnerToBusinessDictNoDuplication() {
        when(partnerBusinessDictYtReader.loadInfoFromYtTable()).thenReturn(
                ImmutableList.of(
                        new PartnerBusinessLinkDTO(1L,10L, 100L),
                        new PartnerBusinessLinkDTO(2L,20L, 200L)
                )
        );
        importPartnerExecutor.doJob(null);
        importPartnerExecutor.doJob(null);
    }
}
