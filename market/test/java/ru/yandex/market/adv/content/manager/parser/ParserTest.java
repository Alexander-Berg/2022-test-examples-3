package ru.yandex.market.adv.content.manager.parser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cms.client.mock.CmsMockClient;
import ru.yandex.cms.client.model.CmsTemplate;
import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.adv.content.manager.mapper.template.TemplateModelMapper;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationContentModel;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationContentRuleModel;
import ru.yandex.market.adv.content.manager.model.parser.ParsedEntry;
import ru.yandex.market.adv.content.manager.model.parser.ParsedField;
import ru.yandex.market.adv.content.manager.model.parser.ParsedFieldType;
import ru.yandex.market.adv.content.manager.parser.factory.ParserFactory;
import ru.yandex.market.adv.content.manager.parser.handler.entry.ParserEntryHandler;
import ru.yandex.market.adv.content.manager.parser.handler.field.ModerationFieldHandler;
import ru.yandex.market.adv.content.manager.parser.handler.field.ParserFieldHandler;
import ru.yandex.market.adv.content.manager.parser.interceptor.BusinessFillerParserEntryInterceptor;
import ru.yandex.mj.generated.server.model.Template;

import static ru.yandex.market.adv.content.manager.constant.ModerationConstants.MODERATION_RULE_CMS_IMAGE;
import static ru.yandex.market.adv.content.manager.constant.ModerationConstants.MODERATION_RULE_CMS_TEXT;

public class ParserTest extends AbstractContentManagerTest {

    @Autowired
    private TemplateModelMapper templateModelMapper;
    @Autowired
    private CmsMockClient cmsMockClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ParserFactory parserFactory;

    @DisplayName("Парсинг входящего документа с тестовым handler-ом.")
    @Test
    void parse_list_success() {
        var testFieldHandler = new ParserFieldHandler() {
            private final HashMap<String, ParsedField> fieldsMap = new HashMap<>();

            @Override
            public void handle(@Nonnull ParsedField field) {
                fieldsMap.put(field.getId(), field);
            }
        };

        var testEntryHandler = new ParserEntryHandler() {
            private final HashMap<String, ParsedEntry> entriesMap = new HashMap<>();

            @Override
            public void handle(@Nonnull ParsedEntry entry) {
                entriesMap.put(entry.getId(), entry);
            }
        };

        String file = loadFile("json/parser_banner_carousel_only_template.json");
        CmsTemplate cmsTemplate = cmsMockClient.parseCmsTemplate(file);
        Template template = templateModelMapper.map(cmsTemplate);

        parserFactory.create(
                        List.of(testFieldHandler),
                        List.of(testEntryHandler),
                        List.of(new BusinessFillerParserEntryInterceptor("433"))
                )
                .parse(template.getIncludes());

        Assertions.assertThat(testEntryHandler.entriesMap)
                .containsAllEntriesOf(Map.of(
                        "107142645",
                        new ParsedEntry("107142645", "PICTURE"),
                        "107142644",
                        new ParsedEntry("107142644",
                                "#/definitions/../src/legacy/modules/DataCollector/garsons/" +
                                        "RootMediaSet/PictureBannerSlideCms"),
                        "107142646",
                        new ParsedEntry("107142646",
                                "#/definitions/../src/widgets/content/BannerCarousel/index/WidgetProps"),
                        "107142625",
                        new ParsedEntry("107142625", "PAGE_LINKS"),
                        "107142624",
                        new ParsedEntry("107142624", "BRAND_DESKTOP"),
                        "1071951324",
                        new ParsedEntry("1071951324", "PAGE_LINK_BUSINESS_ID")
                ));
        Assertions.assertThat(testFieldHandler.fieldsMap)
                .containsAllEntriesOf(Map.of(
                        "1071951324_BUSINESS_ID_0",
                        new ParsedField("1071951324_BUSINESS_ID_0", "BUSINESS_ID", ParsedFieldType.UNKNOWN,
                                "433", null,
                                new ParsedEntry("1071951324", "PAGE_LINK_BUSINESS_ID")),
                        "107142624_LINKS_0",
                        new ParsedField("107142624_LINKS_0", "LINKS", ParsedFieldType.REFERENCE,
                                "107142625", null,
                                new ParsedEntry("107142624", "BRAND_DESKTOP")),
                        "107142625_PAGE_LINK_0",
                        new ParsedField("107142625_PAGE_LINK_0", "PAGE_LINK", ParsedFieldType.REFERENCE,
                                "107142626", null,
                                new ParsedEntry("107142625", "PAGE_LINKS")),
                        "107142646_customHeight_0",
                        new ParsedField("107142646_customHeight_0", "customHeight", ParsedFieldType.UNKNOWN,
                                "300", null, new ParsedEntry("107142646",
                                "#/definitions/../src/widgets/content/BannerCarousel/index/WidgetProps")),
                        "107142644_link_0",
                        new ParsedField("107142644_link_0", "link", ParsedFieldType.UNKNOWN,
                                "https://yandex.ru", null, new ParsedEntry("107142644",
                                "#/definitions/../src/legacy/modules/DataCollector/garsons/" +
                                        "RootMediaSet/PictureBannerSlideCms")),
                        "107142645_IMAGE_URL_0",
                        new ParsedField("107142645_IMAGE_URL_0", "IMAGE_URL", ParsedFieldType.UNKNOWN,
                                "//avatars.mds.yandex.net/get-marketcms/1534436" +
                                        "/img-137667c4-e196-4b5e-abc0-48e6e5c9c5db.png/optimize", null,
                                new ParsedEntry("107142645", "PICTURE"))
                ));
    }

    @DisplayName("Парсинг входящего документа с moderation handler-ом.")
    @Test
    void parse_moderationHandler_moderationTypes() throws IOException {
        ModerationFieldHandler moderationHandler = new ModerationFieldHandler(
                Map.of(
                        ParsedFieldType.TEXT, MODERATION_RULE_CMS_TEXT,
                        ParsedFieldType.IMAGE, MODERATION_RULE_CMS_IMAGE
                )
        );
        Template template = objectMapper.readValue(loadFile("json/moderation_handler_types.json"), Template.class);

        parserFactory.create(
                        List.of(moderationHandler),
                        List.of(),
                        List.of(new BusinessFillerParserEntryInterceptor("433"))
                )
                .parse(template.getIncludes());

        Map<String, ModerationContentModel> fieldsMap = moderationHandler.getFieldsToModerateMap();
        Map<String, ModerationContentModel> expectedMap = Map.of(
                "107142645_TEXTS_0",
                new ModerationContentModel("0", new ModerationContentRuleModel(MODERATION_RULE_CMS_TEXT)),
                "107142645_TEXTS_1",
                new ModerationContentModel("1", new ModerationContentRuleModel(MODERATION_RULE_CMS_TEXT)),
                "107142645_IMAGE_URL_0",
                new ModerationContentModel("//avatars.mds.yandex.net/get-marketcms/1534436" +
                        "/img-137667c4-e196-4b5e-abc0-48e6e5c9c5db.png/optimize",
                        new ModerationContentRuleModel(MODERATION_RULE_CMS_IMAGE)),
                "107142645_IMAGE_ALT_0",
                new ModerationContentModel("Подпись",
                        new ModerationContentRuleModel(MODERATION_RULE_CMS_TEXT))
        );

        Assertions.assertThat(fieldsMap)
                .containsExactlyInAnyOrderEntriesOf(expectedMap);
    }
}
