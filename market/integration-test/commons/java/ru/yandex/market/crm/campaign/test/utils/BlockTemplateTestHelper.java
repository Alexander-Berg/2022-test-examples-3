package ru.yandex.market.crm.campaign.test.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.campaign.services.templates.BlockTemplateService;
import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.domain.CommonDateRange;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.InfoBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.ModelBlockConf;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.crm.core.domain.templates.TemplateType;
import ru.yandex.market.crm.mapreduce.domain.ImageLink;

/**
 * @author apershukov
 */
@Component
public class BlockTemplateTestHelper {

    private final BlockTemplateService templateService;

    public BlockTemplateTestHelper(BlockTemplateService templateService) {
        this.templateService = templateService;
    }

    public String prepareMessageTemplate() {
        BlockTemplate template = new BlockTemplate()
                .setType(TemplateType.HEAD)
                .setName("Test variant template")
                .setBody(
                        """
                        <body>
                        <div>
                            <%
                            def blocksHtml = renderBlocks('''
                            <!-- padding --><div style="height: 24px; line-height: 24px; font-size: 8px;">&nbsp;</div>
                            ''');
                            %>
                            ${blocksHtml}
                        </div>
                        <a href="${ctx.unsubscribe()}">unsubscribe link</a>
                        <img src="">
                        </body>"""
                );

        return templateService.saveTemplate(template).getId();
    }

    public ModelBlockConf prepareModelBlock(int modelCount) {
        BlockTemplate template = new BlockTemplate()
                .setType(TemplateType.MODEL)
                .setName("Test template")
                .setBody("<div>some body</div>");

        BlockTemplate blockTemplate = templateService.saveTemplate(template);

        ModelBlockConf modelBlockConf = new ModelBlockConf();
        modelBlockConf.setId("model_block");
        modelBlockConf.setAlgorithm("recommendationsByViews");
        modelBlockConf.setModelCount(modelCount);
        modelBlockConf.setTemplate(blockTemplate.getId());
        modelBlockConf.setColor(Color.GREEN);
        modelBlockConf.setProperties(
                ImmutableMap.of("period", new CommonDateRange(30))
        );
        return modelBlockConf;
    }

    public BlockTemplate prepareBannerBlockTemplate() {
        BlockTemplate template = new BlockTemplate()
                .setType(TemplateType.BANNER)
                .setName("Test template")
                .setBody("<div>${data.text}</div>");

        return templateService.saveTemplate(template);
    }

    public BannerBlockConf prepareCreativeBlock() {
        return prepareCreativeBlock(null);
    }

    public BannerBlockConf prepareCreativeBlock(String text) {
        BlockTemplate blockTemplate = prepareBannerBlockTemplate();

        BannerBlockConf creative = new BannerBlockConf();
        creative.setId("creative");
        creative.setTemplate(blockTemplate.getId());
        creative.setText(text);
        creative.setBanners(
                Collections.singletonList(
                        new ImageLink("", "http://yandex.ru/image", "")
                )
        );
        return creative;
    }

    public InfoBlockConf prepareInfoBlock(String innerHtml) {
        BlockTemplate blockTemplate = new BlockTemplate();
        blockTemplate.setType(TemplateType.INFO);
        blockTemplate.setName("Test template");
        blockTemplate.setBody("<div>${data.html}</div>");
        blockTemplate = templateService.saveTemplate(blockTemplate);

        InfoBlockConf block = new InfoBlockConf();
        block.setId(UUID.randomUUID().toString().replace("-", "_"));
        block.setTemplate(blockTemplate.getId());
        block.setHtml(innerHtml);

        return block;
    }

    public BlockTemplate saveBlockTemplate(String name, TemplateType model, String file) {
        BlockTemplate blockTemplate = new BlockTemplate()
                .setName(name)
                .setType(model)
                .setBody(resourceAsString("/templates/" + file));

        return templateService.saveTemplate(blockTemplate);
    }

    public BlockTemplate updateBlockTemplate(String templateId, String body) {
        BlockTemplate blockTemplate = templateService.get(templateId);
        blockTemplate.setBody(body);
        return templateService.saveTemplate(blockTemplate);
    }

    private String resourceAsString(String path) {
        try {
            return IOUtils.toString(
                    getClass().getResourceAsStream(path),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
