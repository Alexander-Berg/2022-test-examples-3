package ru.yandex.market.gutgin.tms.pipeline.dcp.xls;


import Market.DataCamp.DataCampOffer;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.partner.content.common.BaseDcpExcelDBStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.XlsDataCampDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcRawSkuDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessState;
import ru.yandex.market.partner.content.common.db.jooq.enums.XlsLogbrokerStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileProcess;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.XlsDatacampOffer;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileCountedData;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class ChangeFileProcessStateByCountTaskActionTest extends BaseDcpExcelDBStateGenerator {

    @Autowired
    GcRawSkuDao gcRawSkuDao;

    @Autowired
    XlsDataCampDao xlsDataCampDao;

    ChangeFileProcessStateByCountTaskAction taskAction;

    @Before
    public void setUp() {
        super.setUp();
        taskAction = new ChangeFileProcessStateByCountTaskAction(
            gcRawSkuDao, fileProcessDao, xlsDataCampDao
        );
    }

    @Test
    public void whenNoOffersUploadedShouldChangeFileToInvalid() {
        List<GcRawSku> gcRawSkus = generateGcRawSkus(10);
        
        taskAction.doRun(new ProcessFileCountedData(requestId, processId, 0, 0));
        
        FileProcess fileProcess = fileProcessDao.fetchOneById(processId);
        Assertions.assertThat(fileProcess.getProcessState()).isEqualTo(FileProcessState.INVALID);
    }

    @Test
    public void whenAllOffersUploadedShouldChangeFileToFinished() {
        List<GcRawSku> gcRawSkusWithCompleteStatus = generateGcRawSkus(10);
        createOffersInStatus(gcRawSkusWithCompleteStatus);

        taskAction.doRun(new ProcessFileCountedData(requestId, processId, 0, 0));

        FileProcess fileProcess = fileProcessDao.fetchOneById(processId);
        Assertions.assertThat(fileProcess.getProcessState()).isEqualTo(FileProcessState.FINISHED);
    }

    @Test
    public void whenNotAllOffersUploadedShouldChangeFileToMixed() {
        List<GcRawSku> gcRawSkusWithCompleteStatus = generateGcRawSkus(10);
        List<GcRawSku> gcRawSkusWithNoOffer = generateGcRawSkus(10);
        createOffersInStatus(gcRawSkusWithCompleteStatus);

        taskAction.doRun(new ProcessFileCountedData(requestId, processId, 0, 0));

        FileProcess fileProcess = fileProcessDao.fetchOneById(processId);
        Assertions.assertThat(fileProcess.getProcessState()).isEqualTo(FileProcessState.MIXED);
    }

    private void createOffersInStatus(List<GcRawSku> gcRawSkus) {
        List<XlsDatacampOffer> offers = gcRawSkus.stream()
            .map(this::convertToXlsDatacampOffer)
            .collect(Collectors.toList());
        xlsDataCampDao.insert(offers);
    }

    private XlsDatacampOffer convertToXlsDatacampOffer(GcRawSku gcRawSku) {
        XlsDatacampOffer offer = new XlsDatacampOffer();
        Timestamp timestamp = Timestamp.from(Instant.now());
        offer.setCreateDate(timestamp);
        offer.setUpdateDate(timestamp);
        offer.setDatacampOffer(DataCampOffer.Offer.newBuilder().build());
        offer.setGcRawSkuId(gcRawSku.getId());
        offer.setStatus(XlsLogbrokerStatus.SUCCESS);
        return offer;
    }
}