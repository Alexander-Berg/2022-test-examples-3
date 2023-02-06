package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.MessageInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class MappingOnValidModelValidationTest {

    private MappingOnValidModelValidation validation;
    private ModelStorageHelper modelStorageHelper;
    @Autowired
    private GcSkuValidationDao gcSkuValidationDao;
    @Autowired
    private GcSkuTicketDao gcSkuTicketDao;

    @Before
    public void setUp() throws Exception {
        this.modelStorageHelper = mock(ModelStorageHelper.class);
        this.validation = new MappingOnValidModelValidation(gcSkuValidationDao,
                gcSkuTicketDao
                , modelStorageHelper);
    }

    @Test
    public void notValidSku() {
        HashMap<Long, ModelStorage.Model> modelsMap = new HashMap<>();
        modelsMap.put(100L, ModelStorage.Model.newBuilder()
                .setId(100L)
                .setSourceType("NOT_PARTNER")
                .build());
        modelsMap.put(101L, ModelStorage.Model.newBuilder()
                .setId(101L)
                .setSourceType("NOT_PARTNER")
                .build());
        doReturn(modelsMap).when(modelStorageHelper).findModelsMap(any());

        List<GcSkuTicket> tickets = Arrays.asList(ticket(100L), ticket(101L));
        ProcessTaskResult<List<TicketValidationResult>> validate = validation.validate(tickets);

        List<TicketValidationResult> result = validate.getResult();
        assertThat(result).hasSize(2);
        result.forEach(ticketResult -> {
            assertThat(ticketResult.isValid()).isFalse();
            ImmutableList<MessageInfo> validationMessages = ticketResult.getValidationMessages();
            assertThat(validationMessages).hasSize(1);
            assertThat(validationMessages).extracting(MessageInfo::getCode)
                    .containsOnly("ir.partner_content.dcp.validation.invalid_model");
            assertThat(validationMessages).extracting(MessageInfo::toString)
                    .containsAnyOf(expectedErrorMessage("100"), expectedErrorMessage("101"));
        });
    }

    @Test
    public void notValidModel() {
        doReturn(Collections.singletonMap(100L,
                ModelStorage.Model.newBuilder()
                        .setId(100)
                        .setSourceType("PARTNER_SKU")
                        .addRelations(ModelStorage.Relation.newBuilder()
                                .setId(110)
                                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                .build())
                        .build()
                )
        ).when(modelStorageHelper).findModelsMap(any());
        doReturn(Collections.singletonMap(110L,
                ModelStorage.Model.newBuilder()
                        .setId(110)
                        .setSourceType("NOT_PARTNER_SKU")
                        .build())
        ).when(modelStorageHelper).findParentModelBySku(any());

        ProcessTaskResult<List<TicketValidationResult>> validate =
                validation.validate(Collections.singletonList(ticket(100L)));

        List<TicketValidationResult> result = validate.getResult();
        assertThat(result).hasSize(1);
        result.forEach(ticketResult -> {
            assertThat(ticketResult.isValid()).isFalse();
            ImmutableList<MessageInfo> validationMessages = ticketResult.getValidationMessages();
            assertThat(validationMessages).hasSize(1);
            assertThat(validationMessages).extracting(MessageInfo::getCode)
                    .containsOnly("ir.partner_content.dcp.validation.invalid_model");
            assertThat(validationMessages).extracting(MessageInfo::toString)
                    .containsOnly(expectedErrorMessage("100"));
        });
    }

    @NotNull
    private String expectedErrorMessage(String id) {
        return "Для офера test_ssku невозможно редактирование карточки " + id + ", т.к. она является маркетной";
    }

    @Test
    public void validSku() {
        doReturn(Collections.singletonMap(100L,
                ModelStorage.Model.newBuilder()
                        .setId(100)
                        .setSourceType("PARTNER_SKU")
                        .build())
        ).when(modelStorageHelper).findModelsMap(any());

        ProcessTaskResult<List<TicketValidationResult>> validate =
                validation.validate(Collections.singletonList(ticket(100L)));

        List<TicketValidationResult> result = validate.getResult();
        assertThat(result).hasSize(1);
        TicketValidationResult result1 = result.get(0);
        assertThat(result1.isValid()).isTrue();
    }

    @Test
    public void shouldNotDuplicateDcpInvalidModel() {
        long modelId = 100L;
        long parentModelId = 110L;

        doReturn(Collections.singletonMap(modelId,
                ModelStorage.Model.newBuilder()
                        .setId(modelId)
                        // Should not be PARTNER_SKU or FAST_SKU
                        .setSourceType(ModelStorage.ModelType.GENERATED_SKU.name())
                        // Should have parent model
                        .addRelations(ModelStorage.Relation.newBuilder()
                                .setId(parentModelId)
                                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        )
                        .build())
        ).when(modelStorageHelper).findModelsMap(any());

        doReturn(Collections.singletonMap(parentModelId,
                ModelStorage.Model.newBuilder()
                        .setId(parentModelId)
                        .build())
        ).when(modelStorageHelper).findParentModelBySku(any());

        ProcessTaskResult<List<TicketValidationResult>> result = validation.validate(Collections.singletonList(ticket(modelId)));

        ImmutableList<MessageInfo> validationMessages = result.getResult().get(0).getValidationMessages();
        assertThat(validationMessages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.validation.invalid_model");
        assertEquals(1, validationMessages.size());
    }

    @NotNull
    private GcSkuTicket ticket(long existingMboPskuId) {
        GcSkuTicket gcSkuTicket = new GcSkuTicket();
        gcSkuTicket.setId(1L);
        gcSkuTicket.setShopSku("test_ssku");
        gcSkuTicket.setExistingMboPskuId(existingMboPskuId);
        return gcSkuTicket;
    }
}