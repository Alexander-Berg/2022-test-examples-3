package ru.yandex.market.partner.content.common.db.dao.goodcontent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.GC_VALIDATION_MESSAGE;


public class GcSkuValidationDaoTest extends DBDcpStateGenerator {
    public static final long CATEGORY_ID = 1234567L;
    public static final int SOURCE_ID = 7306;
    public static final int SHOP_ID = 293049;
    public static final String SHOP_SKU_1 = "1";
    public static final String SHOP_SKU_2 = "2";

    @Autowired
    private GcSkuValidationDao gcSkuValidationDao;

    @Before
    public void setUp() {
        super.setUp();
        createSource(SOURCE_ID, SHOP_ID);
    }

    @Test
    public void deleteAllValidationInfoForFileProcess() {
        String mainPictureUrl = "mainPictureUrl";

        Long ticketId = generateDBDcpInitialStateNew(state -> {
            state.reInitWithNewIdentifiers(PARTNER_SHOP_ID, SHOP_SKU_1);
            state.getDcpOfferBuilder().withPictures(mainPictureUrl);
        }).getId();

        TicketValidationResult validationResult = TicketValidationResult.invalid(
            ticketId,
            Messages.get().pictureInvalid(mainPictureUrl,
                false,
                false,
                true,
                false,
                SHOP_SKU_1
            )
        );

        gcSkuValidationDao.saveValidationResults(
            Collections.singletonList(validationResult),
            GcSkuValidationType.PICTURE_MBO_VALIDATION
        );

        List<GcSkuValidation> gcSkuValidations =
            gcSkuValidationDao.getGcSkuValidations(GcSkuValidationType.PICTURE_MBO_VALIDATION, ticketId);
        assertThat(gcSkuValidations).hasSize(1);
    }

    @Issue("MARKETIR-9398")
    @Test
    public void saveValidationResultsUniqueByMessage() {

        String mainPictureUrl = "mainPictureUrl";

        Long ticketId1 = generateDBDcpInitialStateNew(state -> {
            state.reInitWithNewIdentifiers(PARTNER_SHOP_ID, SHOP_SKU_1);
            state.getDcpOfferBuilder().withPictures(mainPictureUrl);
        }).getId();
        Long ticketId2 = generateDBDcpInitialStateNew(state -> {
            state.reInitWithNewIdentifiers(PARTNER_SHOP_ID, SHOP_SKU_2);
            state.getDcpOfferBuilder().withPictures(mainPictureUrl);
        }).getId();

        TicketValidationResult result1 = TicketValidationResult.invalid(
            ticketId1,
            Messages.get().pictureInvalid(mainPictureUrl,
                false,
                false,
                true,
                false,
                SHOP_SKU_1,
                SHOP_SKU_2
            )
        );

        TicketValidationResult result2 = TicketValidationResult.invalid(
            ticketId2,
            Messages.get().pictureInvalid(mainPictureUrl,
                false,
                false,
                true,
                false,
                SHOP_SKU_1,
                SHOP_SKU_2
            )
        );

        //---

        gcSkuValidationDao.saveValidationResultsUniqueByMessage(
            Arrays.asList(result1, result2),
            GcSkuValidationType.PICTURE_MBO_VALIDATION
        );

        //---

        List<GcSkuValidation> gcSkuValidations =
            gcSkuValidationDao.getGcSkuValidations(GcSkuValidationType.PICTURE_MBO_VALIDATION, ticketId1, ticketId2);

        assertThat(gcSkuValidations).hasSize(2);

        Long validationId1 = gcSkuValidations.get(0).getId();
        Long validationId2 = gcSkuValidations.get(1).getId();

        assertThat(validationId1).isNotEqualTo(validationId2);

        List<MessageInfo> messages1 = gcSkuValidationDao.getGcValidationMessages(validationId1);
        List<MessageInfo> messages2 = gcSkuValidationDao.getGcValidationMessages(validationId2);

        assertThat(messages1).hasSize(1);
        assertThat(messages2).hasSize(1);

        MessageInfo message1 = messages1.get(0);
        MessageInfo message2 = messages2.get(0);

        assertThat(message1).isEqualTo(message2);

        Long messageId1 = dsl()
            .select(GC_VALIDATION_MESSAGE.PROTOCOL_MESSAGE_ID)
            .from(GC_VALIDATION_MESSAGE)
            .where(GC_VALIDATION_MESSAGE.VALIDATION_ID.eq(validationId1))
            .fetchOneInto(Long.class);

        Long messageId2 = dsl()
            .select(GC_VALIDATION_MESSAGE.PROTOCOL_MESSAGE_ID)
            .from(GC_VALIDATION_MESSAGE)
            .where(GC_VALIDATION_MESSAGE.VALIDATION_ID.eq(validationId2))
            .fetchOneInto(Long.class);

        assertThat(messageId1).isEqualTo(messageId2);
    }

    @Test
    public void whenFailDataThenCollectIt() throws JsonProcessingException {
        String mainPictureUrl = "mainPictureUrl";
        Long ticketId1 = generateDBDcpInitialStateNew(state -> {
            state.reInitWithNewIdentifiers(PARTNER_SHOP_ID, SHOP_SKU_1);
            state.getDcpOfferBuilder().withPictures(mainPictureUrl);
        }).getId();
        List<ParamInfo> params = new ArrayList<>();
        ParamInfo info1 = new ParamInfo(1L, "param1", false);
        ParamInfo info2 = new ParamInfo(2L, "param2", false);
        params.add(info1);
        params.add(info2);
        FailData failData = new FailData(params);
        TicketValidationResult result1 = TicketValidationResult.invalid(
                ticketId1,
                Collections.singletonList(Messages.get().pictureInvalid(mainPictureUrl,
                        false,
                        false,
                        true,
                        false,
                        SHOP_SKU_1,
                        SHOP_SKU_2
                )), failData
        );

        gcSkuValidationDao.saveValidationResults(
                Collections.singletonList(result1),
                GcSkuValidationType.PICTURE_MBO_VALIDATION
        );

        List<FailData> failDataList = gcSkuValidationDao.getFailData(ticketId1);
        assertThat(failDataList).hasSize(1);
        assertThat(failDataList.get(0).getParams()).containsExactlyInAnyOrder(info1, info2);
    }
}
