package ru.yandex.market.crm.campaign.services.sending;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.TestExternalServicesConfig;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.placeholders.AppPropertiesConfiguration;
import ru.yandex.market.crm.campaign.services.gen.UniformGlobalSplittingDescription;
import ru.yandex.market.crm.campaign.services.sending.context.EmailPlainSendingContext;
import ru.yandex.market.crm.campaign.services.sending.email.EmailSendingYtPaths;
import ru.yandex.market.crm.campaign.services.sending.template.TemplateRenderer;
import ru.yandex.market.crm.campaign.services.sending.template.YaSenderTemplateCreator;
import ru.yandex.market.crm.campaign.services.templates.BlockTemplateService;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.InfoBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.ModelBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.StaticModelBlockConf;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.crm.core.domain.templates.TemplateType;
import ru.yandex.market.crm.core.services.external.yandexsender.YaSenderApiClient;
import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.core.services.sending.UtmLinks;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.mapreduce.domain.ImageLink;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUser;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelType;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BlockData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.ModelBlockData;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderData;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderDataRow;
import ru.yandex.market.crm.templates.TemplateService;
import ru.yandex.market.mcrm.http.HttpClientConfiguration;

import static org.mockito.Mockito.mock;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {
        AppPropertiesConfiguration.class,
        TestEnvironmentResolver.class,
        HttpClientConfiguration.class,
        TestExternalServicesConfig.class,
        TemplateService.class,
        StaticBlockDataFactory.class,
        JacksonConfig.class
})
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class YaSenderTemplateRenderServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(YaSenderTemplateRenderServiceTest.class);

    @Inject
    private YaSenderApiClient client;
    @Inject
    private TemplateService templateService;
    @Inject
    private StaticBlockDataFactory staticBlockDataFactory;
    @Inject
    private JsonSerializer jsonSerializer;
    //@Inject
    private YaSenderTemplateCreator yaSenderTemplateCreator;

    @Mock
    private BlockTemplateService blockTemplateService;

    @Test
    public void checkTemplateForYaSender() {
        EmailPlainSending sending = new EmailPlainSending();
        sending.setId("test_sending");
        EmailSendingConf conf = new EmailSendingConf();
        conf.setSubscriptionType(2L);
        conf.setVariants(Lists.newArrayList(
                createVariant("a"),
                createVariant("b")
        ));

        EmailPlainSendingContext ctx = new EmailPlainSendingContext(
                sending, conf, "test",
                Collections.emptySet(), null, mock(UniformGlobalSplittingDescription.class),
                new EmailSendingYtPaths(YPath.cypressRoot()),
                SubscriptionTypes.ADVERTISING
        );

        String template = yaSenderTemplateCreator.createTemplate(
                sending.getId(),
                LocalDateTime.now(),
                conf.getVariants()
        ).getContent();

        LOG.debug("Шаблон рассылятора:\n" + template);

        EmailSendingVariantConf var = ctx.getVariant("a");
        YaSenderData yasenderData = YaSenderData.create(var.getId(), createUserData(ctx));
        LOG.debug("Данные для рассылятора:\n" + jsonSerializer.writeObjectAsString(yasenderData));
        String res = client.render(template, new YaSenderDataRow(null, yasenderData, null));
        LOG.debug("Результат рендеринга:\n" + res);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        List<BlockTemplate> templates = Lists.newArrayList(
                new BlockTemplate("head", TemplateType.HEAD,
                        """
                        <html>
                            <head>
                            <title>${ctx.subject()}</title>
                            </head>
                            <body>
                            <div class="mail_preheader">
                                ${ctx.preheader()}
                            </div>
                            <div class="date">
                                ${ctx.date()} ---- ${ctx.time()}
                            </div>
                            <%= renderBlocks('<!-- separator -->') %>
                            <a href="${ctx.unsubscribe()}">Отказаться</a> от рассылки
                            <a href="http://market.yandex.ru">Маркет</a>
                            </body>
                        </html>"""
                ),
                new BlockTemplate("ban", TemplateType.BANNER,
                        """
                        <div>${data.title}</div>
                        <span>Здравствуйте!</span>
                        <div>${data.text}</div>
                        <a href="${data.banners[0].link}">
                            <img src="${data.banners[0].img}" alt="${data.banners[0].alt}"/>
                        </a>"""
                ),
                new BlockTemplate("info", TemplateType.INFO,
                        """
                        <div id="info">
                            <span>${data.title}</span>
                            ${data.html}
                        </div>"""
                ),
                new BlockTemplate("models", TemplateType.MODEL,
                        """
                        <div id="models">
                            <span>${data.title}</span>
                            <a href="${data.banner.link}">
                            <img src="${data.banner.img}" alt="${data.banner.alt}"/>
                            </a>
                        <% for(def m in data.models) { %>
                            <div id="model_${m.id}">
                            <a href="${m.link}">
                                <img src="${m.img}" alt="${m.name}"/>
                            </a>
                            Name: ${m.name}
                            Price: ${m.price}
                            Discount: ${m.discount}
                            </div>
                        <% } %>
                        </div>"""
                )
        );
        Mockito.when(blockTemplateService.get(Mockito.anyString()))
                .then(invocation -> {
                    String id = invocation.getArgument(0, String.class);
                    return Iterables.find(templates, t -> id.equals(t.getId()));
                });
        TemplateRenderer templateRenderer = new TemplateRenderer(templateService);
        this.yaSenderTemplateCreator = new YaSenderTemplateCreator(blockTemplateService, templateRenderer,
                staticBlockDataFactory);
    }

    private List<BlockData> createBlockDatas(EmailSendingVariantConf variantConf) {
        return variantConf.getBlocks().stream()
                .map(blockConf -> {
                    switch (blockConf.getType()) {
                        case MODEL:
                            return createModelBlockData((ModelBlockConf) blockConf);
                        default:
                            return staticBlockDataFactory.apply(blockConf);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private BannerBlockConf createCreativeConf() {
        BannerBlockConf blockConf = new BannerBlockConf();
        blockConf.setId("creative");
        blockConf.setTemplate("ban");
        blockConf.setTitle("creative title");
        blockConf.setText("creative text");
        blockConf.setBanners(Lists.newArrayList(new ImageLink("http://yandex.ru", "https://ya.img/creative.jpg",
                "ban_alt")));
        return blockConf;
    }

    private InfoBlockConf createInfoConf() {
        InfoBlockConf conf = new InfoBlockConf();
        conf.setId("info");
        conf.setTitle("info title");
        conf.setTemplate("info");
        conf.setHtml("<a href=\"http://yandex.ru/my\">Моя страница</a>");
        return conf;
    }

    private ModelInfo createModel(String id) {
        ModelInfo model = new ModelInfo(id);
        model.setType(ModelType.MODEL);
        model.setName("test model " + id);
        model.setHid(555);
        model.setImg("https://ya.img/models/" + id + ".jpg");
        model.setLink("https://market.yandex.ru/product/" + id);
        model.setPrice("999");
        model.setDiscount("20%");
        return model;
    }

    private ModelBlockConf createModelBlockConf() {
        ModelBlockConf conf = new ModelBlockConf();
        conf.setId("models_block");
        conf.setTitle("models title");
        conf.setTemplate("models");
        conf.setModelCount(2);
        return conf;
    }

    private ModelBlockData createModelBlockData(ModelBlockConf conf) {
        ModelBlockData modelBlock = new ModelBlockData();
        modelBlock.setId(conf.getId());
        modelBlock.setTitle(conf.getTitle());
        List<ModelInfo> models = new ArrayList<>();
        for (int i = 0; i < conf.getModelCount(); ++i) {
            models.add(createModel("888" + i));
        }
        modelBlock.setModels(models);
        modelBlock.setBanner(new ImageLink("http://yandex.ru/catalog/888", "https://ya.img/catalogs/888.jpg",
                "catalog888_alt"));
        return modelBlock;
    }

    private StaticModelBlockConf createStaticModelBlockConf() {
        StaticModelBlockConf conf = new StaticModelBlockConf();
        conf.setId("stm");
        conf.setTitle("static models title");
        conf.setTemplate("models");
        conf.setBanner(new ImageLink("http://yandex.ru/catalog/111", "https://ya.img/catalogs/111.jpg", "catalog_alt"));
        conf.setModels(Lists.newArrayList(
                createModel("777"),
                createModel("111")
        ));
        return conf;
    }

    private CampaignUserData createUserData(EmailPlainSendingContext ctx) {
        List<BlockData> blocks = createBlockDatas(ctx.getVariant("a"));

        UtmLinks.forEmailSending(ctx.getId(), null)
                .withEmail("user@ya.ru")
                .updateLinks(blocks);

        return new CampaignUserData()
                .setUserInfo(
                        new CampaignUser("a")
                                .setEmail("user@ya.ru")
                                .setUnsubscribeAdvertising("https://yandex.ru/my/unsubscribe")
                )
                .setBlocks(blocks);
    }

    private EmailSendingVariantConf createVariant(String id) {
        EmailSendingVariantConf variantConf = new EmailSendingVariantConf();
        variantConf.setId(id);
        variantConf.setTemplate("head");
        variantConf.setSubject("Тема письма " + id);
        variantConf.setPreheader("Предзаголовок " + id);
        variantConf.setBlocks(Lists.newArrayList(
                createCreativeConf(),
                createInfoConf(),
                createStaticModelBlockConf(),
                createModelBlockConf()
        ));
        return variantConf;
    }
}
