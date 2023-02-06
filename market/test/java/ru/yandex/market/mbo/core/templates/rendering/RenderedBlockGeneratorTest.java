package ru.yandex.market.mbo.core.templates.rendering;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.visual.templates.rendering.OutputTemplateRenderingResult;
import ru.yandex.market.mbo.gwt.models.visual.templates.rendering.RenderedBlock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dmserebr
 * @date 03/09/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RenderedBlockGeneratorTest {
    @Test
    public void testValidXml() throws IOException  {
        String xml = "<ya_guru_modelcard>" +
            "<block name=\"Состав\">" +
            "<spec_guru_modelcard>" +
            "<name><![CDATA[Радиоприемник]]></name>" +
            "<value><![CDATA[есть, цифровой тюнер]]></value>" +
            "</spec_guru_modelcard>" +
            "</block>" +
            "</ya_guru_modelcard>";

        OutputTemplateRenderingResult renderingResult = OutputTemplateRenderingResult.result(
            xml, Collections.emptyList());

        List<RenderedBlock> blocks = RenderedBlockGenerator.generateBlocks(renderingResult);

        Assertions.assertThat(blocks).hasSize(1);
        Assertions.assertThat(blocks.get(0).getItems()).containsExactly(
            new RenderedBlock.Item("Радиоприемник", "есть, цифровой тюнер"));
    }

    @Test
    public void testInvalidXml() throws IOException  {
        String xml = "<ya_guru_modelcard>" +
            "<block name=\"Состав\">" +
            "<spec_guru_modelcard>" +
            "<name><![CDATA[Радиоприемник]]></name>" +
            "<value><![CDATA[есть, цифровой тюнер]]></value>" +
            "</spec_guru_modelcard>";

        OutputTemplateRenderingResult renderingResult = OutputTemplateRenderingResult.result(
            xml, Collections.emptyList());

        List<RenderedBlock> blocks = RenderedBlockGenerator.generateBlocks(renderingResult);

        Assertions.assertThat(blocks).isEmpty();
    }

    @Test
    public void testRealData() throws IOException  {
        InputStream inputStream = RenderedBlockGeneratorTest.class.getResourceAsStream(
            "/mbo-core/templates/template-90404.xml");
        String xml = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        OutputTemplateRenderingResult renderingResult = OutputTemplateRenderingResult.result(
            xml, Collections.emptyList());

        List<RenderedBlock> blocks = RenderedBlockGenerator.generateBlocks(renderingResult);

        Assertions.assertThat(blocks).hasSize(8);
        Assertions.assertThat(blocks.stream().map(RenderedBlock::getName).collect(Collectors.toList()))
            .containsExactly("Состав", "Общие характеристики", "Поддерживаемые носители и форматы",
                "Дисплей", "Управление", "Интерфейсы", "Тюнер", "Дополнительно");
        Assertions.assertThat(blocks.stream().map(b -> b.getItems().size()).collect(Collectors.toList()))
            .containsExactly(9, 4, 4, 3, 4, 6, 3, 6);
        Assertions.assertThat(blocks.get(0).getItems().get(0)).isEqualTo(
            new RenderedBlock.Item("Радиоприемник", "есть, цифровой тюнер"));
        Assertions.assertThat(blocks.get(0).getItems().get(8)).isEqualTo(
            new RenderedBlock.Item("ТВ-тюнер", "есть"));
        Assertions.assertThat(blocks.get(1).getItems().get(0)).isEqualTo(
            new RenderedBlock.Item("Пиковая мощность", "4x50 Вт"));
        Assertions.assertThat(blocks.get(1).getItems().get(3)).isEqualTo(
            new RenderedBlock.Item("Интерфейс CD-ченджера", "нет"));
        Assertions.assertThat(blocks.get(5).getItems().get(5)).isEqualTo(
            new RenderedBlock.Item("Поддержка профиля A2DP", "есть"));
    }
}
