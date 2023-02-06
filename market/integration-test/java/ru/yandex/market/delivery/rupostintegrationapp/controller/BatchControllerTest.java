package ru.yandex.market.delivery.rupostintegrationapp.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.rupostintegrationapp.BaseContextualTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.Batch.DbBatch;
import ru.yandex.market.delivery.rupostintegrationapp.dao.Batch.DbBatchRepository;
import ru.yandex.market.delivery.russianpostapiclient.bean.declaredenum.MailCategory;
import ru.yandex.market.delivery.russianpostapiclient.bean.declaredenum.MailType;

class BatchControllerTest extends BaseContextualTest {
    private static final int BATCHES_ON_PAGE = DbBatchRepository.BATCHES_ON_PAGE;

    //TODO взяты данные существующего батча из базы, подумать как сделать лучше
    private static final String BATCH_NAME = "247";
    private static final String SENDER_ID = "10206336";
    private static final String DATE = "2017-03-21";
    private static final String BRAND = "ИП «doctor»";
    private static final String INCORPORATION = "ИП «doctor»";
    private static final String MAIL_CATEGORY = MailCategory.WITH_DECLARED_VALUE.name();
    private static final String MAIL_TYPE = MailType.EMS_OPTIMAL.name();


    @Autowired
    private BatchController batchController;

    @Test
    void buildSearchNullRequestTest() {
        List<DbBatch> dbBatchList = batchController.searchBatch(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0
        );

        softly.assertThat(dbBatchList).isNotNull();
        softly.assertThat(dbBatchList.size()).isLessThanOrEqualTo(BATCHES_ON_PAGE);
    }

    @Test
    void buildSearchRequestTest() {
        List<DbBatch> dbBatchList = batchController.searchBatch(
            null,
            SENDER_ID,
            DATE,
            BRAND,
            INCORPORATION,
            BATCH_NAME,
            MAIL_CATEGORY,
            MAIL_TYPE,
            true,
            false,
            0
        );

        softly.assertThat(dbBatchList).isNotNull();
        softly.assertThat(dbBatchList.size()).isLessThanOrEqualTo(BATCHES_ON_PAGE);

        List<DbBatch> assertDbBatchList = dbBatchList.stream()
            .filter(dbBatch -> dbBatch.getSenderId().equals(SENDER_ID))
            .filter(dbBatch -> dbBatch.getDate().toString().equals(DATE))
            .filter(dbBatch -> dbBatch.getBrand().equals(BRAND))
            .filter(dbBatch -> dbBatch.getIncorporation().equals(INCORPORATION))
            .filter(dbBatch -> dbBatch.getMailCategory().equals(MAIL_CATEGORY))
            .filter(dbBatch -> dbBatch.getChecked().equals(true))
            .filter(dbBatch -> dbBatch.getBroken().equals(false))
            .collect(Collectors.toList());

        softly.assertThat(dbBatchList.size()).isEqualTo(assertDbBatchList.size());
    }

    @Test
    void senderIdTest() {
        List<DbBatch> dbBatchList = batchController.searchBatch(
            null,
            SENDER_ID,
            null,
            null,
            null,
            null,
            null,
            MAIL_TYPE,
            null,
            null,
            0
        );

        softly.assertThat(dbBatchList).isNotNull();
        softly.assertThat(dbBatchList.size()).isLessThanOrEqualTo(BATCHES_ON_PAGE);

        List<DbBatch> assertDbBatchList = dbBatchList.stream()
            .filter(dbBatch -> dbBatch.getSenderId().equals(SENDER_ID))
            .filter(dbBatch -> dbBatch.getMailType().toString().equals(MAIL_TYPE))
            .collect(Collectors.toList());

        softly.assertThat(dbBatchList.size()).isEqualTo(assertDbBatchList.size());
    }

    @Test
    void countTest() {
        Integer count = batchController.countBatch(
            null,
            SENDER_ID,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        softly.assertThat(count).isNotNull();
    }

    @Test
    void mailTypeTest() {
        List<MailType> mailTypeList = batchController.getMailTypes();

        softly.assertThat(mailTypeList).isNotNull();
        softly.assertThat(mailTypeList.size()).isLessThanOrEqualTo(MailType.values().length);
    }

    @Test
    void mailCategoryTest() {
        List<MailCategory> mailCategoryList = batchController.getMailCategories();

        softly.assertThat(mailCategoryList).isNotNull();
        softly.assertThat(mailCategoryList.size()).isLessThanOrEqualTo(MailType.values().length);
    }
}
