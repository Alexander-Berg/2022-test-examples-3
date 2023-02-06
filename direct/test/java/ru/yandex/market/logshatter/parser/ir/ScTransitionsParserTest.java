package ru.yandex.market.logshatter.parser.ir;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 13/04/16
 */
public class ScTransitionsParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new ScTransitionsParser());
        checker.check(
            "date=2016-04-13T03:51:00+0300\toffer_id=001ff978fc40bfcf0a2caf0060b56d3e\tcategory_id=7814994\tvendor_id=8339653\tfeed_id=291782\tshop_id=62666\tstatus=UPDATE\told_cluster_id=-1\tnew_cluster_id=1294975215\told_clutch_type=VENDOR_CLUTCH\tnew_clutch_type=OFFER_ID_CLUTCH_OK\tshop_offer_id=42\tclassifier_good_id=26995a452f1bc941cf71bf8c87e13bd9\tware_md5=EqIxrT-6Tag0cYbAF-b3ag"
        );

        checker.check(
            "date=2016-04-13T03:51:00+0300\toffer_id=001ff978fc40bfcf0a2caf0060b56d3e\tcategory_id=7814994\tvendor_id=8339653\tfeed_id=291782\tshop_id=62666\tstatus=UPDATE\told_cluster_id=-1\tnew_cluster_id=1294975215\told_clutch_type=VENDOR_CLUTCH\tnew_clutch_type=OFFER_ID_CLUTCH_OK\tshop_offer_id=42\tclassifier_good_id=26995a452f1bc941cf71bf8c87e13bd9\tware_md5=EqIxrT-6Tag0cYbAF-b3ag"
        );

        checker.check(
            "date=2016-04-13T03:51:00+0300\toffer_id=011c4eccc4b75f969f4f08b325cf5375\tcategory_id=5037939\tvendor_id=987647\tfeed_id=378286\tshop_id=230298\tstatus=UPDATE\told_category_id=\tnew_category_id=5037939\told_classifier_category_id=90582\tnew_classifier_category_id=5037939\told_matched_type=\tnew_matched_type=NO_MATCH\tshop_offer_id=42\tclassifier_good_id=26995a452f1bc941cf71bf8c87e13bd9\tware_md5=EqIxrT-6Tag0cYbAF-b3ag"
        );

        checker.check(
            "date=2016-09-20T05:15:00+0300\toffer_id=0347d0c5d74efa610f9d242aecdc1be8\tcategory_id=7812908\tvendor_id=6211303\tfeed_id=291782\tshop_id=62666\tshop_offer_id=NI464EWJFW88INXL\tclassifier_good_id=d5bba866df3f32ae120aad77de6025a4\tware_md5=4ZR8g1MaVlTc0b1XqRRQOw\tstatus=NEW\tnew_category_id=7812908\tnew_mapped_id=90401\tnew_classification_type=Norm\tnew_classifier_category_id=7812908\tnew_matched_category_id=0\tnew_vendor_id=6211303\tnew_matched_type=NO_MATCH\tnew_matched_id=0\tnew_local_vendor_id=0\tnew_model_id=0\tnew_modification_id=0\tnew_light_model_id=0\tnew_light_modification_id=0\tnew_light_match_type=NO_MATCH\tnew_cluster_id=1608872132\tnew_clutch_type=ALIAS_CLUTCH_OK"
        );

        String line1 = "date=2016-09-21T05:25:00+0300\toffer_id=08aa04e8546094277dfa05fda7f0cf66\tstatus=DELETE\tcategory_id=7814994\tvendor_id=8336571\tfeed_id=291782\tshop_id=62666\tshop_offer_id=VA984AUJWR50A045\tclassifier_good_id=26995a452f1bc941cf71bf8c87e13bd9\tware_md5=EqIxrT-6Tag0cYbAF-b3ag\told_long_cluster_id=100500100500\tnew_long_cluster_id=100500100500";

        checker.check(
            line1,
            1474424700,
            checker.getHost(),
            "08aa04e8546094277dfa05fda7f0cf66",
            "26995a452f1bc941cf71bf8c87e13bd9",
            "EqIxrT-6Tag0cYbAF-b3ag",
            7814994,
            8336571,
            62666,
            291782,
            "VA984AUJWR50A045",
            "DELETE",
            -2, -2,
            -2, -2,
            "", "",
            -2, -2,
            -2, -2,
            -2, -2,
            "", "",
            -2, -2,
            -2, -2,
            -2, -2,
            -2, -2,
            "", "",
            -2, -2,
            -2, -2,
            "", "",
            -2L, -2L,
            100500100500L, 100500100500L
        );

        String line2 = "date=2016-09-21T05:25:00+0300\toffer_id=002e2d13f773cb02ba91600e259cb201\tcategory_id=7815003\tvendor_id=8338943\tfeed_id=443645\tshop_id=179622\tshop_offer_id=1301470711953861\tclassifier_good_id=8848724b2b830cf9921e232a3c9e92aa\tware_md5=xC3Ed6MENxyRBEYo_TAusQ\tstatus=NEW\tnew_category_id=7815003\tnew_mapped_id=7815003\tnew_classification_type=Mapped\tnew_classifier_category_id=7815003\tnew_matched_category_id=0\tnew_vendor_id=8338943\tnew_matched_type=NO_MATCH\tnew_matched_id=0\tnew_local_vendor_id=0\tnew_model_id=0\tnew_modification_id=0\tnew_light_model_id=0\tnew_light_modification_id=0\tnew_light_match_type=NO_MATCH\tnew_cluster_id=1702986963\tnew_clutch_type=OFFER_ID_CLUTCH_OK\told_long_cluster_id=100500100500\tnew_long_cluster_id=100500100500";
        checker.check(
            line2,
            1474424700,
            checker.getHost(),
            "002e2d13f773cb02ba91600e259cb201",
            "8848724b2b830cf9921e232a3c9e92aa",
            "xC3Ed6MENxyRBEYo_TAusQ",
            7815003,
            8338943,
            179622,
            443645,
            "1301470711953861",
            "NEW",
            -2, 7815003,
            -2, 7815003,
            "", "Mapped",
            -2, 7815003,
            -2, 0,
            -2, 8338943,
            "", "NO_MATCH",
            -2, 0,
            -2, 0,
            -2, 0,
            -2, 0,
            "", "NO_MATCH",
            -2, 0,
            -2, 0,
            "", "OFFER_ID_CLUTCH_OK",
            -2L, 1702986963L,
            100500100500L, 100500100500L
        );
    }
}