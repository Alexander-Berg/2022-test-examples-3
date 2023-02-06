package manual.ru.yandex.market.psku.postprocessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.PartnerContentServiceStub;
import ru.yandex.market.ir.http.PartnerContentUi.ListGcSkuTicketResponse;
import ru.yandex.market.psku.postprocessor.bazinga.deduplication.CreatePskuForDSBSOfferWatcher.PartnerContentServiceWrapper;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;

import static manual.ru.yandex.market.psku.postprocessor.PartnerContentServiceTest.BusinessIdShopSkuPair.pair;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class PartnerContentServiceTest extends BaseDBTest {

    private static final Boolean USE_PROD = false;
    private static final Long BUSINESS_ID = 10919608L;
    private static final String SHOP_SKU = "olyaklu08112122";

    // File should have format of BUSINESS_ID, SHOP_SKU and have END at the end of file
    private static final String FILE_PATH = "/home/a-murashko/Desktop/offers_to_create.txt";

    private static final String AG_API_HOST_PROD = "http://autogen-api.vs.market.yandex.net:34540/v2/";
    private static final String AG_API_HOST_TEST = "http://autogen-api.tst.vs.market.yandex.net:34540/v2/";

    @Autowired
    ClusterMetaDao clusterMetaDao;

    @Autowired
    ClusterContentDao clusterContentDao;

    PartnerContentServiceStub partnerContentService;
    PartnerContentServiceWrapper partnerContentServiceWrapper;

    @Before
    public void setUp() throws Exception {
        partnerContentService = new PartnerContentServiceStub();
        partnerContentService.setHost(USE_PROD ? AG_API_HOST_PROD : AG_API_HOST_TEST);
        partnerContentServiceWrapper = new PartnerContentServiceWrapper(partnerContentService);
    }

    @Test
    public void testPartnerContentServiceWorks() {
        ClusterContent clusterContent = new ClusterContent();
        clusterContent.setBusinessId(BUSINESS_ID);
        clusterContent.setOfferId(SHOP_SKU);
        ListGcSkuTicketResponse listGcSkuTicketResponse =
                partnerContentServiceWrapper.listGcSkuTickets(clusterContent);

        assertThat(listGcSkuTicketResponse.getDataCount()).isEqualTo(1);
        System.out.println(listGcSkuTicketResponse);
    }

    @Test
    public void testPartnerContentServiceWorksOnBigAmount() throws FileNotFoundException {
        List<BusinessIdShopSkuPair> pairs = getPairsFromFile();

        Map<BusinessIdShopSkuPair, ListGcSkuTicketResponse.Status> pairToStatus = new HashMap<>();
        Map<ListGcSkuTicketResponse.Status, Integer> statusToCount = new HashMap<>();
        List<BusinessIdShopSkuPair> notFoundPairs = new ArrayList<>();
        List<BusinessIdShopSkuPair> pairsWithoutStatus = new ArrayList<>();

        pairs.forEach(pair -> {
            ClusterContent clusterContent = new ClusterContent();
            clusterContent.setBusinessId(pair.getBusinessId());
            clusterContent.setOfferId(pair.getShopSku());
            ListGcSkuTicketResponse listGcSkuTicketResponse =
                    partnerContentServiceWrapper.listGcSkuTickets(clusterContent);

            if (listGcSkuTicketResponse.getDataCount() > 0) {
                ListGcSkuTicketResponse.Row data = listGcSkuTicketResponse.getData(0);
                if (data.hasStatus()) {
                    ListGcSkuTicketResponse.Status status = data.getStatus();
                    pairToStatus.put(pair, status);
                    statusToCount.computeIfAbsent(status, s -> 1);
                    statusToCount.computeIfPresent(status, (s, v) -> v + 1);
                } else {
                    pairsWithoutStatus.add(pair);
                }
            } else {
                notFoundPairs.add(pair);
            }

        });

        System.out.println(pairToStatus);
        System.out.println(statusToCount);
        System.out.println(notFoundPairs);
        System.out.println(pairsWithoutStatus);
    }

    public List<BusinessIdShopSkuPair> getPairsFromFile() throws FileNotFoundException {
        List<BusinessIdShopSkuPair> pairs = new ArrayList<>();
        File inputFile = new File(FILE_PATH);
        Scanner sc = new Scanner(inputFile);
        while (sc.hasNextLine()) {
            String s = sc.nextLine();
            if (!s.isEmpty()) {
                String[] split = s.split(",");
                pairs.add(pair(Long.parseLong(split[0].trim()), split[1].trim()));
            }
        }
        return pairs;
    }

    public static class BusinessIdShopSkuPair {
        private final Long businessId;
        private final String shopSku;

        public BusinessIdShopSkuPair(Long businessId, String shopSku) {
            this.businessId = businessId;
            this.shopSku = shopSku;
        }

        public static BusinessIdShopSkuPair pair(Long businessId, String shopSku) {
            return new BusinessIdShopSkuPair(businessId, shopSku);
        }

        public Long getBusinessId() {
            return businessId;
        }

        public String getShopSku() {
            return shopSku;
        }
    }
}