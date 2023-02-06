package ru.yandex.common.util.json;

import junit.framework.TestCase;
import ru.yandex.common.util.Su;
import ru.yandex.common.util.collections.Cf;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Date: Jul 19, 2011
 * Time: 6:49:44 PM
 *
 * @author Dima Schitinin, dimas@yandex-team.ru
 */
public class PlainJsonParametersSourceTest extends TestCase {

    private PlainJsonParametersSource params;

    @Override
    protected void setUp() throws Exception {
        params = new PlainJsonParametersSource();
        params.setMultiParam("sources", asList("10", "20", "30"));
    }

    public void testGetMultiParamsAsLongList() throws Exception {
        assertEquals(asList(10l, 20l, 30l), params.getMultiParamsAsLongList("sources"));        
        assertEquals(Collections.<Long>emptyList(), params.getMultiParamsAsLongList("attributes"));        
    }

    public void testManyFromRealLifeOneOfThemFails() throws Exception {
        final List<String> ins = Cf.list(
            "{\"EXTRACTOR\":\"specRobotExtractorFactory\", \"LISTENER\":\"specRobotListenerFactory\", \"PROCESSORS\":[\"importProcessor\"], \"ENTITY_TYPE_ID\":\"14\"}",
            "{\"ENTITY_TYPE_ID\":\"13\", \"SP_ROBOT_TYPE_ID\":\"1\", \"SP_ROBOT_BEAN_NAME\":\"androidMarketAllEnExtractor\"}",
            "{\"ENTITY_TYPE_IDS\":\"13,14\"}",
            "{\"ENTITY_TYPE_ID\":\"14\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app/itunes-ru-newest\" , \"page-provider\":\"iTunesRuNewestPageProvider\"}",
            "{\"factory\":\"visitableServiceEntityPumpFactory\", \"preloader\":\"freshPreloader\"}",
            "{\"ENTITY_TYPE_ID\":\"13\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app/blackberry\", \"page-provider\":\"scriptPageProvider\"}",
            "{\"filler\":\"itunesCompatibilityFiller\", \"factory\":\"parameterizedEntityPumpFactory\", \"ENTITY_TYPE_IDS\":[13,14], \"ATTRIBUTES\":[\"url\", \"requirements\"], \"SOURCES\":[57304, 57304]}",
            "{\"days\":\"4\"}",
            "{\"ENTITY_TYPE_ID\":\"14\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app/itunes-ru\", \"page-provider\":\"commonPageProvider\"}",
            "{\"ENTITY_TYPE_ID\":\"-3\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app-staff/blackberry-popular\", \"page-provider\":\"scriptPageProvider\"}",
            "{\"filler\":\"mentionFiller\", \"factory\":\"parameterizedEntityPumpFactory\", \"ATTRIBUTES\":[\"url\"], \"ENTITY_TYPE_IDS\":[13, 14], \"EVENT\":\"UPDATE\", \"REMOTENESS\":\"1\"}",
            "{\"ENTITY_TYPE_ID\":\"13\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app/itunes-com\", \"page-provider\":\"commonPageProvider\"}",
            "{\"ENTITY_TYPE_ID\":\"-3\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app-staff/market.android.com-popular\", \"page-provider\":\"scriptPageProvider\"}",
            "{\"ENTITY_TYPE_ID\":\"13\", \"SP_ROBOT_TYPE_ID\":\"1\", \"SP_ROBOT_BEAN_NAME\":\"newestAppsMarketExtractor\"}",
            "{\"EXTRACTOR\":\"specRobotExtractorFactory\", \"LISTENER\":\"specRobotListenerFactory\", \"PROCESSORS\":[\"importProcessor\"], \"ENTITY_TYPE_ID\":\"-1\"}",
//            "{\"filler\":\"categoryFiller\", \"parameterizedEntityPumpFactory\", \"ENTITY_TYPE_IDS\":[-1]}",
            "{\"ENTITY_TYPE_ID\":\"14\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app/ovi-ru\", \"page-provider\":\"commonPageProvider\"}",
            "{\"ENTITY_TYPE_ID\":\"-3\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app-staff/itunes.apple.com-popular\"}",
            "{\"ENTITY_TYPE_ID\":\"14\", \"SP_ROBOT_TYPE_ID\":\"1\", \"SP_ROBOT_BEAN_NAME\":\"newestRuAppsMarketExtractor\"}",
            "{\"EXTRACTOR\":\"specRobotExtractorFactory\", \"LISTENER\":\"specRobotListenerFactory\", \"PROCESSORS\":[\"importProcessor\"], \"ENTITY_TYPE_ID\":\"13\"}",
            "{\"filler\":\"mentionFiller\", \"pump\":\"allAppsUrls\"}",
            "{\"filler\":\"oviCompatibilityFiller\"}",
            "{\"ENTITY_TYPE_ID\":\"13\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app/ovi-en\", \"page-provider\":\"commonPageProvider\"}",
            "{\"ENTITY_TYPE_ID\":\"-1\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app-staff/itunes.apple.com-category\", \"page-provider\":\"commonPageProvider\"}",
            "{\"EXTRACTOR\":\"specRobotExtractorFactory\", \"LISTENER\":\"specRobotListenerFactory\", \"PROCESSORS\":[\"importProcessor\"], \"ENTITY_TYPE_ID\":\"-3\"}",
            "{\"filler\":\"popularityFiller\", \"factory\":\"parameterizedEntityPumpFactory\", \"ENTITY_TYPE_IDS\":[-3]}",
            "{\"ENTITY_TYPE_ID\":\"14\", \"SP_ROBOT_TYPE_ID\":\"1\", \"SP_ROBOT_BEAN_NAME\":\"androidMarketAllRuExtractor\"}",
            "{\"DOMAIN_IDS\":[57304, 57303], \"PATH_TO_EXPORT_FILE\":\"/tmp/itunes_suggest.txt\"}",
            "{\"DOMAIN_IDS\":[57300, 57301], \"PATH_TO_EXPORT_FILE\":\"/tmp/amarket_suggest.txt\"}",
            "{\"ENTITY_TYPE_ID\":\"-3\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app-staff/ovi-popular\"}",
            "{\"ENTITY_TYPE_ID\":\"13\", \"SP_ROBOT_TYPE_ID\":\"0\", \"SP_ROBOT_CONFIG_PATH\":\"mobile-app/itunes-en-newest\" , \"page-provider\":\"iTunesEnNewestPageProvider\"}"
        );

        for (final String in : ins) {
            System.out.println("in => " + in);
            final ExtendedJsonParametersSource src = new ExtendedJsonParametersSource(in);
            System.out.println(src.getNames() + " " + src.getAllParams().size());
        }
    }

    public void testParseLong() throws Exception {

        final PlainJsonParametersSource s = new PlainJsonParametersSource("{config-id:10}");
        assertEquals(10L, s.getParamAsLong("config-id", -1)); // выводит -1
        assertEquals("10", s.getParam("config-id")); // выводит 10
        assertEquals(Cf.list(10L), s.getMultiParamsAsLongList("config-id"));
        assertEquals(Cf.list(10), s.getMultiParamsAsIntList("config-id"));

    }

    public void testGetAll() throws Exception {
        final PlainJsonParametersSource s = new PlainJsonParametersSource("{config-id:[10, 20, 30],foo:\"bar\",bar:13}");
        System.out.println(Su.join(s.getAllParams(), "\n"));
    }

}
