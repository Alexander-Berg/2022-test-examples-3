package ru.yandex.market.mbo.conflictrule;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.xml.XmlWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * @author ayratgdl
 * @date 07.11.17
 */
public class ConflictRuleFormalizerXmlTest {
    private static final Long CATEGORY_1 = 101L;
    private static final Long PARAMETER_1 = 201L;
    private static final Long PARAMETER_2 = 202L;
    private static final Long PARAMETER_3 = 203L;
    private static final Long OPTION_1_1 = 311L;
    private static final Long OPTION_1_2 = 312L;
    private static final Long OPTION_2_1 = 321L;
    private static final Long OPTION_2_2 = 322L;
    private static final Long OPTION_3_1 = 331L;
    private static final Long OPTION_3_2 = 332L;


    @Test
    public void oneConflictRuleToXml() throws IOException {
        ConflictRule conflictRule = new ConflictRule()
            .setCategoryId(CATEGORY_1)
            .setFirstParamId(PARAMETER_1)
            .setSecondParamId(PARAMETER_2)
            .setScope(ConflictRule.OfferScope.ALL_OFFER)
            .setEnumerationType(ConflictRule.EnumerationType.ALL_EXCEPT_LISTED)
            .addPair(OPTION_1_1, OPTION_2_1)
            .addPair(OPTION_1_2, OPTION_2_2);

        StringWriter actualWriter = new StringWriter();
        ConflictRuleFormalizerXml.toXml(conflictRule, new XmlWriter(actualWriter));
        String actualXml = actualWriter.toString();

        String expectedXml = "<conflict-rule type=\"param-conflict-resolving\">\n" +
            " <masterParamId>" + PARAMETER_1 + "</masterParamId>\n" +
            " <slaveParamId>" + PARAMETER_2 + "</slaveParamId>\n" +
            " <intersection>false</intersection>\n" +
            " <excluded-values>\n" +
            "  <value masterValueId=\"" + OPTION_1_1 + "\" slaveValueId=\"" + OPTION_2_1 + "\"/>\n" +
            "  <value masterValueId=\"" + OPTION_1_2 + "\" slaveValueId=\"" + OPTION_2_2 + "\"/>\n" +
            " </excluded-values>\n" +
            "</conflict-rule>\n";

        Assert.assertEquals(expectedXml, actualXml);
    }

    @Test
    public void severalConflictRulesToXml() throws IOException {
        List<ConflictRule> conflictRules = Arrays.asList(
            new ConflictRule()
                .setCategoryId(CATEGORY_1)
                .setFirstParamId(PARAMETER_1)
                .setSecondParamId(PARAMETER_2)
                .setScope(ConflictRule.OfferScope.ALL_OFFER)
                .setEnumerationType(ConflictRule.EnumerationType.ALL_EXCEPT_LISTED)
                .addPair(OPTION_1_1, OPTION_2_1)
                .addPair(OPTION_1_2, OPTION_2_2),
            new ConflictRule()
                .setCategoryId(CATEGORY_1)
                .setFirstParamId(PARAMETER_1)
                .setSecondParamId(PARAMETER_3)
                .setScope(ConflictRule.OfferScope.EQUAL_PARTS)
                .setEnumerationType(ConflictRule.EnumerationType.LISTED)
                .addPair(OPTION_1_1, OPTION_3_1)
                .addPair(OPTION_1_2, OPTION_3_2),
            new ConflictRule()
                .setCategoryId(CATEGORY_1)
                .setFirstParamId(PARAMETER_2)
                .setSecondParamId(PARAMETER_3)
                .setScope(ConflictRule.OfferScope.ALL_OFFER)
                .setEnumerationType(ConflictRule.EnumerationType.ALL_EXCEPT_LISTED)
        );

        StringWriter actualWriter = new StringWriter();
        ConflictRuleFormalizerXml.toXml(conflictRules, new XmlWriter(actualWriter));
        String actualXml = actualWriter.toString();

        String expectedXml = "<conflict-rules>\n" +
            " <conflict-rule type=\"param-conflict-resolving\">\n" +
            "  <masterParamId>" + PARAMETER_1 + "</masterParamId>\n" +
            "  <slaveParamId>" + PARAMETER_2 + "</slaveParamId>\n" +
            "  <intersection>false</intersection>\n" +
            "  <excluded-values>\n" +
            "   <value masterValueId=\"" + OPTION_1_1 + "\" slaveValueId=\"" + OPTION_2_1 + "\"/>\n" +
            "   <value masterValueId=\"" + OPTION_1_2 + "\" slaveValueId=\"" + OPTION_2_2 + "\"/>\n" +
            "  </excluded-values>\n" +
            " </conflict-rule>\n" +
            " <conflict-rule type=\"param-conflict-resolving\">\n" +
            "  <masterParamId>" + PARAMETER_1 + "</masterParamId>\n" +
            "  <slaveParamId>" + PARAMETER_3 + "</slaveParamId>\n" +
            "  <intersection>true</intersection>\n" +
            "  <included-values>\n" +
            "   <value masterValueId=\"" + OPTION_1_1 + "\" slaveValueId=\"" + OPTION_3_1 + "\"/>\n" +
            "   <value masterValueId=\"" + OPTION_1_2 + "\" slaveValueId=\"" + OPTION_3_2 + "\"/>\n" +
            "  </included-values>\n" +
            " </conflict-rule>\n" +
            "</conflict-rules>\n";

        Assert.assertEquals(expectedXml, actualXml);
    }

}
