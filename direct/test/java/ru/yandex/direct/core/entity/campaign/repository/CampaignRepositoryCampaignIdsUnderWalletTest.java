package ru.yandex.direct.core.entity.campaign.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignRepositoryCampaignIdsUnderWalletTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    private int shard;
    private long walletCid;
    private List<Long> childCampIds = new ArrayList<>();
    private CampaignInfo walletCampInfo;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        walletCampInfo =
                steps.campaignSteps().createCampaign(TestCampaigns.activeWalletCampaign(null, null));
        shard = walletCampInfo.getShard();
        walletCid = walletCampInfo.getCampaignId();

    }

    public void createChildCampaigns(int numOfCampaigns) {
        for (int i = 0; i < numOfCampaigns; i++) {
            Campaign testCampaign = TestCampaigns.activeCampaignByCampaignType(campaignType,
                    walletCampInfo.getClientId(),
                    walletCampInfo.getUid());

            testCampaign.getBalanceInfo().setWalletCid(walletCid);
            CampaignInfo childCampInfo =
                    steps.campaignSteps().createCampaign(testCampaign, walletCampInfo.getClientInfo());

            childCampIds.add(childCampInfo.getCampaignId());
        }

        List<Long> campsToCheck = new ArrayList<>();
        campsToCheck.add(walletCid);
        campsToCheck.addAll(childCampIds);
        checkState(campaignRepository.getCampaigns(shard, campsToCheck).size() == (numOfCampaigns + 1),
                "проверяем, что в базе сохранился кошелек + дочерние кампании");

    }

    @Test
    public void checkGetCampaignIdsUnderWalletWithNoCampaigns() {

        List<Long> actualChildCampIds = campaignRepository.getCampaignIdsUnderWallet(shard, walletCid);
        actualChildCampIds.sort(Comparator.naturalOrder());
        childCampIds.sort(Comparator.naturalOrder());

        assertThat("проверяем, что список созданных и полученных методом id-дочерних кампаний совпадают: ",
                actualChildCampIds, beanDiffer(childCampIds));
    }

    @Test
    public void checkGetCampaignIdsUnderWalletForOneChild() {
        createChildCampaigns(1);

        List<Long> actualChildCampIds = campaignRepository.getCampaignIdsUnderWallet(shard, walletCid);
        actualChildCampIds.sort(Comparator.naturalOrder());
        childCampIds.sort(Comparator.naturalOrder());

        assertThat("проверяем, что список созданных и полученных методом id-дочерних кампаний совпадают: ",
                actualChildCampIds, beanDiffer(childCampIds));
    }

    @Test
    public void checkGetCampaignIdsUnderWalletForTwoChild() {
        createChildCampaigns(2);

        List<Long> actualChildCampIds = campaignRepository.getCampaignIdsUnderWallet(shard, walletCid);
        actualChildCampIds.sort(Comparator.naturalOrder());
        childCampIds.sort(Comparator.naturalOrder());

        assertThat("проверяем, что список созданных и полученных методом id-дочерних кампаний совпадают: ",
                actualChildCampIds, beanDiffer(childCampIds));
    }
}
