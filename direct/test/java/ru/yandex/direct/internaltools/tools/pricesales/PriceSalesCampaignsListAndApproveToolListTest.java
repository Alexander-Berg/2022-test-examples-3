package ru.yandex.direct.internaltools.tools.pricesales;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestClientNdsRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.pricesales.container.PriceSalesCampaignInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;

@InternalToolsTest
@RunWith(SpringRunner.class)
public class PriceSalesCampaignsListAndApproveToolListTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestClientNdsRepository testClientNdsRepository;

    @Autowired
    private PriceSalesCampaignsListAndApproveTool tool;

    @BeforeClass
    public static void setLocale() {
        LocaleContextHolder.setLocale(Locale.TRADITIONAL_CHINESE);
        Locale.setDefault(new Locale("en", "US"));
    }

    @Test
    public void test() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        CurrencyCode currency = client.getClient().getWorkCurrency();
        testClientNdsRepository.updateClientNds(client.getShard(), client.getClientId().asLong(),
                Percent.fromPercent(BigDecimal.valueOf(18)));
        ClientInfo agency = steps.clientSteps().createDefaultClientAnotherShard();
        PricePackage pricePackage = steps.pricePackageSteps().createApprovedPricePackageWithClients(client)
                .getPricePackage();
        CampaignInfo walletCampaign = steps.campaignSteps().createCampaign(
                activeWalletCampaign(client.getClientId(), client.getUid())
                        .withBalanceInfo(activeBalanceInfo(currency)
                                .withSum(new BigDecimal(3000))
                                .withSumSpent(new BigDecimal(100))));
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client,
                defaultCpmPriceCampaignWithSystemFields(client, pricePackage)
                        .withImpressionRateCount(186)
                        .withAgencyId(agency.getClientId().asLong())
                        .withAgencyUid(agency.getUid())
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                        .withWalletId(walletCampaign.getCampaignId()));
        steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, client);

        List<PriceSalesCampaignInfo> campaignsInfoFromTool = tool.getMassData();
        PriceSalesCampaignInfo expectedCampaignInfo = new PriceSalesCampaignInfo()
                .withCampaignId(campaign.getId())
                .withStartDate(campaign.getStartDate())
                .withEndDate(campaign.getEndDate())
                .withOrderVolume(campaign.getFlightOrderVolume())
                .withPricePackageTitle(pricePackage.getTitle() + " (" + pricePackage.getId() + ")")
                .withClientId(client.getClientId().asLong())
                .withClientLogin(client.getLogin())
                .withAgencyClientId(agency.getClientId().asLong())
                .withAgencyLogin(agency.getLogin())
                // walletMoney = (wallet.sum - wallet.sumSpent) / (1 + nds)
                .withWalletMoney("2457.62 " + currency)
                .withMinVolume(1L)
                .withTargeting("")
                .withFrequencyLimit(186)
                .withImpressionRateIntervalDays(0)
                .withInventoriColor(InventoriColor.RED.getText())
                .withGeo("Северо-Запад, Центр, Урал, Юг, Северный Кавказ");
        assertThat(campaignsInfoFromTool).contains(expectedCampaignInfo);
    }
}
