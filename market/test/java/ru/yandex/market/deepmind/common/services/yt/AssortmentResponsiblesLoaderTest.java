//package ru.yandex.market.deepmind.common.services.yt;
//
//import java.util.List;
//
//import javax.annotation.Resource;
//
//import org.assertj.core.api.Assertions;
//import org.junit.Before;
//import org.junit.Test;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import ru.yandex.inside.yt.kosher.cypress.YPath;
//import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
//import ru.yandex.market.yql_query_service.service.QueryService;
//import ru.yandex.market.yql_test.annotation.YqlTest;
//
//public class AssortmentResponsiblesLoaderTest extends DeepmindBaseDbTestClass {
//    private static final String TABLE =
//        "//home/market/prestable/mstat/dictionaries/autoorder/assortment_responsibles/latest";
//    @Resource
//    private JdbcTemplate yqlJdbcTemplate;
//    @Resource
//    private QueryService queryService;
//    private AssortmentResponsiblesLoader assortmentResponsiblesLoader;
//
//    @Before
//    public void setUp() throws Exception {
//        assortmentResponsiblesLoader = new AssortmentResponsiblesLoader(
//            yqlJdbcTemplate,
//            YPath.simple(TABLE),
//            queryService
//        );
//    }
//
//    @Test
//    @YqlTest(
//        schemasDir = "/yt/schemas",
//        schemas = {
//            TABLE,
//        },
//        csv = "AssortmentResponsiblesLoaderTest.yql.before.csv",
//        yqlMock = "AssortmentResponsiblesLoaderTest.yql.mock"
//    )
//    public void test() {
//        var mskuToResponsibles = assortmentResponsiblesLoader.getResponsiblesByMsku1P(List.of(974189L, 1016430L));
//        Assertions.assertThat(mskuToResponsibles)
//            .containsExactlyInAnyOrder("masvlsergeeva", "adanilova85");
//    }
//}
