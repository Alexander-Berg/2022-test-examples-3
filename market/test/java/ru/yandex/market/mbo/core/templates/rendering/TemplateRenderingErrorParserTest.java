package ru.yandex.market.mbo.core.templates.rendering;

import Market.Gumoful.TemplateRendering;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplateType;
import ru.yandex.market.mbo.gwt.models.visual.templates.rendering.ErrorPositionInfo;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * created on 09.11.2016
 */
@SuppressWarnings("all")
public class TemplateRenderingErrorParserTest {

    private OutputTemplateType type = OutputTemplateType.PUT_MODEL;

    @Test
    public void oneByte() {
        Map<OutputTemplateType, String> templatesContent = new EnumMap<>(OutputTemplateType.class);
        templatesContent.put(type, "template");
        TemplateRenderingErrorParser errorParser = new TemplateRenderingErrorParser(templatesContent);
        List<ErrorPositionInfo> infos = errorParser.parse(error(1, 1), type);

        assertEquals(1, infos.size());

        ErrorPositionInfo info = infos.get(0);
        assertEquals(1, info.getStartPosition());
        assertEquals(2, info.getEndPosition());
    }

    @Test
    public void multipleBytes() {
        Map<OutputTemplateType, String> templatesContent = new EnumMap<>(OutputTemplateType.class);
        templatesContent.put(type, "шаблон");
        TemplateRenderingErrorParser errorParser = new TemplateRenderingErrorParser(templatesContent);
        List<ErrorPositionInfo> infos = errorParser.parse(error(2, 2), type);

        assertEquals(1, infos.size());

        ErrorPositionInfo info = infos.get(0);
        assertEquals(1, info.getStartPosition());
        assertEquals(2, info.getEndPosition());
    }

    @Test
    public void multipleAfterOneByte() {
        Map<OutputTemplateType, String> templatesContent = new EnumMap<>(OutputTemplateType.class);
        templatesContent.put(type, "temp шаблон");
        TemplateRenderingErrorParser errorParser = new TemplateRenderingErrorParser(templatesContent);
        List<ErrorPositionInfo> infos = errorParser.parse(error(7, 2), type);

        assertEquals(1, infos.size());

        ErrorPositionInfo info = infos.get(0);
        assertEquals(6, info.getStartPosition());
        assertEquals(7, info.getEndPosition());
    }

    @Test
    public void undefinedObjectName() {
        String raw = "temp шаблон temp, hello s_template1 size";
        String searchWord = "temp";

        Map<OutputTemplateType, String> templatesContent = new EnumMap<>(OutputTemplateType.class);
        templatesContent.put(type, raw);

        TemplateRenderingErrorParser errorParser = new TemplateRenderingErrorParser(templatesContent);
        List<ErrorPositionInfo> infos = errorParser.parse(error(0, 0, searchWord), type);

        assertEquals(3, infos.size());

        ErrorPositionInfo first = infos.get(0);
        ErrorPositionInfo second = infos.get(1);
        ErrorPositionInfo third = infos.get(2);

        int searchWordIndex = raw.indexOf(searchWord);
        assertEquals(searchWordIndex, first.getStartPosition());
        assertEquals(searchWordIndex + searchWord.length(), first.getEndPosition());

        searchWordIndex = raw.indexOf(searchWord, searchWordIndex + searchWord.length());
        assertEquals(searchWordIndex, second.getStartPosition());
        assertEquals(searchWordIndex + searchWord.length(), second.getEndPosition());

        searchWordIndex = raw.indexOf(searchWord, searchWordIndex + searchWord.length());
        assertEquals(searchWordIndex, third.getStartPosition());
        assertEquals(searchWordIndex + searchWord.length(), third.getEndPosition());
    }

    private static TemplateRendering.TError error(int offset, int length) {
        return error(offset, length, "");
    }

    private static TemplateRendering.TError error(int offset, int length, String objectName) {
        return TemplateRendering.TError.newBuilder()
            .setPosition(TemplateRendering.TPosition.newBuilder()
                .setOffset(offset)
                .setLength(length)
            )
            .setUndefinedObjectName(objectName)
            .build();
    }
}
