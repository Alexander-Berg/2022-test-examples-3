package ru.yandex.market.billing.tasks;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import NMarket.NMbi.NCreditsInfo.ECreditTemplateOrganizationType;
import NMarket.NMbi.NCreditsInfo.TCreditTemplate;
import NMarket.NMbi.NCreditsInfo.TCreditsInfo;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.credit.CreditTemplateType;
import ru.yandex.market.core.credit.CreditTemplateValidator;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link CreditTemplatesExportExecutor}.
 *
 * @author serenitas
 */
public class CreditTemplatesExportExecutorTest extends FunctionalTest {

    @Autowired
    private CreditTemplatesExportExecutor creditTemplatesExportExecutor;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private NamedHistoryMdsS3Client namedHistoryMdsS3Client;

    @BeforeEach
    void init() {
        environmentService.addValue("credits.min.price.limit", "3600");
    }

    @Test
    @DisplayName("Проверка кредитных шаблонов с OrganizationType = Bank")
    @DbUnitDataSet(before = "CreditTemplatesExportExecutor.before.csv")
    void testMultipleOrganizationType() {
        checkUploadedTemplates(this::checkCreditsInfo);
    }

    @Test
    @DisplayName("Проверка кредитного шаблона с OrganizationType = Microcredit")
    @DbUnitDataSet(before = "CreditTemplatesExportExecutor.microcredit.before.csv")
    void testMicrocreditTemplate() {
        checkUploadedTemplates(creditsInfo -> {
            assertThat(creditsInfo.CreditTemplatesLength(), is(1));

            TCreditTemplate template = creditsInfo.CreditTemplates(0);
            Assertions.assertEquals(4000, template.Id());
            Assertions.assertEquals(1, template.ShopId());
            Assertions.assertEquals(1, template.ShopTemplateId());
            Assertions.assertEquals("", template.BankTitle());
            Assertions.assertEquals(12, template.Period());
            Assertions.assertEquals(1240000, template.Rate());
            Assertions.assertEquals(5000, template.MinOfferPrice());
            Assertions.assertEquals(99999, template.MaxOfferPrice());
            Assertions.assertEquals("https://conditions.uk/mfo", template.TermsUrl());
            Assertions.assertEquals((int) CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE.getId(), template.Type());
            Assertions.assertEquals(ECreditTemplateOrganizationType.Microcredit, template.OrganizationType());
        });
    }

    @Test
    @DisplayName("Проверка кредитного шаблона с OrganizationType = YandexKassa")
    @DbUnitDataSet(before = "CreditTemplatesExportExecutor.yandexkassa.before.csv")
    void testYandexKassaTemplate() {
        checkUploadedTemplates(creditsInfo -> {
            assertThat(creditsInfo.CreditTemplatesLength(), is(1));

            TCreditTemplate template = creditsInfo.CreditTemplates(0);
            Assertions.assertEquals(4000, template.Id());
            Assertions.assertEquals(ECreditTemplateOrganizationType.YandexKassa, template.OrganizationType());
        });
    }

    @Test
    @DisplayName("Проверка кредитного шаблона с OrganizationType, которого нет в enum'е")
    @DbUnitDataSet(before = "CreditTemplatesExportExecutor.wrongOrganizationType.before.csv")
    void testWrongOrganizationType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> creditTemplatesExportExecutor.doJob(null));
    }

    private void checkUploadedTemplates(Consumer<TCreditsInfo> creditsInfoConsumer) {
        when(namedHistoryMdsS3Client.upload(any(String.class), any(ContentProvider.class)))
                .then(invocation -> {
                    ContentProvider contentProvider = invocation.getArgument(1);
                    InputStream inputStream = contentProvider.getInputStream();
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                    creditsInfoConsumer.accept(TCreditsInfo.getRootAsTCreditsInfo(byteBuffer));
                    return null;
                });
        creditTemplatesExportExecutor.doJob(null);
    }

    private void checkCreditsInfo(TCreditsInfo creditsInfo) {
        long actualLimit = creditsInfo.MinOfferPrice();
        long expectedLimit = environmentService.getIntValue(CreditTemplateValidator.CREDITS_MIN_PRICE_LIMIT, -1);
        Assertions.assertEquals(expectedLimit, actualLimit);

        int templatesLength = creditsInfo.CreditTemplatesLength();
        Assertions.assertEquals(3, templatesLength);

        TCreditTemplate template = creditsInfo.CreditTemplates(0);
        Assertions.assertEquals(1000, template.Id());
        Assertions.assertEquals(1, template.ShopId());
        Assertions.assertEquals(1, template.ShopTemplateId());
        Assertions.assertEquals("Сберегательная банка", template.BankTitle());
        Assertions.assertEquals(12, template.Period());
        Assertions.assertEquals(1240000, template.Rate());
        Assertions.assertEquals(5000, template.MinOfferPrice());
        Assertions.assertEquals(99999, template.MaxOfferPrice());
        Assertions.assertEquals("http://conditions.uk", template.TermsUrl());
        Assertions.assertEquals((int) CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE.getId(), template.Type());
        Assertions.assertEquals(ECreditTemplateOrganizationType.Bank, template.OrganizationType());
    }

}
