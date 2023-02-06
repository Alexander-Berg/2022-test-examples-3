package ru.yandex.market.crm.campaign.http.controller;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailSendingFactInfo;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactStatus;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactType;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.templates.BlockTemplateDto;
import ru.yandex.market.crm.campaign.domain.templates.BlockTemplateDto.UsingEntityType;
import ru.yandex.market.crm.campaign.domain.templates.TemplatesResult;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;
import ru.yandex.market.crm.campaign.services.sending.EmailSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.facts.EmailSendingFactInfoDAO;
import ru.yandex.market.crm.campaign.services.templates.BlockTemplateService;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.sending.UploadedImage;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.BlockConf;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.crm.core.domain.templates.TemplateType;
import ru.yandex.market.crm.core.services.image.ImageOwnerType;
import ru.yandex.market.crm.core.services.image.UploadedImageDAO;
import ru.yandex.market.crm.core.services.messages.MessageTemplatesDAO;
import ru.yandex.market.crm.core.services.templates.BlockTemplatesDAO;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
public class TemplatesControllerTest extends AbstractControllerMediumTest {

    private static final UploadedImage IMAGE = new UploadedImage("Image", "https://yastatic.ru/image");
    @Inject
    private JsonSerializer jsonSerializer;
    @Inject
    private JsonDeserializer jsonDeserializer;
    @Inject
    private EmailSendingDAO emailSendingDAO;
    @Inject
    private EmailSendingFactInfoDAO emailSendingFactInfoDAO;
    @Inject
    private BlockTemplatesDAO blockTemplatesDao;
    @Inject
    private BlockTemplateService blockTemplateService;
    @Inject
    private CampaignDAO campaignDAO;
    @Inject
    private UploadedImageDAO uploadedImageDao;
    @Inject
    private MessageTemplatesDAO messageTemplatesDAO;

    private static BlockTemplate blockTemplate(TemplateType type) {
        return new BlockTemplate()
                .setName("Template")
                .setType(type)
                .setBody("<div/>")
                .setImages(Collections.singletonList(IMAGE));
    }

    private static BlockTemplate blockTemplate() {
        return blockTemplate(TemplateType.BANNER);
    }

    private static void assertUsage(UsingEntityType entityType, String name, String id, BlockTemplateDto.Usage usage) {
        assertEquals(entityType, usage.getEntityType());
        assertEquals(id, usage.getId());
        assertEquals(name, usage.getName());
    }

    @Test
    public void testSaveNewTemplate() throws Exception {
        UploadedImage image = uploadedImageDao.create(ImageOwnerType.BLOCK_TEMPLATE, IMAGE);

        BlockTemplate template = blockTemplate()
                .setImages(Collections.singletonList(image));

        BlockTemplateDto dto = saveTemplate(template);

        Assertions.assertNotNull(dto);
        Assertions.assertNotNull(dto.getId());
        assertEquals(1, (int) dto.getVersion());
        Assertions.assertNotNull(dto.getKey());
        assertEquals(template.getName(), dto.getName());

        BlockTemplate savedTemplate = blockTemplateService.get(dto.getId());
        assertEquals(template.getName(), savedTemplate.getName());
        assertEquals(1, (int) savedTemplate.getVersion());
        Assertions.assertNotNull(savedTemplate.getKey());

        List<UploadedImage> images = uploadedImageDao.list(ImageOwnerType.BLOCK_TEMPLATE, savedTemplate.getId());
        assertEquals(1, images.size());
        assertEquals(image.getUrl(), images.get(0).getUrl());
        assertEquals(image.getName(), images.get(0).getName());
    }

    /**
     * В случае если происходит редактирование шаблона, используемого
     * в отправленной рассылке, создается новая версия шаблона
     */
    @Test
    public void testEditTemplateUsedInSentEmailSending() throws Exception {
        UploadedImage image1 = uploadedImageDao.create(ImageOwnerType.BLOCK_TEMPLATE, IMAGE);

        BlockTemplate originalTemplate = blockTemplate()
                .setImages(Collections.singletonList(image1));

        originalTemplate = blockTemplateService.saveTemplate(originalTemplate);
        saveSentSending(originalTemplate);

        UploadedImage image2 = uploadedImageDao.create(
                ImageOwnerType.BLOCK_TEMPLATE,
                new UploadedImage("Image 2", "https://yastatic.ru/image2")
        );

        BlockTemplate alteredTemplate = blockTemplate()
                .setName("Altered template")
                .setKey(originalTemplate.getKey())
                .setBody("<div>value</div>")
                .setImages(Arrays.asList(image1, image2));

        BlockTemplateDto dto = saveTemplate(alteredTemplate);

        BlockTemplate currentVersion = blockTemplateService.get(dto.getId());

        Assertions.assertNotEquals(originalTemplate.getId(), currentVersion.getId());
        assertEquals(alteredTemplate.getName(), currentVersion.getName());
        assertEquals(alteredTemplate.getBody(), currentVersion.getBody());
        assertEquals(2, (int) currentVersion.getVersion());
        assertEquals(originalTemplate.getKey(), currentVersion.getKey());

        List<UploadedImage> currentImages = uploadedImageDao.list(
                ImageOwnerType.BLOCK_TEMPLATE,
                currentVersion.getId()
        );
        assertEquals(
                ImmutableSet.of(image1.getUrl(), image2.getUrl()),
                currentImages.stream().map(UploadedImage::getUrl).collect(Collectors.toSet())
        );

        BlockTemplate prevVersion = blockTemplateService.get(originalTemplate.getId());
        assertEquals(originalTemplate.getId(), prevVersion.getId());
        assertEquals(originalTemplate.getName(), prevVersion.getName());
        assertEquals(originalTemplate.getBody(), prevVersion.getBody());

        List<UploadedImage> prevImages = uploadedImageDao.list(ImageOwnerType.BLOCK_TEMPLATE, prevVersion.getId());
        assertEquals(1, prevImages.size());
        assertEquals(image1.getUrl(), prevImages.get(0).getUrl());
    }

    /**
     * При редактировании нигде не используемого шаблона новая версия не создается
     */
    @Test
    public void testEditTemplateNotUsedAnywhere() throws Exception {
        BlockTemplate originalTemplate = blockTemplate();

        originalTemplate = blockTemplateService.saveTemplate(originalTemplate);

        BlockTemplate alteredTemplate = blockTemplate()
                .setName("Altered template")
                .setKey(originalTemplate.getKey())
                .setBody("<div>value</div>");

        BlockTemplateDto dto = saveTemplate(alteredTemplate);

        BlockTemplate lastVersion = blockTemplateService.get(dto.getId());

        assertEquals(alteredTemplate.getBody(), lastVersion.getBody());
        assertEquals(originalTemplate.getVersion(), lastVersion.getVersion());
    }

    /**
     * При редактировании шаблона, используемого в не отправленной промо-рассылке
     * новая версия шаблона не создается
     */
    @Test
    public void testEditTemplateUsedInNotSentSending() throws Exception {
        BlockTemplate originalTemplate = blockTemplate();

        originalTemplate = blockTemplateService.saveTemplate(originalTemplate);

        saveNotSentSending(originalTemplate);

        BlockTemplate alteredTemplate = blockTemplate()
                .setName("Altered template")
                .setKey(originalTemplate.getKey())
                .setBody("<div>value</div>");

        BlockTemplateDto dto = saveTemplate(alteredTemplate);
        BlockTemplate lastVersion = blockTemplateService.get(dto.getId());

        assertEquals(alteredTemplate.getBody(), lastVersion.getBody());
        assertEquals(originalTemplate.getVersion(), lastVersion.getVersion());
    }

    /**
     * При редактировании шаблона, используемого в неактуальной версии шаблона сообщения
     * происходит создание новой версии шаблона
     */
    @Test
    public void testEditUsedInOldVersionEmailTemplate() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());

        saveMessageTemplate(1, config -> {
            BannerBlockConf block = new BannerBlockConf();
            block.setId("block_id");
            block.setTemplate(template.getId());
            config.setBlocks(Collections.singletonList(block));
        });

        saveMessageTemplate(2, config -> {
        });

        BlockTemplate alteredTemplate = blockTemplate()
                .setName("Altered template")
                .setKey(template.getKey())
                .setBody("<div>value</div>");

        BlockTemplateDto dto = saveTemplate(alteredTemplate);
        BlockTemplate lastVersion = blockTemplateService.get(dto.getId());

        assertEquals(2, (int) lastVersion.getVersion());
        Assertions.assertNotEquals(template.getId(), lastVersion.getId());
    }

    /**
     * При редактировании шаблона, используемого в неактуальной версии шаблона сообщения
     * происходит создание новой версии шаблона
     */
    @Test
    public void testEditUsedInOldVersionUnpublishedEmailTemplate() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());

        saveMessageTemplate(1, MessageTemplateState.DRAFT, config -> {
            BannerBlockConf block = new BannerBlockConf();
            block.setId("block-id");
            block.setTemplate(template.getId());
            config.setBlocks(Collections.singletonList(block));
        });

        saveMessageTemplate(2, config -> {
        });

        BlockTemplate alteredTemplate = blockTemplate()
                .setName("Altered template")
                .setKey(template.getKey())
                .setBody("<div>value</div>");

        BlockTemplateDto dto = saveTemplate(alteredTemplate);
        BlockTemplate lastVersion = blockTemplateService.get(dto.getId());

        assertEquals(2, (int) lastVersion.getVersion());
        Assertions.assertNotEquals(template.getId(), lastVersion.getId());
    }

    /**
     * При редактировании шаблона, используемого в актуальной опубликованной версии шаблона сообщения,
     * происходит создание новой версии
     */
    @Test
    public void testEditUsedInActualPublishedVersionEmailTemplate() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());

        saveMessageTemplate(1, config -> {
        });

        saveMessageTemplate(2, config -> {
            BannerBlockConf block = new BannerBlockConf();
            block.setId("block_id");
            block.setTemplate(template.getId());
            config.setBlocks(Collections.singletonList(block));
        });

        BlockTemplate alteredTemplate = blockTemplate()
                .setKey(template.getKey())
                .setBody("<div>value</div>");

        BlockTemplateDto dto = saveTemplate(alteredTemplate);
        BlockTemplate lastVersion = blockTemplateService.get(dto.getId());

        assertEquals(2, (int) lastVersion.getVersion());
        Assertions.assertNotEquals(template.getId(), lastVersion.getId());
        assertEquals(alteredTemplate.getBody(), lastVersion.getBody());

        BlockTemplate oldTemplate = blockTemplatesDao.get(template.getId());
        Assertions.assertTrue(oldTemplate.isArchived());
    }

    /**
     * При редактировании шаблона, используемого в актуальной не опубликованной версии шаблона сообщения,
     * не происходит создание новой версии
     */
    @Test
    public void testEditUsedInActualNotPublishedVersionEmailTemplate() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());

        saveMessageTemplate(1, MessageTemplateState.DRAFT, config -> {
            BannerBlockConf block = new BannerBlockConf();
            block.setId("block_id");
            block.setTemplate(template.getId());
            config.setBlocks(Collections.singletonList(block));
        });

        BlockTemplate alteredTemplate = blockTemplate()
                .setKey(template.getKey())
                .setBody("<div>value</div>");

        BlockTemplateDto dto = saveTemplate(alteredTemplate);
        BlockTemplate lastVersion = blockTemplateService.get(dto.getId());

        assertEquals(1, (int) lastVersion.getVersion());
        assertEquals(template.getId(), lastVersion.getId());
        assertEquals(alteredTemplate.getBody(), lastVersion.getBody());
    }

    /**
     * При создании новой версии шаблона, она заменяет старые версии
     * в актуальных шаблонах сообщений и еще промо-рассылках которые еще
     * не успели отправить
     */
    @Test
    public void testReplaceTemplateIfNewVersionCreated() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());

        EmailPlainSending sentSending = saveSentSending(template);
        EmailPlainSending newSending = saveNotSentSending(template);

        BannerBlockConf block = new BannerBlockConf();
        block.setId("block_id");
        block.setTemplate(template.getId());

        var oldTemplate = saveMessageTemplate(
                1,
                config -> config.setBlocks(Collections.singletonList(block))
        );

        var newTemplate = saveMessageTemplate(
                2,
                config -> {
                    List<BlockConf> blocks = new ArrayList<>();
                    blocks.add(block);

                    BannerBlockConf block2 = new BannerBlockConf();
                    block2.setId("block-2_id");
                    block2.setTemplate("another_template");
                    blocks.add(block2);

                    config.setBlocks(blocks);
                }
        );

        BlockTemplate alteredTemplate = blockTemplate()
                .setKey(template.getKey())
                .setBody("<div>value</div>");

        BlockTemplateDto lastVersion = saveTemplate(alteredTemplate);

        sentSending = emailSendingDAO.getSending(sentSending.getId());
        newSending = emailSendingDAO.getSending(newSending.getId());

        oldTemplate = getTemplate(oldTemplate);
        newTemplate = getTemplate(newTemplate);

        assertEquals(template.getId(),
                sentSending.getConfig().getVariants().get(0).getBlocks().get(0).getTemplate());
        assertEquals(template.getId(), oldTemplate.getConfig().getBlocks().get(0).getTemplate());

        assertEquals(lastVersion.getId(),
                newSending.getConfig().getVariants().get(0).getBlocks().get(0).getTemplate());

        Assertions.assertFalse(newTemplate.isLastVersion());
        List<BlockConf> templateBlocks = newTemplate.getConfig().getBlocks();
        assertEquals(template.getId(), templateBlocks.get(0).getTemplate());
        assertEquals("another_template", templateBlocks.get(1).getTemplate());

        var currentTemplateVersion =
                messageTemplatesDAO.<EmailMessageConf>getLastVersion(newTemplate.getKey()).orElseThrow();
        Assertions.assertNotEquals(newTemplate.getId(), currentTemplateVersion.getId());
        assertEquals(MessageTemplateState.DRAFT, currentTemplateVersion.getState());

        templateBlocks = currentTemplateVersion.getConfig().getBlocks();
        assertEquals(lastVersion.getId(), templateBlocks.get(0).getTemplate());
        assertEquals("another_template", templateBlocks.get(1).getTemplate());
    }

    @Test
    public void testReplaseMessageBodyTemplateIfNewVersionCreated() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate(TemplateType.HEAD));

        var oldTemplate = saveMessageTemplate(
                1,
                config -> config.setTemplate(template.getId())
        );

        var newTemplate = saveMessageTemplate(
                2,
                MessageTemplateState.DRAFT,
                config -> config.setTemplate(template.getId())
        );

        BlockTemplate alteredTemplate = blockTemplate(TemplateType.HEAD)
                .setKey(template.getKey())
                .setBody("<div>value</div>");

        BlockTemplateDto lastVersion = saveTemplate(alteredTemplate);

        oldTemplate = getTemplate(oldTemplate);
        newTemplate = getTemplate(newTemplate);

        Assertions.assertNotEquals(template.getId(), lastVersion.getId());
        assertEquals(template.getId(), oldTemplate.getConfig().getTemplate());
        assertEquals(lastVersion.getId(), newTemplate.getConfig().getTemplate());
    }

    @Test
    public void testReplaceSendingBodyTemplateIfNewVersionCreate() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate(TemplateType.HEAD));

        EmailPlainSending sentSending = saveSentSending(config -> {
            EmailSendingVariantConf variant = new EmailSendingVariantConf();
            variant.setTemplate(template.getId());
            config.setVariants(Collections.singletonList(variant));
        });

        EmailPlainSending newSending = saveNotSentSending(config -> {
            EmailSendingVariantConf variant = new EmailSendingVariantConf();
            variant.setTemplate(template.getId());
            config.setVariants(Collections.singletonList(variant));
        });

        BlockTemplate alteredTemplate = blockTemplate(TemplateType.HEAD)
                .setKey(template.getKey())
                .setBody("<div>value</div>");

        BlockTemplateDto lastVersion = saveTemplate(alteredTemplate);

        sentSending = emailSendingDAO.getSending(sentSending.getId());
        newSending = emailSendingDAO.getSending(newSending.getId());

        Assertions.assertNotEquals(template.getId(), lastVersion.getId());
        assertEquals(template.getId(), sentSending.getConfig().getVariants().get(0).getTemplate());
        assertEquals(lastVersion.getId(), newSending.getConfig().getVariants().get(0).getTemplate());
    }

    /**
     * При удалении шаблона, используемого в отправленной рассылке
     * вместо удаления он просто отмечается как удаленный
     */
    @Test
    public void testDeleteTemplateUsedInSentSending() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());
        saveSentSending(template);

        requestDelete(template)
                .andExpect(status().isOk());

        template = blockTemplateService.get(template.getId());
        Assertions.assertTrue(template.isArchived());
    }

    /**
     * При удалении шаблона используемого в неотправленной рассылке
     * происходит удаление этого шаблона из БД
     */
    @Test
    public void testDeleteTemplateNotUsedInSentSending() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());

        requestDelete(template)
                .andExpect(status().isOk());

        Assertions.assertFalse(blockTemplateService.templateExists(template.getId()));
    }

    @Test
    public void testDeleteTemplateUsedInOldMessageTemplate() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());

        saveMessageTemplate(1, MessageTemplateState.DRAFT, config -> {
            BannerBlockConf block = new BannerBlockConf();
            block.setId("blockId");
            block.setTemplate(template.getId());
            config.setBlocks(Collections.singletonList(block));
        });

        saveMessageTemplate(2, config -> {
        });

        requestDelete(template)
                .andExpect(status().isOk());

        Assertions.assertTrue(blockTemplateService.get(template.getId()).isArchived());
    }

    @Test
    public void testArchivedIsNotReturnedInAllTemplatesList() throws Exception {
        BlockTemplate template = blockTemplate()
                .setId("iddqd")
                .setKey("key")
                .setVersion(1)
                .setArchived(true);
        blockTemplatesDao.insertTemplate(template);

        Map<TemplateType, List<BlockTemplateDto>> templates = requestTemplates();

        Assertions.assertTrue(templates.get(TemplateType.BANNER).isEmpty());
    }

    @Test
    public void testGetListTemplatesByType() throws Exception {
        blockTemplateService.saveTemplate(blockTemplate(TemplateType.ARTICLE));
        blockTemplateService.saveTemplate(blockTemplate(TemplateType.ARTICLE));
        blockTemplateService.saveTemplate(blockTemplate(TemplateType.ARTICLE));

        blockTemplateService.saveTemplate(blockTemplate(TemplateType.HEAD));
        blockTemplateService.saveTemplate(blockTemplate(TemplateType.BANNER));
        blockTemplateService.saveTemplate(blockTemplate(TemplateType.DYNAMIC_MODEL));
        var blockTemplate = blockTemplateService.saveTemplate(blockTemplate(TemplateType.FOOTER));
        blockTemplateService.saveTemplate(blockTemplate(TemplateType.INFO));
        blockTemplateService.saveTemplate(blockTemplate(TemplateType.MODEL));

        var result = requestTemplatesFromType(TemplateType.ARTICLE);

        assertEquals(3, result.size());
        for (var template : result) {
            assertNull(template.getBody());
            assertEquals(TemplateType.ARTICLE, template.getType());
        }

        var result2 = requestTemplatesFromType(TemplateType.FOOTER);

        assertEquals(1, result2.size());

        var template = result2.get(0);

        assertNull(template.getBody());
        assertEquals(blockTemplate.getId(), template.getId());
        assertEquals(blockTemplate.getKey(), template.getKey());
        assertEquals(blockTemplate.getName(), template.getName());
    }

    @Test
    public void testGetTemplateUsedInMailSendings() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());
        saveSentSending(template);
        EmailPlainSending sending2 = saveNotSentSending(template);

        BlockTemplateDto response = requestTemplate(template);

        assertEquals(template.getId(), response.getId());
        assertEquals(template.getKey(), response.getKey());
        assertEquals(template.getVersion(), response.getVersion());
        assertEquals(template.getType(), response.getType());
        assertEquals(template.getName(), response.getName());
        assertEquals(template.getBody(), response.getBody());
        assertEquals(template.getModificationTime().truncatedTo(ChronoUnit.SECONDS),
                response.getModificationTime());

        List<BlockTemplateDto.Usage> usages = response.getUsages();
        assertEquals(1, usages.size());
        assertUsage(UsingEntityType.EMAIL_SENDING, sending2.getName(), sending2.getId(), usages.get(0));
    }

    @Test
    public void testGetTemplatesUsedInEmailTemplates() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());

        BannerBlockConf block = new BannerBlockConf();
        block.setId("block_id");
        block.setTemplate(template.getId());

        saveMessageTemplate(
                1,
                config -> config.setBlocks(Collections.singletonList(block))
        );

        var messageTemplate = saveMessageTemplate(
                2,
                config -> config.setBlocks(Collections.singletonList(block))
        );

        BlockTemplateDto response = requestTemplate(template);

        List<BlockTemplateDto.Usage> usages = response.getUsages();
        assertEquals(1, usages.size());
        assertUsage(
                UsingEntityType.MESSAGE_TEMPLATE,
                messageTemplate.getName(),
                messageTemplate.getId(),
                usages.get(0)
        );
    }

    /**
     * Нельзя удалить шаблон используемый в еще не отправленной рассылке
     */
    @Test
    public void test400OnDeleteTemplateUsedInNotSentSending() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());
        saveNotSentSending(template);

        requestDelete(template)
                .andExpect(status().isBadRequest());
    }

    /**
     * Нельзя удалить шаблон используемый в актуальной версии шаблона сообщения
     */
    @Test
    public void test400OnDeleteTemplateUsedInActualMessageTemplate() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate());

        saveMessageTemplate(
                1,
                config -> {
                    BannerBlockConf block = new BannerBlockConf();
                    block.setId("block-id");
                    block.setTemplate(template.getId());

                    config.setBlocks(Collections.singletonList(block));
                }
        );

        requestDelete(template)
                .andExpect(status().isBadRequest());
    }

    /**
     * При удалении шаблона тела письма используемого в отправленной рассылке он архивируется
     */
    @Test
    public void testDeleteBodyTemplateUsedInSentSending() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate(TemplateType.HEAD));

        saveSentSending(config -> {
            EmailSendingVariantConf variant = new EmailSendingVariantConf();
            variant.setTemplate(template.getId());
            config.setVariants(Collections.singletonList(variant));
        });

        requestDelete(template)
                .andExpect(status().isOk());

        Assertions.assertTrue(blockTemplateService.get(template.getId()).isArchived());
    }

    /**
     * При удалении шаблона тела письма используемого в неактуальной версии шаблона
     * сообщения происходит его архивирование
     */
    @Test
    public void testDeleteBodyTemplateUsedInOldTemplateVersion() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate(TemplateType.HEAD));

        saveMessageTemplate(
                1,
                config -> config.setTemplate(template.getId())
        );

        saveMessageTemplate(
                2,
                config -> config.setTemplate(UUID.randomUUID().toString())
        );

        requestDelete(template)
                .andExpect(status().isOk());

        Assertions.assertTrue(blockTemplateService.get(template.getId()).isArchived());
    }

    /**
     * Нельзя удалить шаблон тела письма используемый в отправленной рассылке
     */
    @Test
    public void test400OnDeleteBodyTemplateUsedInNotSentSending() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate(TemplateType.HEAD));

        saveNotSentSending(config -> {
            EmailSendingVariantConf variant = new EmailSendingVariantConf();
            variant.setTemplate(template.getId());
            config.setVariants(Collections.singletonList(variant));
        });

        requestDelete(template)
                .andExpect(status().isBadRequest());
    }

    /**
     * Нельзя удалить шаблон тела письма используемый в актуальном шалоне сообщения
     */
    @Test
    public void test400OnDeleteBodyTemplateUsedInNewVersion() throws Exception {
        BlockTemplate template = blockTemplateService.saveTemplate(blockTemplate(TemplateType.HEAD));

        saveMessageTemplate(
                1,
                config -> config.setTemplate(template.getId())
        );

        requestDelete(template)
                .andExpect(status().isBadRequest());
    }

    private BlockTemplateDto requestTemplate(BlockTemplate template) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/templates/{id}", template.getId())
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        return jsonDeserializer.readObject(
                BlockTemplateDto.class,
                result.getResponse().getContentAsByteArray()
        );
    }

    private Map<TemplateType, List<BlockTemplateDto>> requestTemplates() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/templates")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        TemplatesResult templatesResult = jsonDeserializer.readObject(
                TemplatesResult.class,
                result.getResponse().getContentAsByteArray()
        );

        return templatesResult.getTemplates();
    }

    private List<BlockTemplate> requestTemplatesFromType(TemplateType templateType) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/templates/by-type/" + templateType.name())
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        return jsonDeserializer.readObject(
                new TypeReference<>() {},
                result.getResponse().getContentAsByteArray()
        );

    }

    private MessageTemplate<EmailMessageConf> saveMessageTemplate(int version,
                                                                  MessageTemplateState state,
                                                                  Consumer<EmailMessageConf> configurator) {
        EmailMessageConf config = new EmailMessageConf();
        configurator.accept(config);

        var messageTemplate = new MessageTemplate<EmailMessageConf>();
        messageTemplate.setType(MessageTemplateType.EMAIL);
        messageTemplate.setId(UUID.randomUUID().toString());
        messageTemplate.setKey("key");
        messageTemplate.setConfig(config);
        messageTemplate.setVersion(version);
        messageTemplate.setName("Message template");
        messageTemplate.setState(state);

        messageTemplatesDAO.save(messageTemplate);
        return messageTemplate;
    }

    private MessageTemplate<EmailMessageConf> saveMessageTemplate(int version, Consumer<EmailMessageConf> configurator) {
        return saveMessageTemplate(version, MessageTemplateState.PUBLISHED, configurator);
    }

    private EmailPlainSending saveNotSentSending(Consumer<EmailSendingConf> configSetter) {
        Campaign campaign = new Campaign();
        campaign.setName("Test Campaign");
        campaign = campaignDAO.insert(campaign);

        EmailSendingConf sendingConf = new EmailSendingConf();
        sendingConf.setSubscriptionType(2L);
        configSetter.accept(sendingConf);

        EmailPlainSending sending = new EmailPlainSending();
        sending.setCampaignId(campaign.getId());
        sending.setConfig(sendingConf);
        sending.setId(UUID.randomUUID().toString());
        sending.setName("sending");

        emailSendingDAO.createSending(sending);

        sending.setStageAndStatus(SendingStage.NEW, StageStatus.FINISHED);
        emailSendingDAO.updateSendingStates(sending.getId(), sending);

        return sending;
    }

    private EmailPlainSending saveNotSentSending(BlockTemplate usingTemplate) {
        return saveNotSentSending(config -> useBlockInConfig(usingTemplate, config));
    }

    private EmailPlainSending saveSentSending(BlockTemplate usingTemplate) {
        return saveSentSending(config -> useBlockInConfig(usingTemplate, config));
    }

    private void useBlockInConfig(BlockTemplate usingTemplate, EmailSendingConf config) {
        BlockConf blockConf = new BannerBlockConf();
        blockConf.setId("block-id");
        blockConf.setTemplate(usingTemplate.getId());

        EmailSendingVariantConf variantConf = new EmailSendingVariantConf();
        variantConf.setId("a");
        variantConf.setBlocks(Collections.singletonList(blockConf));

        config.setVariants(Collections.singletonList(variantConf));
    }

    private EmailPlainSending saveSentSending(Consumer<EmailSendingConf> configSetter) {
        Campaign campaign = new Campaign();
        campaign.setName("Test Campaign");
        campaign = campaignDAO.insert(campaign);

        EmailSendingConf sendingConf = new EmailSendingConf();
        sendingConf.setSubscriptionType(2L);
        configSetter.accept(sendingConf);

        EmailPlainSending sending = new EmailPlainSending();
        sending.setCampaignId(campaign.getId());
        sending.setConfig(sendingConf);
        sending.setId(UUID.randomUUID().toString());
        sending.setName("sending");

        emailSendingDAO.createSending(sending);

        EmailSendingFactInfo factInfo = new EmailSendingFactInfo();
        factInfo.setId(UUID.randomUUID().toString());
        factInfo.setStatus(SendingFactStatus.SENDING_IN_PROGRESS);
        factInfo.setType(SendingFactType.FINAL);
        factInfo.setSendingId(sending.getId());
        factInfo.setAuthor("apershukov");
        emailSendingFactInfoDAO.save(factInfo);

        return sending;
    }

    private BlockTemplateDto saveTemplate(BlockTemplate template) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(template))
        ).andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        return jsonDeserializer.readObject(
                BlockTemplateDto.class,
                mvcResult.getResponse().getContentAsString()
        );
    }

    @Nonnull
    private ResultActions requestDelete(BlockTemplate template) throws Exception {
        return mockMvc.perform(delete("/api/templates/{id}", template.getId())
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print());
    }

    private MessageTemplate<EmailMessageConf> getTemplate(MessageTemplate<EmailMessageConf> oldTemplate) {
        return messageTemplatesDAO.<EmailMessageConf>getTemplateById(oldTemplate.getId()).orElseThrow();
    }
}
