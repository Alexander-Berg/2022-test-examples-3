package ru.yandex.market.mbo.db.templates.generator;

import org.assertj.core.api.Assertions;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplate;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class OutputTemplateAssert {
    private final OutputTemplate outputTemplate;

    private OutputTemplateAssert(OutputTemplate outputTemplate) {
        this.outputTemplate = outputTemplate;
    }

    public OutputTemplateAssert notNull() {
        Assertions.assertThat(outputTemplate)
            .withFailMessage("Expected template not to be null")
            .isNotNull();
        return this;
    }

    public OutputTemplateAssert contentNotNullOrEmpty() {
        notNull();

        Assertions.assertThat(outputTemplate.getContent())
            .withFailMessage("Expected template.content not to be null or empty")
            .isNotEmpty();
        return this;
    }

    public OutputTemplateAssert contentNullOrEmpty() {
        notNull();

        Assertions.assertThat(outputTemplate.getContent())
            .withFailMessage("Expected template.content to be null or empty")
            .isNullOrEmpty();
        return this;
    }

    public OutputTemplateAssert contentContainsBlock(String blockName) {
        contentNotNullOrEmpty();

        Assertions.assertThat(containsBlock(blockName, outputTemplate.getContent()))
            .withFailMessage("Expected to contain block: '%s'.\n\nContent:\n%s", blockName, outputTemplate.getContent())
            .isTrue();
        return this;
    }

    public OutputTemplateAssert contentDoesntContainBlock(String blockName) {
        contentNotNullOrEmpty();

        Assertions.assertThat(containsBlock(blockName, outputTemplate.getContent()))
            .withFailMessage("Expected NOT to contain block: '%s'.\n\nContent:\n%s",
                blockName, outputTemplate.getContent())
            .isFalse();
        return this;
    }


    public OutputTemplateAssert contentContainsExactlyPartnerParamsInBlock(String blockName,
                                                                           boolean isPartner,
                                                                           String... xslNames) {
        return contentContainsExactlyPartnerParamsInBlock(blockName, isPartner, Arrays.asList(xslNames));
    }

    // {is_partner#ifnz}
    // {color_glob#ifnz}
    //   <spec_guru_modelcard>
    //     <name><![CDATA[color_glob]]></name>
    //     <value><![CDATA[{color_glob}]]></value>
    //   </spec_guru_modelcard>
    // {#endif}
    // {#endif}
    public OutputTemplateAssert contentContainsExactlyPartnerParamsInBlock(String blockName,
                                                                           boolean isPartner,
                                                                           List<String> xslNames) {
        contentContainsBlock(blockName);

        String contentBlock = getBlock(blockName, outputTemplate.getContent());
        List<String> contentXslNames = new ArrayList<>();

        Matcher partnerSectionMatcher = Pattern.compile(
            "\\{" + XslNames.IS_PARTNER + "#" + (isPartner ? "ifnz" : "ifz") + "}(.+?\\{#endif})\\s*\\{#endif}",
            Pattern.DOTALL)
            .matcher(contentBlock);
        while (partnerSectionMatcher.find()) {
            String parametersSection = partnerSectionMatcher.group(1);
            Matcher parameterMatcher = Pattern.compile("\\{([A-Za-z0-9_-]+)#ifnz}")
                .matcher(parametersSection);
            while (parameterMatcher.find()) {
                contentXslNames.add(parameterMatcher.group(1));
            }
        }

        Assertions.assertThat(contentXslNames)
            .describedAs("Failed to find xslNames in block:\n" + contentBlock)
            .containsExactlyElementsOf(xslNames);
        return this;
    }

    /**
     * Проверяет, что перечислены только конкретные параметры с учетом порядка.
     */
    public OutputTemplateAssert contentContainsExactlyParamsInBlock(String blockName, String... xslNames) {
        return contentContainsExactlyParamsInBlock(blockName, Arrays.asList(xslNames));
    }

    /**
     * Проверяет, что перечислены только конкретные параметры с учетом порядка в формате.
     */
    // {color_glob#ifnz}
    //  <spec_guru_modelcard>
    //    <name><![CDATA[color_glob]]></name>
    //    <value><![CDATA[{color_glob}]]></value>
    //  </spec_guru_modelcard>
    //  {#endif}
    public OutputTemplateAssert contentContainsExactlyParamsInBlock(String blockName, List<String> xslNames) {
        contentContainsBlock(blockName);

        String contentBlock = getBlock(blockName, outputTemplate.getContent());
        Pattern generalPattern = Pattern.compile("\\{([A-Za-z0-9_-]+)#ifnz}"); // catch {color_glob#ifnz}
        List<String> contentXslNames = Pattern.compile("\n").splitAsStream(contentBlock)
            .map(line -> {
                Matcher matcher1 = generalPattern.matcher(line);
                return matcher1.find() ? matcher1.group(1) : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Assertions.assertThat(contentXslNames)
            .describedAs("Failed to find xslNames in block:\n" + contentBlock)
            .containsExactlyElementsOf(xslNames);
        return this;
    }

    public OutputTemplateAssert contentContainsExactlyParamsInBlockInRaw(String blockName, String... xslNames) {
        return contentContainsExactlyParamsInBlockInRaw(blockName, Arrays.asList(xslNames));
    }

    /**
     * Проверяет, что перечислены только конкретные параметры с учетом порядка в формате.
     */
    // <![CDATA[{string parse="";string out="";
    // if($color_glob) out=out+parse+"color_glob: " + $color_glob + "";
    // if(out) parse=", ";
    // if($Type) out=out+parse+"type: " + $Type;
    // if(out) parse=", ";
    // return out;#exec}
    // ]]>
    public OutputTemplateAssert contentContainsExactlyParamsInBlockInRaw(String blockName, List<String> xslNames) {
        contentContainsBlock(blockName);

        String contentBlock = getBlock(blockName, outputTemplate.getContent());
        Pattern technicalPattern = Pattern.compile("\\$([A-Za-z0-9_-]+)"); // catch $color_glob
        List<String> contentXslNames = Pattern.compile("\n").splitAsStream(contentBlock)
            .map(line -> {
                Matcher matcher1 = technicalPattern.matcher(line);
                return matcher1.find() ? matcher1.group(1) : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Assertions.assertThat(contentXslNames)
            .describedAs("Failed to find xslNames in block:\n" + contentBlock)
            .containsExactlyElementsOf(xslNames);
        return this;
    }

    public static OutputTemplateAssert assertThat(OutputTemplate outputTemplate) {
        return new OutputTemplateAssert(outputTemplate);
    }

    private String getBlock(String blockName, String text) {
        int startIndex = text.indexOf("<block name=\"" + blockName + "\">");
        int endIndex = text.indexOf("</block>", startIndex);
        return text.substring(startIndex, endIndex + "</block>".length());
    }

    private boolean containsBlock(String blockName, String text) {
        int startIndex = text.indexOf("<block name=\"" + blockName + "\">");
        return startIndex != -1;
    }
}
