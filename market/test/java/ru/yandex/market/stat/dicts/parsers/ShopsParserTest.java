package ru.yandex.market.stat.dicts.parsers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.stat.dicts.config.ParsersDictsConfig;
import ru.yandex.market.stat.dicts.records.ShopsDictionaryRecord;
import ru.yandex.market.stat.parsers.ParsersMetricsService;
import ru.yandex.market.stat.parsers.services.FieldParserHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

/**
 * @author Alexander Novikov <hronos@yandex-team.ru>
 */
@ContextConfiguration(classes = {ParsersDictsConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class ShopsParserTest {

    @Mock
    private BeanFactory beanFactory;

    @Mock
    private ParsersMetricsService metricsService;

    private MbiDatParser<ShopsDictionaryRecord> shopsParser;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        FieldParserHelper fieldParserHelper = new FieldParserHelper(metricsService, beanFactory);
        shopsParser = new MbiDatParser<>(fieldParserHelper, ShopsDictionaryRecord.class);
    }

    @Test
    public void load() throws IOException {
        // When
        List<ShopsDictionaryRecord> recordList = loadRecords(shopsParser, "/parsers/shops-utf8.dat");
        assertThat(recordList.size(), is(4));

        //template
//        assertThat(shopsDictionaryRecord.getShop_id(), is());
//        assertThat(shopsDictionaryRecord.getClient_id(), is());
//        assertThat(shopsDictionaryRecord.getCpa(), is());
//        assertThat(shopsDictionaryRecord.getDatasource_name(), is());
//        assertThat(shopsDictionaryRecord.getDelivery_services(), is());
//        assertThat(shopsDictionaryRecord.getDelivery_src(), is());
//        assertThat(shopsDictionaryRecord.getFree(), is());
//        assertThat(shopsDictionaryRecord.getFrom_market(), is());
//        assertThat(shopsDictionaryRecord.getHome_region(), is());
//        assertThat(shopsDictionaryRecord.getIs_booknow(), is());
//        assertThat(shopsDictionaryRecord.getIs_cpa_partner(), is());
//        assertThat(shopsDictionaryRecord.getIs_cpa_prior(), is());
//        assertThat(shopsDictionaryRecord.getIs_discounts_enabled(), is());
//        assertThat(shopsDictionaryRecord.getIs_enabled(), is());
//        assertThat(shopsDictionaryRecord.getIs_global(), is());
//        assertThat(shopsDictionaryRecord.getIs_mock(), is());
//        assertThat(shopsDictionaryRecord.getLocal_delivery_cost(), is());
//        assertThat(shopsDictionaryRecord.getPhone(), is());
//        assertThat(shopsDictionaryRecord.getPhone_display_options(), is());
//        assertThat(shopsDictionaryRecord.getPrepay_enabled(), is());
//        assertThat(shopsDictionaryRecord.getPriority_region_original(), is());
//        assertThat(shopsDictionaryRecord.getPriority_regions(), is());
//        assertThat(shopsDictionaryRecord.getShop_cluster_id(), is());
//        assertThat(shopsDictionaryRecord.getShop_currency(), is());
//        assertThat(shopsDictionaryRecord.getShopname(), is());
//        assertThat(shopsDictionaryRecord.getShow_premium(), is());
//        assertThat(shopsDictionaryRecord.getTariff(), is());
//        assertThat(shopsDictionaryRecord.getIs_online(), is());
//        assertThat(shopsDictionaryRecord.getYclid_disabled(), is());

        //disabled shop
        ShopsDictionaryRecord shopsDictionaryRecord = recordList.get(0);
        assertThat(shopsDictionaryRecord.getShop_id(), is(999999999));
        assertThat(shopsDictionaryRecord.getClient_id(), is(nullValue()));
        assertThat(shopsDictionaryRecord.getCpa(), is("NO"));
        assertThat(shopsDictionaryRecord.getDatasource_name(), is("yandexdescriptions"));
        assertThat(shopsDictionaryRecord.getDelivery_services(), is(""));
        assertThat(shopsDictionaryRecord.getDelivery_src(), is("WEB"));
        assertThat(shopsDictionaryRecord.getIs_discounts_enabled(), is(true));
        assertThat(shopsDictionaryRecord.getIs_enabled(), is(false));
        assertThat(shopsDictionaryRecord.getIs_global(), is(false));
        assertThat(shopsDictionaryRecord.getIs_online(), is(true));
        assertThat(shopsDictionaryRecord.getPhone_display_options(), is("*"));
        assertThat(shopsDictionaryRecord.getShop_currency(), is("RUR"));
        assertThat(shopsDictionaryRecord.getTariff(), is("CLICKS"));
        assertThat(shopsDictionaryRecord.getShow_premium(), is(true));
        assertThat(shopsDictionaryRecord.getShopname(), is("yandexdescriptions"));
        assertThat(shopsDictionaryRecord.getRegions(), is(nullValue()));
        assertThat(shopsDictionaryRecord.getReturn_delivery_address(), is(false));
        assertThat(shopsDictionaryRecord.getIs_placed(), is(false));
        assertThat(shopsDictionaryRecord.getCpc(), is("SBX"));
        assertThat(shopsDictionaryRecord.getIs_cpa20(), is(false));
        assertThat(shopsDictionaryRecord.getSupplier_type(), is(3));

        //enabled shop
        shopsDictionaryRecord = recordList.get(1);
        assertThat(shopsDictionaryRecord.getShop_id(), is(148));
        assertThat(shopsDictionaryRecord.getClient_id(), is(49850));
        assertThat(shopsDictionaryRecord.getCpa(), is("REAL"));
        assertThat(shopsDictionaryRecord.getDatasource_name(), is("www.bestwatch.ru"));
        assertThat(shopsDictionaryRecord.getDelivery_services(), is("8=0;99=1;3=1;1=1;2=1;"));
        assertThat(shopsDictionaryRecord.getDelivery_src(), is("YML"));
        assertThat(shopsDictionaryRecord.getFree(), is(false));
        assertThat(shopsDictionaryRecord.getFrom_market(), is(true));
        assertThat(shopsDictionaryRecord.getHome_region(), is(225));
        assertThat(shopsDictionaryRecord.getIs_booknow(), is(false));
        assertThat(shopsDictionaryRecord.getIs_cpa_partner(), is(false));
        assertThat(shopsDictionaryRecord.getIs_cpa_prior(), is(true));
        assertThat(shopsDictionaryRecord.getIs_discounts_enabled(), is(true));
        assertThat(shopsDictionaryRecord.getIs_enabled(), is(true));
        assertThat(shopsDictionaryRecord.getIs_global(), is(false));
        assertThat(shopsDictionaryRecord.getIs_mock(), is(false));
        assertThat(shopsDictionaryRecord.getLocal_delivery_cost(), is(new BigDecimal(18000)));
        assertThat(shopsDictionaryRecord.getPhone(), is("7 (495) 589-27-07"));
        assertThat(shopsDictionaryRecord.getPhone_display_options(), is("*"));
        assertThat(shopsDictionaryRecord.getPrepay_enabled(), is(true));
        assertThat(shopsDictionaryRecord.getPriority_region_original(), is(213));
        assertThat(shopsDictionaryRecord.getPriority_regions(), is(213));
        assertThat(shopsDictionaryRecord.getShop_cluster_id(), is(new BigDecimal(15802297)));
        assertThat(shopsDictionaryRecord.getShop_currency(), is("RUR"));
        assertThat(shopsDictionaryRecord.getShopname(), is("Bestwatch"));
        assertThat(shopsDictionaryRecord.getShow_premium(), is(true));
        assertThat(shopsDictionaryRecord.getTariff(), is("CLICKS"));
        assertThat(shopsDictionaryRecord.getIs_online(), is(true));
        assertThat(shopsDictionaryRecord.getYclid_disabled(), is(false));
        assertThat(shopsDictionaryRecord.getRegions(), containsInAnyOrder(
            11266, 120835, 10243, 120834, 118793, 119817, 10251, 120845, 11276, 120844, 101388, 11277, 120847, 120846, 11280, 11286, 119833, 120859, 116765, 120860, 11294, 118814, 11295, 116766, 118817, 11297, 118819, 118818, 118821, 118820, 119844, 117805, 11309, 117811, 117813, 117812, 117815, 11318, 117814, 117817, 118841, 117816, 117819, 117818, 118851, 11330, 99395, 118850, 99394, 118853, 99397, 118852, 99396, 118855, 99399, 118854, 99398, 118857, 99401, 118856, 101448, 99400, 118859, 99403, 118858, 75, 99402, 118861, 11340, 99405, 118860, 118863, 99407, 118862, 99406, 118865, 118864, 118867, 118866, 11347, 118869, 11348, 118868, 11349, 118871, 11350, 118870, 119894, 11351, 119897, 118872, 20586, 100459, 11375, 114806, 24696, 24697, 24698, 24699, 118909, 24700, 117884, 24701, 20606, 24702, 118910, 24703, 24704, 24705, 24706, 102530, 24707, 24708, 24709, 119943, 11398, 20617, 118923, 118922, 11403, 119949, 118931, 11411, 120983, 120982, 120985, 20632, 120984, 117912, 120987, 11418, 120986, 120989, 11420, 116893, 120988, 120991, 120990, 20639, 11423, 120993, 120992, 116896, 120995, 11426, 100514, 117922, 120997, 118949, 120996, 100516, 120999, 101543, 120998, 121001, 21672, 121000, 21673, 121003, 21674, 121002, 130218, 21675, 121005, 21676, 121004, 21677, 121007, 119982, 121009, 20656, 121008, 20658, 101554, 11443, 117945, 11450, 11451, 20668, 118972, 11453, 20673, 120000, 117956, 101575, 20680, 21709, 20689, 120019, 213, 119001, 121059, 121061, 121060, 121063, 121062, 121064, 20714, 20715, 117994, 121066, 101613, 20717, 121068, 20718, 121071, 119025, 121073, 119024, 121072, 119027, 119026, 121074, 119029, 119028, 119031, 119030, 119033, 119032, 119035, 119034, 119037, 118013, 119036, 119039, 119038, 119040, 120064, 37125, 37128, 98568, 101645, 37136, 98581, 98582, 20759, 98585, 120089, 98584, 98587, 98586, 98589, 98588, 98591, 37150, 101663, 98590, 98593, 98592, 98595, 98594, 98597, 98596, 98598, 98601, 98603, 98602, 98605, 98604, 37165, 98607, 37166, 98606, 98609, 98608, 118064, 98611, 98610, 37171, 98612, 37173, 98615, 98614, 98617, 98616, 150840, 118082, 118091, 118090, 101713, 120149, 120153, 117080, 117097, 101736, 115048, 117101, 117103, 117102, 119153, 120176, 117109, 121215, 121214, 101765, 121221, 121220, 121223, 121222, 120201, 121224, 121226, 118159, 10645, 101785, 101784, 10650, 101786, 117151, 117152, 10658, 119202, 120242, 118203, 121275, 120250, 21949, 10687, 21951, 10693, 120263, 120267, 10699, 120268, 10705, 120279, 10712, 118246, 117222, 119272, 118255, 101870, 119281, 120309, 100855, 10743, 100854, 100856, 119303, 119305, 119304, 119306, 117263, 10772, 10776, 119325, 118300, 120352, 118308, 113192, 10795, 20012, 20013, 117295, 20014, 117294, 20015, 117297, 20016, 117296, 20017, 10802, 117299, 20018, 117298, 100914, 117300, 119351, 113206, 119353, 119352, 119355, 117307, 119354, 117306, 119356, 10819, 120391, 120393, 117324, 120399, 100948, 10841, 10842, 117347, 10853, 119396, 118375, 10857, 119402, 118381, 119413, 119412, 100980, 119415, 119414, 119416, 118411, 118410, 10897, 10904, 119455, 119454, 119456, 118432, 10926, 119470, 120497, 101041, 101043, 101042, 10933, 10939, 118465, 10946, 101058, 20165, 10950, 20166, 120521, 110283, 118483, 117458, 29396, 110301, 118493, 110303, 110302, 99041, 110304, 99043, 99042, 99044, 99047, 99046, 119526, 99049, 99048, 99051, 119531, 99050, 99053, 119533, 99052, 119532, 99057, 99056, 99059, 99058, 99061, 99060, 99063, 115447, 99062, 135931, 11004, 135934, 11010, 11012, 11013, 11015, 117513, 11020, 135951, 11021, 118542, 118557, 121632, 20259, 135972, 118569, 101163, 100139, 101162, 100138, 101165, 101164, 101167, 101166, 101169, 101168, 101171, 101170, 101173, 101172, 101175, 101174, 101177, 101176, 101179, 101178, 101181, 101180, 11069, 11070, 101183, 119615, 101182, 101185, 101184, 101187, 101186, 101189, 11077, 101188, 101191, 101190, 101193, 101192, 101195, 101194, 11084, 101197, 101196, 101199, 101198, 118609, 101200, 11095, 11108, 11112, 11117, 11120, 11121, 11122, 11123, 11124, 11125, 11126, 11127, 11128, 11129, 136059, 117636, 11146, 11148, 11153, 11156, 11158, 11176, 118701, 118703, 118702, 118709, 118708, 118710, 11193, 11194, 11195, 11196, 11197, 11198, 118719, 11199, 959, 10176, 101313, 11201, 118723, 11203, 11204, 118725, 101317, 11205, 118724, 11206, 11207, 101318, 11209, 11210, 11212, 11213, 118735, 11214, 974, 11215, 11217, 11219, 11221, 29653, 11222, 11226, 101343, 118750, 101342, 116705, 101345, 11232, 116704, 101344, 101347, 101346, 119778, 11235, 101349, 116708, 101348, 101351, 119782, 101350, 101353, 101352, 101355, 101354, 101357, 101356, 119791, 101359, 101358, 101361, 101360, 114679, 10231, 10233
        ));
        assertThat(shopsDictionaryRecord.getReturn_delivery_address(), is(true));
        assertThat(shopsDictionaryRecord.getIs_placed(), is(true));
        assertThat(shopsDictionaryRecord.getCpc(), is("NO"));
        assertThat(shopsDictionaryRecord.getIs_cpa20(), is(true));
        assertThat(shopsDictionaryRecord.getSupplier_type(), is(3));

        shopsDictionaryRecord = recordList.get(3);
        assertThat(shopsDictionaryRecord.getSupplier_type(), is(4));
        assertThat(shopsDictionaryRecord.getEnable_auto_discounts(), is(true));

    }

    @Test
    public void loadFromProdWithSandboxData() throws Exception {
        // When
        List<ShopsDictionaryRecord> records = loadRecords(shopsParser, "sandbox:shops-utf8-prod.dat.gz");

        // Then
        assertThat(records.size(), is(149388));
    }

    @Test
    public void loadFromTestingWithSandboxData() throws Exception {
        // When
        List<ShopsDictionaryRecord> records = loadRecords(shopsParser, "sandbox:shops-utf8-testing.dat.gz");

        // Then
        assertThat(records.size(), is(149750));
    }
}
