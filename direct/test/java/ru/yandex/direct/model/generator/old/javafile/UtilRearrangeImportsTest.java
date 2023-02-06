package ru.yandex.direct.model.generator.old.javafile;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilRearrangeImportsTest {
    public static final String TEST = "// Auto-generated by model-generator, source: adfox_deal.conf\n" +
            "package ru.yandex.direct.core.entity.deal.model;\n" +
            "\n" +
            "import com.google.common.collect.ImmutableSet;\n" +
            "import java.math.BigDecimal;\n" +
            "import java.time.LocalDateTime;\n" +
            "import java.util.Objects;\n" +
            "\n" +
            "import static org.hamcrest.Matchers.empty;\n" +
            "import static org.hamcrest.Matchers.hasSize;\n" +
            "import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;\n" +
            "import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;\n" +
            "import java.util.List;\n" +
            "import java.util.Set;\n" +
            "import javax.annotation.Generated;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxTargetingsTextPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAdfoxDescriptionPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAgencyFeePercentPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAgencyFeeTypePropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxClientIdPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAdfoxStatusPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxContactsPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxCpmPropHolder;\n" +
            "    \t \n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxPublisherNamePropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAdfoxNamePropHolder;\n" +
            "import ru.yandex.direct.currency.CurrencyCode;\n" +
            "import ru.yandex.direct.currency.Percent;\n" +
            "import ru.yandex.direct.model.ModelProperty;\n" +
            "\n" +
            "/**\n" +
            " *\n" +
            " *   Сделка как она приходит из Adfox\n" +
            " *    */\n" +
            "@Generated(\n" +
            "        value = \"ru.yandex.direct.model.generator.Tool\",\n" +
            "        comments = \"generated from adfox_deal.conf\"\n" +
            ")\n" +
            "public class DealAdfox implements DealSimple {\n" +
            "    private Long id;\n" +
            "\n" +
            "    private String publisherName;\n" +
            "\n" +
            "    private String adfoxName;\n" +
            "\n" +
            "    private String adfoxDescription;\n" +
            "\n" +
            "    private Long clientId;";

    public static final String EXPECTED = "// Auto-generated by model-generator, source: adfox_deal.conf\n" +
            "package ru.yandex.direct.core.entity.deal.model;\n" +
            "\n" +
            "import java.math.BigDecimal;\n" +
            "import java.time.LocalDateTime;\n" +
            "import java.util.List;\n" +
            "import java.util.Objects;\n" +
            "import java.util.Set;\n" +
            "\n" +
            "import javax.annotation.Generated;\n" +
            "\n" +
            "import com.google.common.collect.ImmutableSet;\n" +
            "\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAdfoxDescriptionPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAdfoxNamePropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAdfoxStatusPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAgencyFeePercentPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxAgencyFeeTypePropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxClientIdPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxContactsPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxCpmPropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxPublisherNamePropHolder;\n" +
            "import ru.yandex.direct.core.entity.deal.modelprop.DealAdfoxTargetingsTextPropHolder;\n" +
            "import ru.yandex.direct.currency.CurrencyCode;\n" +
            "import ru.yandex.direct.currency.Percent;\n" +
            "import ru.yandex.direct.model.ModelProperty;\n" +
            "\n" +
            "import static org.hamcrest.Matchers.empty;\n" +
            "import static org.hamcrest.Matchers.hasSize;\n" +
            "import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;\n" +
            "import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;\n" +
            "\n" +
            "/**\n" +
            " *\n" +
            " *   Сделка как она приходит из Adfox\n" +
            " *    */\n" +
            "@Generated(\n" +
            "        value = \"ru.yandex.direct.model.generator.Tool\",\n" +
            "        comments = \"generated from adfox_deal.conf\"\n" +
            ")\n" +
            "public class DealAdfox implements DealSimple {\n" +
            "    private Long id;\n" +
            "\n" +
            "    private String publisherName;\n" +
            "\n" +
            "    private String adfoxName;\n" +
            "\n" +
            "    private String adfoxDescription;\n" +
            "\n" +
            "    private Long clientId;";

    @Test
    public void rearrangeImports() {
        assertThat(Util.rearrangeImports(TEST)).isEqualTo(EXPECTED);
    }
}