package ru.yandex.market.gutgin.tms.db.dao;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.gutgin.tms.base.BaseDbGutGinTest;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Source;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationDaoTest extends BaseDbGutGinTest {
    Logger log = LoggerFactory.getLogger("test");

    @Resource
    SourceDao sourceDao;

    @Resource
    GcSkuTicketDao ticketDao;

    @Resource
    GcSkuValidationDao validationDao;

    @Test
    public void test() throws IOException {
        Source source = new Source();
        source.setSourceId(1);
        source.setSourceName("aaaa");
        sourceDao.insert(source);

        GcSkuTicket ticket = new GcSkuTicket();
        ticket.setSourceId(1);
        ticket.setCategoryId(1111L);
        ticket.setStatus(GcSkuTicketStatus.NEW);
        ticket.setCreateDate(Timestamp.from(Instant.now()));
        ticket.setUpdateDate(Timestamp.from(Instant.now()));
        ticketDao.insert(ticket);
        log.info("ticket id: " + ticket.getId());

        GcSkuValidation validation  = new GcSkuValidation();
        validation.setSkuTicketId(ticket.getId());
        validation.setValidationType(GcSkuValidationType.TITLE_LENGTH_VALIDATION);
        validation.setIsOk(false);
        validation.setCheckDate(Timestamp.from(Instant.now()));
        validation.setFailData(new FailData(Arrays.asList(new ParamInfo(123L, null, false))));
        validationDao.insert(validation);

        List<GcSkuValidation> savedValidations = validationDao.fetchByTicketIds(Arrays.asList(ticket.getId()));
        assertThat(savedValidations).hasSize(1);
        GcSkuValidation savedValidation = savedValidations.get(0);

        FailData failData = savedValidation.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).hasSize(1);
        ParamInfo paramInfo = failData.getParams().get(0);
        assertThat(paramInfo.getParamId()).isEqualTo(123);
        assertThat(paramInfo.getParamName()).isNull();
    }

}
