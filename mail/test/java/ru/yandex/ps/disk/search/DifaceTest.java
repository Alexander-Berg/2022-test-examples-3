package ru.yandex.ps.disk.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.proxy.ProxySession;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.ps.disk.search.delta.ClusterDelta;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.util.string.UnhexStrings;

public class DifaceTest extends TestBase {
    protected void indexResource(
        final DifaceCluster cluster,
        final long prefix,
        final String stid,
        final String id,
        final String kaliAction,
        final long version)
        throws Exception
    {
        StringBuilderWriter sbw = new StringBuilderWriter();
        try (JsonWriter writer = JsonType.NORMAL.create(sbw)) {
            writer.startArray();
            writer.startObject();
            writer.key("uid");
            writer.value(prefix);
            writer.key("stid");
            writer.value(stid);
            writer.key("height");
            writer.value(200);
            writer.key("width");
            writer.value(100);
            writer.key("preview_stid");
            writer.value(stid);
            writer.key("resource_id");
            writer.value(id);
            writer.key("id");
            writer.value(id);
            writer.endObject();
            writer.endArray();
        }

        String uri = "/face?prefix=" + prefix +"&action=" + kaliAction+ "&ref-queue-id=" + version + "&ref-queue=disk_queue&zoo-queue-id=" + version;
        HttpPost post = new HttpPost(cluster.server().host().toString() + uri);
        post.setEntity(new StringEntity(sbw.toString(), StandardCharsets.UTF_8));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
    }

    public static double[] bytesToFloatArray(final byte[] byteArray){
        int times = Float.SIZE / Byte.SIZE;
        double[] doubles = new double[byteArray.length / times];
        for(int i=0;i<doubles.length;i++){
            doubles[i] = ByteBuffer.wrap(byteArray, i*times, times).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return doubles;
    }

    @Test
    public void testDotProduct() throws Exception {
        URI uri = new URI("1");
        System.out.println(uri.parseServerAuthority());
        System.out.println("host " + uri.getHost());
        //Files.write(Paths.get(new File("/tmp/image.png").toURI()), UnhexStrings.unhex(loadResourceAsString("image.hex")));
        //double[] vector1 = new double[]{0.0686134,0.0815279,0.135428,0.0436081,0.0431698,0.0261578,0.0562518,0.0313502,0.0640735,0.0343087,0.039305,0,0,0.0167853,0,0.0190622,0,0.0132785,0,0.0927395,0.0666068,0,0,0,0,0.100543,0.0537679,0.0637122,0.0823434,0.015338,0,0.0743951,0,0.0726348,0,0.0946564,0.0151033,0.0609844,0.105175,0.139983,0.0346796,0.0262352,0,0.0501874,0.103706,0,0.00837961,0,0.0403211,0.0598068,0.00759883,0,0,0.102036,0,0,0.0226023,0,0,0.0759353,0.1046,0.102817,0.0916521,0.0131761,0.139136,0,0.00739561,0.118101,0,0.0440156,0.0438662,0.0496479,0,0.0765674,0.0880693,0.0102891,0.106303,0.0878819,0.0116084,0.058245,0.00270307,0.0471998,0.113107,0.0573072,0.0417764,0.115169,0.0297402,0.0554934,0.0468924,0.126823,0.074387,0.0402389,0.0544315,0.0360308,0.0408975,0.0594612,0,0.0555918,0.0318524,0.105647,0.0591062,0.0351785,0.0643785,0,0.0200078,0.136907,0.0388909,0.0263714,0,0,0.0361438,0.109399,0,0.072702,0.110207,0.0668838,0.0329564,0.0819026,0,0.0198482,0.0793946,0.0982127,0,0.0510315,0,0,0.098398,0.0309323,0.0756932,0.0300673,0.0628915,0,0.0486037,0.101756,0.0292268,0,0.074235,0.0254145,0.0387292,0,0,0.0299701,0.0304228,0.0927931,0.0246448,0.0564796,0.105466,0,0,0,0.033936,0.069158,0,0.115089,0.126707,0.218356,0.104816,0.206925,0.00348096,0.051642,0.0503121,0.0576129,0.0528752,0.0410032,0,0.0576041,0.0404506,0.0631614,0.0208616,0,0.114203,0.0436395,0.0734487,0.013817,0,0.190796,0,0.0663364,0.0329444,0.0497768,0.0147442,0.0829922,0.0171765,0.0097403,0.085556,0.105989,0.0983885,0.0565382,0.0339193,0.0232905,0.0479009,0,0.0741908,0,0.131582,0,0,0.0733285,0.0721164,0.0998546,0.0674409,0,0,0.0881728,0.0271844,0.0281328,0.0962015,0.0277176,0.025697,0,0.0731067,0,0.0328152,0.0554615,0.0771885,0.0122766,0.0291203,0.100631,0.0620189,0.0132759,0.0050129,0.0518419,0.0719999,0.0647709,0.0770881,0.0181631,0.0188832,0.0583419,0,0.0608559,0.0564393,0.0920892,0.0422008,0,0.0283518,0.0710356,0.0843555,0.0970019,0.0375984,0.0265085,0.0675364,0.0755025,0,0,0.0312091,0,0,0.107143,0.104309,0.0331286,0,0.0534162,0.0346275,0,0,0.0631706};
        //double[] vector2 = new double[]{0.0951306,0.00849044,0.0789727,0,0.0723935,0.152036,0.0957326,0.0250609,0.103871,0.0271979,0.051323,0.0602007,0.0193295,0,0,0.021537,0.0648127,0.0483262,0.0477091,0.0710866,0.0575488,0.0379356,0.0287083,0.0215416,0.0898973,0.148615,0.0846508,0.0626434,0,0,0,0.0558093,0,0.107949,0,0.0168766,0.0105245,0.028754,0.0634958,0.0394314,0.0272249,0,0,0.0347139,0.10251,0,0.0134598,0.0293809,0.0616848,0.0214408,0,0.0355309,0.00784553,0.0902219,0.0391825,0,0,0.0339928,0.0905828,0,0,0.0726353,0.0457931,0.0127598,0.0578821,0,0.0275675,0.135532,0.00436588,0.0238376,0.106273,0.0314347,0.0113779,0.0781966,0.0378046,0.0144981,0,0.0119719,0,0.0926662,0.0993804,0.0336943,0.169394,0.018704,0.0711129,0.112461,0.0561588,0.0572908,0.0717618,0,0.0754737,0.0165623,0.0289067,0.0662713,0,0.00622465,0,0.0186631,0.129632,0.155467,0.0792161,0.0729996,0.0633764,0,0.00375631,0.130268,0,0,0.0771624,0,0,0.0792208,0,0.0741917,0.0927914,0.0905062,0.0690951,0.0639439,0,0,0.0834403,0.104653,0.0354605,0.0915365,0,0,0.0488762,0.100288,0.0335492,0,0.0175873,0,0.0609726,0.0264648,0,0.0214362,0.0451919,0.0680077,0,0.00105775,0.0378099,0.0490704,0.0647917,0.0520525,0.0402552,0.0723408,0.0325893,0,0,0.0559843,0.063583,0.0707304,0,0.11838,0.0721368,0.234527,0.00851843,0,0.0614172,0.0158652,0.00444149,0.0751933,0.0678479,0.0709125,0,0.0187762,0.0109566,0,0.0244523,0.0169381,0.122991,0.0830992,0.0558099,0.0383041,0.0574647,0.0832648,0.0387621,0.0871909,0,0.020639,0.0469369,0.0701321,0.118477,0.0728603,0.0718001,0.0366708,0.0737984,0.0480511,0.102281,0,0.175095,0.0637263,0.010244,0.00742448,0,0.056836,0.0235716,0.022547,0,0.00188067,0,0,0.00315855,0.0392189,0,0.00776624,0.00018909,0.0871129,0,0,0.0289636,0.0684289,0.0205106,0.00115807,0.112546,0.0484905,0.0436516,0,0,0.0938172,0.0383562,0.0549027,0.0406512,0.100356,0.109779,0.047405,0,0.0490212,0.107977,0.06936,0.018371,0.0773932,0.0411832,0.0400285,0.0516639,0,0.00898177,0.110699,0.0807048,0,0.0382702,0.100424,0,0,0.117256,0,0,0.0969208,0.0686302,0.0682285,0,0.100746,0.217908,0.0939699,0.0302885,0.0350791};
        //double[] vector1 = new double[]{0.0951305,0.00849039,0.0789728,0,0.0723933,0.152036,0.0957328,0.0250609,0.103871,0.0271976,0.051323,0.0602009,0.0193295,0,0,0.0215371,0.0648127,0.0483261,0.047709,0.0710866,0.0575489,0.0379356,0.0287083,0.0215415,0.0898974,0.148615,0.0846507,0.0626434,0,0,0,0.0558094,0,0.107949,0,0.0168765,0.0105245,0.0287541,0.0634957,0.0394312,0.027225,0,0,0.0347139,0.10251,0,0.0134599,0.0293809,0.0616849,0.021441,0,0.0355309,0.00784546,0.0902218,0.0391823,0,0,0.0339926,0.0905828,0,0,0.0726353,0.0457931,0.0127597,0.0578821,0,0.0275677,0.135532,0.00436585,0.0238376,0.106273,0.0314347,0.0113777,0.0781968,0.0378045,0.0144981,0,0.0119719,0,0.0926661,0.0993803,0.0336943,0.169394,0.0187042,0.0711127,0.112461,0.0561588,0.0572908,0.0717618,0,0.0754737,0.0165623,0.0289067,0.0662715,0,0.00622445,0,0.018663,0.129632,0.155467,0.079216,0.0729996,0.0633764,0,0.00375623,0.130268,0,0,0.0771624,0,0,0.0792211,0,0.0741917,0.0927914,0.0905063,0.0690952,0.0639439,0,0,0.0834403,0.104653,0.0354605,0.0915364,0,0,0.0488763,0.100288,0.0335492,0,0.0175873,0,0.0609726,0.0264648,0,0.0214362,0.0451917,0.0680077,0,0.00105774,0.03781,0.0490703,0.0647916,0.0520524,0.0402551,0.0723409,0.0325892,0,0,0.0559844,0.0635831,0.0707305,0,0.11838,0.0721367,0.234527,0.00851841,0,0.0614172,0.0158652,0.00444151,0.0751932,0.0678478,0.0709126,0,0.0187761,0.0109566,0,0.0244523,0.0169382,0.122991,0.0830992,0.0558099,0.038304,0.0574647,0.0832648,0.038762,0.0871908,0,0.020639,0.0469369,0.0701321,0.118477,0.0728602,0.0718003,0.036671,0.0737984,0.0480509,0.102281,0,0.175095,0.0637263,0.010244,0.00742449,0,0.0568361,0.0235716,0.0225471,0,0.00188051,0,0,0.00315861,0.0392188,0,0.00776617,0.000189131,0.087113,0,0,0.0289635,0.068429,0.0205105,0.00115805,0.112546,0.0484906,0.0436516,0,0,0.0938173,0.0383562,0.0549027,0.0406509,0.100356,0.109779,0.0474051,0,0.0490212,0.107977,0.0693599,0.018371,0.0773933,0.0411832,0.0400286,0.0516639,0,0.00898169,0.110699,0.0807046,0,0.0382703,0.100424,0,0,0.117256,0,0,0.0969208,0.0686303,0.0682284,0,0.100746,0.217908,0.09397,0.0302885,0.0350789};
        //double[] vector2 = new double[]{0.0686332,0.0815126,0.135461,0.0435832,0.0431952,0.0261204,0.0562522,0.0313339,0.0640543,0.034355,0.0393102,0,0,0.0167662,0,0.0190575,0,0.0132741,0,0.0927546,0.0666272,0,0,0,0,0.100569,0.0537207,0.0637068,0.0823003,0.0153351,0,0.074435,0,0.0726496,0,0.094627,0.0151201,0.0610226,0.105178,0.139947,0.0346522,0.0262301,0,0.0501844,0.103695,0,0.00837343,0,0.0403232,0.0598363,0.00758735,0,0,0.102059,0,0,0.0225944,0,0,0.075893,0.104629,0.102813,0.0916314,0.0131845,0.139142,0,0.0073865,0.118114,0,0.0440211,0.043886,0.0496218,0,0.076511,0.088077,0.0102875,0.106298,0.0878876,0.0116215,0.0582416,0.00271917,0.0471515,0.113098,0.0572559,0.0417852,0.115141,0.029747,0.055509,0.046892,0.126796,0.0743941,0.0402359,0.0544092,0.0360296,0.0409431,0.0594654,0,0.055591,0.0318652,0.105618,0.0590885,0.0351681,0.0643614,0,0.0200368,0.136911,0.0388965,0.0263559,0,0,0.0361647,0.109354,0,0.0726689,0.110189,0.0668737,0.0329376,0.0818634,0,0.0198171,0.0793828,0.0982099,0,0.0510295,0,0,0.0984205,0.0309377,0.0757105,0.0300605,0.062913,0,0.0486163,0.101766,0.0292019,0,0.0742414,0.0254122,0.0387142,0,0,0.0299774,0.0304072,0.0927915,0.0246636,0.0564672,0.105454,0,0,0,0.033972,0.069135,0,0.11513,0.126738,0.218339,0.104816,0.206972,0.00349348,0.051654,0.0503254,0.0576148,0.0528831,0.0409994,0,0.0575654,0.0404689,0.0631326,0.0208558,0,0.114192,0.043654,0.0734444,0.0138341,0,0.190805,0,0.0663627,0.0329405,0.0497926,0.014764,0.0830098,0.0171878,0.00974678,0.0855362,0.106007,0.0984078,0.0565479,0.0339369,0.0232737,0.0478906,0,0.0742028,0,0.131553,0,0,0.0733303,0.0721161,0.0998895,0.0674548,0,0,0.088184,0.0272068,0.0281353,0.0961818,0.0277004,0.0256905,0,0.0730863,0,0.0328039,0.0554715,0.0771789,0.0122581,0.0291019,0.100667,0.0619888,0.0133071,0.00500664,0.0518739,0.0720486,0.0647597,0.0770847,0.0181342,0.0188554,0.0583584,0,0.0608337,0.0564503,0.0920751,0.0421862,0,0.0283653,0.0710439,0.0843449,0.0970239,0.0375923,0.0265046,0.0675442,0.075551,0,0,0.0311777,0,0,0.107123,0.104319,0.0331305,0,0.0534158,0.0346238,0,0,0.0631886};
        double[] vector1 = Face.bytesToDoubleArray(new byte[]{0,0,0,0,-94,-35,56,60,21,-34,-31,61,-43,42,-62,61,30,95,127,60,-7,-91,6,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,97,9,37,61,0,0,0,0,0,0,0,0,118,6,-49,61,0,0,0,0,0,0,0,0,0,0,0,0,-127,-81,-125,61,0,0,0,0,-14,58,61,61,50,-99,-21,60,0,0,0,0,-65,68,-114,60,0,0,0,0,0,0,0,0,2,73,-24,60,108,-101,-64,61,23,124,-92,60,0,0,0,0,85,88,19,61,0,0,0,0,-71,-34,100,61,0,0,0,0,0,0,0,0,0,0,0,0,84,-71,-19,61,0,0,0,0,0,0,0,0,48,72,-77,61,-66,40,-115,62,0,0,0,0,0,0,0,0,0,0,0,0,67,41,-89,59,-97,-12,-63,60,0,0,0,0,14,59,110,60,-81,-116,0,62,0,0,0,0,0,0,0,0,-93,-107,-90,60,0,0,0,0,0,0,0,0,-72,26,-94,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,14,20,-59,61,-104,-65,70,61,0,0,0,0,72,-21,-46,60,8,126,-93,60,-123,-82,31,60,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,-68,-91,23,62,4,-111,3,61,0,0,0,0,0,0,0,0,-21,-125,-98,61,-99,68,-36,60,32,23,26,61,0,0,0,0,0,0,0,0,125,17,-123,61,-15,23,-104,61,0,0,0,0,-92,32,-71,61,-80,63,-17,60,126,105,-119,60,-36,45,-80,61,92,36,4,62,46,12,114,61,-108,65,26,62,9,-39,117,61,35,-128,-26,61,75,125,-73,60,0,0,0,0,-15,-88,60,61,0,0,0,0,92,-58,-49,61,-4,14,-127,60,0,0,0,0,0,0,0,0,0,0,0,0,74,60,3,61,0,0,0,0,-57,69,-30,61,0,0,0,0,0,0,0,0,-29,-31,-86,60,-63,41,-70,61,0,0,0,0,-65,109,24,62,0,0,0,0,0,0,0,0,-91,-94,-86,60,-10,-92,7,62,64,111,-121,60,0,0,0,0,-62,89,72,62,0,0,0,0,0,0,0,0,-45,100,7,62,-1,-28,57,59,0,0,0,0,64,-108,-10,61,-126,89,-16,61,120,-82,116,60,8,-63,-59,61,0,0,0,0,0,0,0,0,0,0,0,0,-31,-27,-69,61,0,0,0,0,0,0,0,0,-102,56,-92,61,0,0,0,0,-111,-24,73,61,-83,-16,-24,61,0,0,0,0,0,0,0,0,-92,123,23,62,0,0,0,0,-34,-5,-34,61,105,124,115,60,0,0,0,0,0,0,0,0,109,84,58,62,40,-114,-83,61,86,-68,-45,61,0,0,0,0,-73,50,108,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,116,59,-64,61,119,75,-28,61,-46,115,39,61,48,-91,121,59,-31,1,124,61,40,68,61,62,62,114,-115,60,0,0,0,0,18,-85,-18,61,47,100,38,61,0,0,0,0,-56,-33,107,61,0,0,0,0,-54,63,4,61,0,0,0,0,66,109,-105,61,-9,101,-67,60,0,0,0,0,-96,1,124,61,124,-34,70,62,0,0,0,0,0,0,0,0,0,0,0,0,93,106,-101,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,-27,97,-64,60,0,0,0,0,80,-60,-41,60,-44,115,123,61,-110,-96,-69,61,-112,-64,75,61,-6,26,-97,61,47,-68,-74,61,-64,101,104,61,0,0,0,0,-77,-10,18,61,3,54,104,61,-8,9,29,61,93,-35,-66,60,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,-22,-52,-30,60,0,0,0,0,-93,-9,14,61,0,0,0,0,95,10,-111,61,0,0,0,0,0,0,0,0,-114,112,-66,61,56,-17,5,61,0,0,0,0,0,0,0,0,0,0,0,0,26,-120,1,62,0,0,0,0,-110,104,-36,61,0,0,0,0,-120,91,-53,61,-110,76,111,61,-70,43,-92,61,-35,34,12,61,0,0,0,0,0,0,0,0,78,-121,-57,59,86,38,7,62,-66,-110,-5,61,-34,-17,-107,61,-102,-22,-77,61,-122,37,92,61,0,0,0,0,0,0,0,0,0,0,0,0,88,-106,-57,61,0,0,0,0,0,0,0,0,-22,41,61,61,0,0,0,0,67,-76,29,62,-40,120,8,61,0,0,0,0,0,0,0,0,0,0,0,0,121,108,-109,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,21,-101,107,60,-103,41,-106,60,0,0,0,0,0,0,0,0,88,-124,-82,59,122,-66,-50,60,0,0,0,0,-49,80,-44,61,6,-37,125,62,0,0,0,0,-121,-52,95,61,113,-82,-2,60});
        double[] vector2 = Face.bytesToDoubleArray(new byte[]{0,0,0,0,108,-99,109,60,-123,118,-47,61,-116,-3,5,62,68,47,-33,61,72,-128,-32,61,0,0,0,0,0,0,0,0,-67,-57,18,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,-75,36,8,62,0,0,0,0,110,-14,-91,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,-27,-2,63,61,-89,-125,-66,61,0,0,0,0,0,0,0,0,-60,-123,10,62,0,0,0,0,0,0,0,0,0,0,0,0,-64,-111,77,62,0,0,0,0,75,-49,-10,61,-34,-89,-99,61,-66,102,-109,62,0,0,0,0,-67,-28,-88,61,0,0,0,0,-27,38,-118,61,-113,42,4,60,0,0,0,0,-107,-76,-71,61,110,93,-32,61,0,0,0,0,0,0,0,0,-108,81,53,62,0,0,0,0,0,0,0,0,44,-52,-100,61,0,0,0,0,0,0,0,0,34,124,1,61,-80,41,-63,59,0,0,0,0,-81,-122,-13,61,96,85,-31,61,0,0,0,0,-41,-117,-126,61,-58,-109,12,61,-53,-100,-42,61,0,0,0,0,0,0,0,0,77,41,28,62,0,0,0,0,-63,-16,-70,61,0,0,0,0,106,23,-126,61,0,0,0,0,17,127,-97,61,-99,-43,-54,61,-19,126,-81,59,-117,23,2,62,0,0,0,0,-82,-96,-32,60,-102,-41,-110,61,0,0,0,0,11,59,17,61,20,-102,-76,61,0,0,0,0,-8,-6,-72,60,-25,115,-56,61,86,-120,48,61,0,0,0,0,90,-100,-91,61,-127,72,-36,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,28,-44,102,61,0,0,0,0,0,0,0,0,34,95,117,61,0,0,0,0,-84,55,-25,61,0,0,0,0,0,0,0,0,-2,98,-103,61,0,0,0,0,51,-120,5,60,-1,-25,-46,61,-59,85,116,61,-112,56,-20,60,0,0,0,0,0,0,0,0,0,0,0,0,-5,17,-55,61,0,0,0,0,0,0,0,0,-61,-63,24,61,0,0,0,0,0,0,0,0,-61,-77,1,62,57,-23,105,60,-20,-89,28,59,52,10,94,61,-9,31,24,60,-21,50,-84,60,-28,-95,121,62,0,0,0,0,0,0,0,0,0,0,0,0,-71,85,35,61,0,0,0,0,43,126,39,60,16,-113,-115,61,0,0,0,0,0,0,0,0,0,0,0,0,-65,-84,-104,60,0,0,0,0,-19,-48,-68,61,0,0,0,0,43,36,-123,61,0,0,0,0,0,0,0,0,69,94,-78,60,-26,-3,-90,61,-30,94,-15,61,-114,56,44,61,0,0,0,0,-99,-77,28,60,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,-78,11,-126,61,75,122,25,62,21,94,-59,61,-77,127,76,60,-32,-72,-71,61,115,-113,125,62,0,0,0,0,0,0,0,0,-15,-92,-83,60,-123,-80,42,60,0,0,0,0,0,0,0,0,0,0,0,0,-34,-96,33,61,0,0,0,0,105,8,-96,60,-28,-121,25,61,0,0,0,0,-59,95,-127,61,-24,-113,-41,61,47,109,-49,60,0,0,0,0,-1,-106,-76,59,-5,124,46,62,0,0,0,0,74,-8,53,60,0,0,0,0,0,0,0,0,-6,102,-112,59,-7,73,16,62,-119,-12,-96,61,0,0,0,0,118,13,19,61,44,21,22,62,14,59,6,59,-119,80,84,61,-79,-39,109,61,0,0,0,0,113,-109,-112,61,0,0,0,0,105,-51,-86,61,0,0,0,0,67,-54,-36,61,0,0,0,0,0,0,0,0,0,0,0,0,16,48,-25,60,0,0,0,0,0,0,0,0,0,0,0,0,-109,-47,-117,61,-71,41,-109,60,0,0,0,0,18,96,-119,61,0,0,0,0,0,0,0,0,-39,117,-79,61,0,0,0,0,-61,68,21,60,0,0,0,0,95,-82,-109,61,38,119,69,60,96,45,-115,61,58,-15,-87,61,-48,-91,-86,60,0,0,0,0,31,-44,77,61,0,0,0,0,0,0,0,0,19,71,-45,60,22,-106,-76,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,79,108,95,61,-80,-61,-102,61,-59,-102,-119,61,-17,8,-84,60,-97,-99,99,61,0,0,0,0,72,-105,-87,61,73,-63,-50,61,59,55,-115,61,0,0,0,0,0,0,0,0,49,98,-105,61,112,-8,-92,59,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,19,-97,17,61,0,0,0,0,-114,88,21,60,0,0,0,0,-67,28,20,62,-7,-125,53,60,0,0,0,0,23,124,86,61,-34,-12,-126,61});

        System.out.println(Face.dotProduct(vector1, vector2));
    }

    @Test
    public void test() throws Exception {
        String sign = "000000000000000059E30D3D5A08AB3D9667093DA31C053DC9810E3D00000000B101913D00000000775C773EE00AD53D000000004509263DA1F4B63D000000000000000000000000000000009BF8383D57D6E03B0DF0BC3D19E4913BEDBE5F3D00000000000000000000000000000000000000006B34A13D00000000A178C53DC68D863D0910593D00000000C703023C000000007361713D00000000356A943C00000000554ED53B00000000BFE25C3C000000000000000013935D3DF67C623B000000000000000000000000A617933D0000000000000000000000000000000021D7173D0000000095F3CC3D00000000886C133E0000000074617D3D0A439D3DE96A583C00000000000000008EFA183D5956AB3C41E5543D7E31763CEE9C1B3C0000000012C3023E05FDE43D22EA323BE88C043C6351E83C7D2FB63C759A4F3E000000004A547B3D8DE4B53DCF0BEC3DE9772B3C1A15503EA1E1713DEE48F23DFAB3243C00000000000000003AB91C3D000000000000000000000000000000003DA8B13D00000000000000000000000000000000142CE03C0000000000000000000000000561DE3D00000000A00EEC3DB64C303D000000009E751C3EC0C2003E000000000000000002163D3D0000000085ABE63DAC41E73D00000000F3B80A3D00000000B2B0E33D45554B3DDDE2823D0000000000000000000000000000000000000000000000001049EB3DAD3D203D0DD0853C379D363DD08F3E3D00000000CBCF1C3E9AA10F3E17C9C93B34B5A73D00000000000000004950233B00000000992AE53C757D6E3D060CD93D11BC0E3E000000000000000000000000000000008392A33D156A533D00000000F430133DBC30D03C0000000000000000A553093C000000006524863C0000000081CFFE390000000053789C3EDA92623EB5E9503CF56B013C000000002364F73D25F8283E000000000000000000000000ED600F3D0000000000000000C3CAB23D000000000000000000000000806EF53DC55AAF3DC146653D0784093EB7C88C3DAC709D3D8EEEF73D00000000000000002385833D6E1A9D3D0000000000000000000000007ACD1C3D68EAD83A650C5C3D00000000A40BAC3D00000000BB58943D0000000019B6B83C000000000000000000000000000000000000000000000000000000006CAC8A3C0000000041EE993D0F8DEC3D0000000065B2F53C0000000000000000000000000000000000000000F65DB13D2DC1243E1BE5E03CA3D1E43CEC35FE3CC499793C6D16303E0000000034320B3DB46CDA3DD6B5BE3D00000000D650B83C000000000000000000000000000000000000000000000000000000000000000000000000000000000000000082FBA93D00000000F595813D00000000000000003DC1BC3D000000000000000000000000";
        byte[] bvector = UnhexStrings.unhex(sign);
        //DecimalFormat df = new DecimalFormat("0.0000000");
        double[] floats = Face.bytesToDoubleArray(bvector);
        System.out.println(Arrays.toString(floats));
        //Arrays.stream(floats).forEach(e -> System.out.print(df.format(e) + " " ));

        try (DifaceCluster cluster = new DifaceCluster(this)) {
            cluster.status(1L);
            String imageData =
                "{\"face_infos\":[{\"x\":0.5208011328,\"y\":0.4226217831,\"width\":0.1004859793,\"height\":0" +
                    ".1625525905,\"image_width\": 100, \"image_height\": 200, \"confidence\":0.982006," +
                    "\"age\":34.3011,\"female_probability\":0.137733," +
                    "\"signature" +
                    "\":\"4F93A43C0000000000000000000000000000000000000000B279973D00000000ABD6153D61A46E3D000000000000000000000000C44FA33C000000000000000071D44B3D0000000000000000000000001406173D112B953D000000000000000000000000A325F53B00000000000000000000000000000000E3618E3BA1578B3D00000000B52BD13D3372A03CE67CEE3C3ADB603DBFE2243D000000000000000054939C3C48A4C33D8278C43CCB058C3DBF8FA83C0000000000000000B3F5813D00000000000000000000000000000000B7B52F3E0000000056838B3E00000000000000002169A23C7E30383D3D081C3D00000000000000000000000010E58A3D1049113D00000000BBAF3B3D2997EC3C00000000000000000000000000000000779D6F3D000000000000000000000000000000001435CD3D00000000000000002DD9C13D0000000000000000E212063D0B822A3C349AC43C0000000000000000000000000000000075D74D3C9D94AD3D0F032E3D22A03B3D3C13733E143EEE3D0000000000000000FA21953D22EF9A3B00000000ADC32A3D6D40733DB601263C6C5ECA3D00000000A8ADA63DAE9C143E72AFFC3D000000000AA0713D00D7043C0000000000000000000000000000000000000000896C573DF2D3473D0000000010CA6D3E6B10B23D0000000000000000000000000000000000000000BDF9C73C75C3453D8599833D00000000000000006910133D000000000000000000000000AACB093DB811653D000000000000000047AAAF3C000000003BF8113D0CFD0A3D8BFAA93D662E923D00000000C55E9D3C00000000D14B493D000000000000000000000000FBD5B73B000000000000000000000000000000005701743E0000000000000000000000008BFC903DCB5AB13D0000000000000000000000000000000000000000D3FF643D000000000000000071D2033EF0AE8F3D25B4273EEE7F843D40DA703C0C96573C000000000000000000000000000000006358923D000000000000000000000000000000003E7A253EA9E4AA3DF82E183D8A39D53DF279B73B520DC13B00000000000000003556093D000000009BF9AD3D9A478D3D2561D33D0000000000000000C816D33C35BC913D00000000000000003DA72B3C000000007266963D0000000000000000BD74523E9C932F3EB25C053E0000000000000000000000006237B43D0000000000000000698BD03D2052B6396A3BA43D00000000EAF6873D2C74DA3C00000000000000000000000000000000E742003E00000000E5685F3D1259973B3CFBA13D1CC1C23D5B9E403DE5B3203C000000000000000059C9043C0262983D0000000000000000D48C083E798FE53C00000000B06D753DF470EC3D69FB103E0000000040A75C3ECD7D8A3C000000003FC6053E647A813E\"}]}";

            cluster.imageparser().add(
                "/process?extract-faces=true&old-cv=false&fail-on-empty=false&stid=320.disk:100.E3333142:200",
                imageData);

            // here we check pure json delta, for format. In other tests we do not check delta format
            // so if you deleting this test, make sure that other tests for format are present
            indexResource(cluster, 1, "320.disk:100.E3333142:200", "abcd100", "update", 20);
            indexResource(cluster, 1, "320.disk:100.E3333142:200", "abcd101", "update", 21);
            cluster.backend().checkSearch(
                "/search?&sort=id&text=id:*&get=*,-face_vector,-facedelta_timestamp&prefix=1&hr",
                new JsonChecker(
                    loadResourceAsString("expected_delta_index_1.json")));
            indexResource(cluster, 1, "320.disk:100.E3333142:200", "abcd101", "rm", 22);
            cluster.backend().checkSearch(
                "/search?&sort=id&text=id:*&get=*,-face_vector,-facedelta_timestamp&prefix=1&hr",
                new JsonChecker(
                    loadResourceAsString("expected_delta_index_2.json")));
        }
    }

    @Test
    public void testWorkflow() throws Exception {
        String faceData =
            "{\"face_infos\":[{\"x\":0.5208011328,\"y\":0.4226217831,\"width\":0.1004859793,\"height\":0" +
                ".1625525905,\"confidence\":0.982006,\"age\":34.3011,\"female_probability\":0.137733," +
                "\"image_width\":100, \"image_height\": 200," +
                "\"signature" +
                "\":\"4F93A43C0000000000000000000000000000000000000000B279973D00000000ABD6153D61A46E3D000000000000000000000000C44FA33C000000000000000071D44B3D0000000000000000000000001406173D112B953D000000000000000000000000A325F53B00000000000000000000000000000000E3618E3BA1578B3D00000000B52BD13D3372A03CE67CEE3C3ADB603DBFE2243D000000000000000054939C3C48A4C33D8278C43CCB058C3DBF8FA83C0000000000000000B3F5813D00000000000000000000000000000000B7B52F3E0000000056838B3E00000000000000002169A23C7E30383D3D081C3D00000000000000000000000010E58A3D1049113D00000000BBAF3B3D2997EC3C00000000000000000000000000000000779D6F3D000000000000000000000000000000001435CD3D00000000000000002DD9C13D0000000000000000E212063D0B822A3C349AC43C0000000000000000000000000000000075D74D3C9D94AD3D0F032E3D22A03B3D3C13733E143EEE3D0000000000000000FA21953D22EF9A3B00000000ADC32A3D6D40733DB601263C6C5ECA3D00000000A8ADA63DAE9C143E72AFFC3D000000000AA0713D00D7043C0000000000000000000000000000000000000000896C573DF2D3473D0000000010CA6D3E6B10B23D0000000000000000000000000000000000000000BDF9C73C75C3453D8599833D00000000000000006910133D000000000000000000000000AACB093DB811653D000000000000000047AAAF3C000000003BF8113D0CFD0A3D8BFAA93D662E923D00000000C55E9D3C00000000D14B493D000000000000000000000000FBD5B73B000000000000000000000000000000005701743E0000000000000000000000008BFC903DCB5AB13D0000000000000000000000000000000000000000D3FF643D000000000000000071D2033EF0AE8F3D25B4273EEE7F843D40DA703C0C96573C000000000000000000000000000000006358923D000000000000000000000000000000003E7A253EA9E4AA3DF82E183D8A39D53DF279B73B520DC13B00000000000000003556093D000000009BF9AD3D9A478D3D2561D33D0000000000000000C816D33C35BC913D00000000000000003DA72B3C000000007266963D0000000000000000BD74523E9C932F3EB25C053E0000000000000000000000006237B43D0000000000000000698BD03D2052B6396A3BA43D00000000EAF6873D2C74DA3C00000000000000000000000000000000E742003E00000000E5685F3D1259973B3CFBA13D1CC1C23D5B9E403DE5B3203C000000000000000059C9043C0262983D0000000000000000D48C083E798FE53C00000000B06D753DF470EC3D69FB103E0000000040A75C3ECD7D8A3C000000003FC6053E647A813E\"}]}";


        try (DifaceCluster cluster = new DifaceCluster(this)) {
            cluster.status(1L);
            //long uid = 283717250;

            List<ClusterDelta> deltas = new ArrayList<>();
            // indexing first
            cluster.indexResource(
                DifaceCluster
                    .createImage("07b5")
                    .face(TypesafeValueContentHandler.parse(faceData)),
                "patch_file",
                "disk_queue",
                1);

            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=id:face*&get=id,type",
                "{\"hitsCount\":1, \"hitsArray\":[{\"id\":\"face_283717250:07b5_0\", \"type\":\"face\"}]}");

            // indexing second
            cluster.indexResource(
                DifaceCluster
                    .createImage("07b6")
                    .face(TypesafeValueContentHandler.parse(faceData)),
                "patch_file",
                "disk_queue",
                2);

            cluster.backend().flush();

            String expectedCluster =
                "{\"hitsCount\":1, \"hitsArray\":[" +
                    "{\"id\": \"face_cluster_283717250_2_0\"," +
                "\"facecluster_id\": \"283717250_2_0\"," +
                "\"facecluster_first_face\": \"283717250:07b6_0\"," +
                "\"facecluster_version\": \"2\"," +
                "\"type\": \"face_cluster\"}]}";

            String expectedFaces =  "{\"hitsCount\":2, \"hitsArray\":[" +
                "{\"id\":\"face_283717250:07b6_0\", \"type\":\"face\"}," +
                "{\"id\":\"face_283717250:07b5_0\", \"type\":\"face\"}" +
                "]}";
            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face&get=id,type&sort=id",
                expectedFaces);
            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face_cluster&get=*&sort=id",
                expectedCluster);

            deltas.add(cluster.delta(
                283717250,
                "283717250_2_0",
                "disk_queue",
                2,
                2,
                cluster.faceAdd("283717250:07b6_0"),
                cluster.faceAdd("283717250:07b5_0"),
                cluster.clusterAdd("283717250_2_0")));

            cluster.checkDelta(283717250, deltas);

            // now check delete
            cluster.indexResource(
                DifaceCluster
                    .createImage("07b6")
                    .face(TypesafeValueContentHandler.parse(faceData)),
                "rm",
                "disk_queue",
                3);

            deltas.add(
                cluster.delta(
                    283717250,
                    "283717250_2_0",
                    "disk_queue",
                    3,
                    3,
                    cluster.faceRemove("283717250:07b6_0")));

            cluster.checkDelta(283717250, deltas);

            // check clusters and faces are present
            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face&get=id,face_deleted,type&sort=id",
                "{\"hitsCount\":2, \"hitsArray\":[" +
                    "{\"id\":\"face_283717250:07b6_0\", \"face_deleted\":\"1\", \"type\":\"face\"}," +
                    "{\"id\":\"face_283717250:07b5_0\", \"face_deleted\":null, \"type\":\"face\"}" +
                    "]}");
            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face_cluster&get=*&sort=id",
                expectedCluster);

            cluster.indexResource(
                DifaceCluster
                    .createImage("07b5")
                    .face(TypesafeValueContentHandler.parse(faceData)),
                "trash_append",
                "disk_queue",
                4);
            cluster.backend().flush();

            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face&get=id,face_deleted,type&sort=id",
                "{\"hitsCount\":2, \"hitsArray\":[" +
                    "{\"id\":\"face_283717250:07b6_0\", \"face_deleted\":\"1\", \"type\":\"face\"}," +
                    "{\"id\":\"face_283717250:07b5_0\", \"face_deleted\":\"1\",  \"type\":\"face\"}" +
                    "]}");
            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face_cluster&get=*&sort=id",
                expectedCluster);

            deltas.add(
                cluster.delta(
                    283717250,
                    "283717250_2_0",
                    "disk_queue",
                    4,
                    4,
                    cluster.faceRemove("283717250:07b5_0")));
            cluster.checkDelta(283717250, "+AND+facedelta_version:[4+TO+100500]", deltas.get(2));

            cluster.indexResource(
                DifaceCluster
                    .createImage("07b5")
                    .face(TypesafeValueContentHandler.parse(faceData)),
                "trash_restore",
                "disk_queue",
                5);
            cluster.backend().flush();

            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face&get=id,face_deleted,type&sort=id",
                "{\"hitsCount\":2, \"hitsArray\":[" +
                    "{\"id\":\"face_283717250:07b6_0\", \"face_deleted\":\"1\", \"type\":\"face\"}," +
                    "{\"id\":\"face_283717250:07b5_0\", \"face_deleted\":null,  \"type\":\"face\"}" +
                    "]}");
            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face_cluster&get=*&sort=id",
                expectedCluster);

            deltas.add(
                cluster.delta(
                    283717250,
                    "283717250_2_0",
                    "disk_queue",
                    5,
                    5,
                    cluster.faceAdd("283717250:07b5_0")));

            cluster.checkDelta(283717250, "+AND+facedelta_version:[5+TO+100500]", deltas.get(3));
            // now we checking skip action
            cluster.indexResource(
                DifaceCluster
                    .createImage("07b6")
                    .face(TypesafeValueContentHandler.parse(faceData)),
                "trash_clean",
                "disk_queue",
                6);
            cluster.backend().flush();
            // check we skipped
            cluster.checkDelta(283717250, deltas);

            cluster.indexResource(
                DifaceCluster
                    .createImage("07aa")
                    .face(TypesafeValueContentHandler.parse(faceData)),
                "patch_file",
                "disk_queue",
                7);
            cluster.backend().flush();

            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face&get=id,face_cluster_id,type&sort=id",
                "{\"hitsCount\":3, \"hitsArray\":[" +
                    "{\"id\":\"face_283717250:07b6_0\", \"face_cluster_id\": \"283717250_2_0\", \"type\":\"face\"}," +
                    "{\"id\":\"face_283717250:07b5_0\", \"face_cluster_id\": \"283717250_2_0\",  \"type\":\"face\"}," +
                    "{\"id\":\"face_283717250:07aa_0\", \"face_cluster_id\": \"283717250_2_0\", \"type\":\"face\"}" +
                    "]}");
            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=type:face_cluster&get=*&sort=id",
                expectedCluster);

            deltas.add(
                cluster.delta(
                    283717250,
                    "283717250_2_0",
                    "disk_queue",
                    7,
                    6,
                    cluster.faceAdd("283717250:07aa_0")));

            // now we check image not to photounlim
            cluster.indexResource(
                DifaceCluster
                    .createImage("0777").etime(null).key("/disk/VasyaDr/image.jpeg")
                    .face(TypesafeValueContentHandler.parse(faceData)),
                "store",
                "disk_queue",
                8);
            cluster.backend().flush();
            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=face_resource_id:283717250%5C:0777*&get=*&sort=id",
                "{\"hitsCount\":0,\"hitsArray\":[]}");
            cluster.checkDelta(283717250, deltas);

            cluster.indexResource(
                DifaceCluster
                    .createImage("0778").etime("125").key("/disk/VasyaDr/image.jpeg")
                    .face(TypesafeValueContentHandler.parse(faceData)),
                "store",
                "disk_queue",
                9);
            cluster.backend().flush();
            cluster.backend().checkSearch(
                "/search?prefix=283717250&text=face_resource_id:283717250%5C:0778*&get=*&sort=id&length=0",
                "{\"hitsCount\":1,\"hitsArray\":[]}");
            deltas.add(
                cluster.delta(
                    283717250,
                    "283717250_2_0",
                    "disk_queue",
                    9,
                    7,
                    cluster.faceAdd("283717250:0778_0")));

            cluster.checkDelta(283717250, deltas);
        }
    }

    private List<Face> loadSides() throws IOException {
        List<Face> sideFaces = new ArrayList<>();
        HashSet<String> sideJpegs = new HashSet<>();
        try (BufferedReader reader =
                 new BufferedReader(
                     new InputStreamReader(
                         this.getClass().getResourceAsStream("sidefacces_jpegs"),
                         StandardCharsets.UTF_8)))
        {
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                sideJpegs.add(line);
            }
        }

        int id = 0;
        try (BufferedReader reader =
                 new BufferedReader(
                     new InputStreamReader(
                         this.getClass().getResourceAsStream("accessors.faces"),
                         StandardCharsets.UTF_8)))
        {
            String line = null;
            //byte[] bvector = new byte[0];
            //double[] dvector = new double[0];
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }

                try {
                    JsonObject obj = TypesafeValueContentHandler.parse(line);
                    JsonMap map = obj.asMap();
                    Iterator<String> iter = map.keySet().iterator();
                    while (iter.hasNext()) {
                        String name = iter.next();
                        if (!sideJpegs.contains(name)) {
                            continue;
                        }
                        JsonList arr = map.getList(name);
                        for (int ja = 0; ja < arr.size(); ja++) {
                            JsonMap fo = arr.get(ja).asMap();
                            Face face =
                                Face.parseFromCoke(
                                    new DiskDoc("accessors/" + name, name, name, 1, 1),
                                    id++,
                                    fo);
//                            float left = (float) fo.getDouble("Left");
//                            float top = (float) fo.getDouble("Top");
//                            float w = (float) fo.getDouble("Width");
//                            float h = (float) fo.getDouble("Height");
//                            float conf = (float) fo.getDouble("Confidence");
//                            JsonList varr = fo.getList("Signature");
//                            bvector = new byte[varr.size()];
//                            dvector = new double[varr.size()];
//                            for (int i = 0; i < varr.size(); i++) {
//                                dvector[i] = varr.get(i).asDouble();
//                            }
//                            Face face = new Face(
//                                "accessor_" + id++,
//                                "accessors/" + name,
//                                1,
//                                1,
//                                left,
//                                top,
//                                w,
//                                h,
//                                conf,
//                                bvector,
//                                dvector,
//                                0,
//                                0);
                            sideFaces.add(face);
                        }
                    }
                } catch (JsonException je) {
                    throw new IOException("Failed to load side faces, invalid file", je);
                }
            }

            logger.info("Sidefaces are loaded, size: " + sideFaces.size());
        }

        return sideFaces;
    }

    @Test
    public void testConvertSidefaces() throws Exception {
        List<Face> faces = loadSides();
        StringBuilderWriter sbw = new StringBuilderWriter();
        JsonWriter writer = JsonType.NORMAL.create(sbw);
        writer.startArray();
        for (Face face: faces) {
            writer.startObject();
            writer.key(FaceBackendFields.FACE_ID.stored());
            writer.value(face.faceId());
            writer.key(FaceBackendFields.FACE_RESOURCE_ID.stored());
            writer.value(face.resourceId());
            writer.key(FaceBackendFields.FACE_VECTOR.stored());
            writer.startArray();
            for (double db: face.dvector()) {
                writer.value(db);
            }
            writer.endArray();
            writer.endObject();
        }

        writer.endArray();
        writer.close();

        Files.writeString(new File("/tmp/accessors.faces").toPath(), sbw.toString());
    }

    @Ignore
    @Test
    public void testChildren122625849() throws Exception {
        DifaceContext context = new DifaceContext() {
            @Override
            public long version() {
                return 0;
            }

            @Override
            public Logger logger() {
                return Logger.getAnonymousLogger();
            }

            @Override
            public boolean debug() {
                return false;
            }

            @Override
            public double minClusterSize() {
                return 10;
            }

            @Override
            public double clusterThreshold() {
                return 0.85;
            }

            @Override
            public ProxySession session() {
                return null;
            }

            @Override
            public FaceStat stat() {
                return new FaceStat();
            }

            @Override
            public boolean cokemulator() {
                return false;
            }
        };

        DpThreshold similarThreshold =
            new DpThreshold(0.8, "Similar", 0);
        DpThreshold toddlerTh =
            new AgeThreshold(0.9, 3, "ToddlerNeigbour", 1);
        DpThreshold childTh =
            new AgeThreshold(0.87, 8, "ChildNeigbour", 2);
        DpThreshold neigbourTh =
            new DpThreshold(0.85,  "Neigbour", 3);

        JsonList facesList =
            TypesafeValueContentHandler.parse(
                new FileReader(
                    //Paths.getSandboxResourcesRoot() + "/98024548_1_0.faces",
                    //Paths.getSandboxResourcesRoot() + "/15560647.faces",
                    //Paths.getSandboxResourcesRoot() + "/436244418.faces",
                    //Paths.getSandboxResourcesRoot() + "/61028120.faces",
                    //Paths.getSandboxResourcesRoot() + "/69194959_1_1.faces",
                    Paths.getSandboxResourcesRoot() + "/69194959_1_1.only.faces",
                    //Paths.getSandboxResourcesRoot() + "/122625849.faces",
                    //Paths.getSandboxResourcesRoot() + "/26521462.faces",
                    StandardCharsets.UTF_8))
                .asList();

        List<Face> faces = new ArrayList<>(facesList.size());
        for (JsonObject jo: facesList) {
            faces.add(Face.parseFromBackend(jo));
        }


        //DbScanClusterizer clusterizer = new DbScanClusterizer(context, "baseid", 0);
        DbScanClusterizer clusterizer = new DbScanClusterizer(
            context,
            "baseid",
            0,
            Arrays.asList(toddlerTh, childTh, neigbourTh),
            similarThreshold,
            "bit");
        List<Cluster> clusters = clusterizer.clusterize(faces);
        System.err.println("Clusters created " + clusters.size() + " " + clusters);
        clusters = dropBySimilarity(context, clusters);
        System.err.println("Clusters after drops " + clusters.size() + " " + clusters);
    }

    @SuppressWarnings("CollectionIncompatibleType")
    private static List<Cluster> dropBySimilarity(final DifaceContext context, final List<Cluster> clusters) {
        int similarsDropped = 0;
        int similarsKept = 0;
        Map<String, Cluster> clustersMap =
            new LinkedHashMap<>(clusters.size() << 1);
        for (Cluster cluster : clusters) {
            clustersMap.put(cluster.id(), cluster);
        }

        Set<Cluster> removedClusters = Collections.emptySet();
        Iterator<Cluster> clusterIterator = clusters.iterator();
        while (clusterIterator.hasNext()) {
            Cluster cluster = clusterIterator.next();
            Map<String, AtomicInteger> map = new LinkedHashMap<>();
            for (Face face : cluster.faces()) {
                for (String clusterId : face.similarToClusters()) {
                    if (!clusterId.equals(cluster.id()) && !removedClusters.contains(clusterId)) {
                        map.computeIfAbsent(
                            clusterId,
                            (k) -> new AtomicInteger(0))
                            .incrementAndGet();
                    }
                }
            }

            if (!map.isEmpty()) {
                context.logger().info(
                    "For cluster " + cluster.id()
                        + " Similars are " + map.toString());
                for (Map.Entry<String, AtomicInteger> entry : map.entrySet()) {
                    Cluster otherCluster = clustersMap.get(entry.getKey());
                    if (entry.getValue().get() > 0.5 * cluster.faces().size()
                        && cluster.faces().size() < 150
                        && 5 * cluster.faces().size() < otherCluster.faces().size()) {
                        for (Face face : cluster.faces()) {
                            face.cluster(null);
                        }
                        clusterIterator.remove();
                        context.logger().info(
                            "Dropping cluster for similarity "
                                + cluster.id()
                                + " to " + otherCluster.id()
                                + " size " + cluster.faces().size()
                                + " " + cluster.faces().get(0).resourceId());
                        similarsDropped += 1;
                        break;
                    } else {
                        similarsKept += 1;
                    }
                }
            }
        }

        System.err.println("Similars dropped " + similarsDropped + " kept " + similarsKept);

        return clusters;
    }

    @Test
    public void testPicasa() throws Exception {
        List<String> regionNames = Arrays.asList(new String[]{"Кристина Погосян", "Тигран"});
        //List<String> regionTypes = Arrays.asList(new String[]{"Face", "Face"});
        List<Double> regionAreaX = Arrays.asList(new Double[]{0.649357, 0.507047});
        List<Double> regionAreaY = Arrays.asList(new Double[]{0.325368, 0.484069});
        List<Double> regionAreaW = Arrays.asList(new Double[]{0.230086, 0.188113});
        //List<Double> regionAreaH = Arrays.asList(new Double[]{0.281863, 0.31781});
        List<FaceMock> faces = Arrays.asList(
            new FaceMock("0.1366845727 0.2479258378 0.5739687443 0.2204093138"),
            new FaceMock("0.1226606369 0.2143671989 0.4311670303 0.4044202805"));

        double[][] diffs = new double[regionNames.size()][];
        for (int i = 0; i < regionNames.size(); i++) {
            diffs[i] = new double[faces.size()];
        }
        for (int i = 0; i < regionNames.size(); i++) {
            double c2x = regionAreaX.get(i);
            double c2y = regionAreaY.get(i);
            System.out.println("Picasa " + c2x + " " + c2y);

            for (int j = 0; j < faces.size(); j++) {
                FaceMock face = faces.get(j);
                double c1x = face.x() + 0.5 * face.w();
                double c1y = face.y() + 0.5 * face.h();

                System.out.println(c1x + " " + c1y);
                double diff = Math.sqrt((c1x - c2x) * (c1x - c2x) + (c1y - c2y) * (c1y - c2y));
                diffs[i][j] = diff;
            }

            System.out.println(Arrays.toString(diffs[i]));
        }

        int[] exifs = new int[regionNames.size()];
        Arrays.fill(exifs, -1);
        int[] models = new int[faces.size()];
        Arrays.fill(models, -1);
        while (true) {
            double min = Double.MAX_VALUE;
            int minI = -1;
            int minJ = -1;
            for (int i = 0; i < regionNames.size(); i++) {
                if (exifs[i] >= 0) {
                    continue;
                }
                for (int j = 0; j < faces.size(); j++) {
                    if (models[j] >= 0) {
                        continue;
                    }

                    System.out.println("Threshold1 " + 0.5 * regionAreaW.get(i));
                    System.out.println("Threshold2 " + 0.5 * faces.get(i).w());
                    if (diffs[i][j] < min
                        && diffs[i][j] < 0.5 * regionAreaW.get(i)
                        && diffs[i][j] < 0.5 * faces.get(i).w())
                    {
                        min = diffs[i][j];
                        minI = i;
                        minJ = j;
                    }
                }
            }

            if (minI < 0 || minJ < 0) {
                break;
            }

            exifs[minI] = minJ;
            models[minJ] = minI;
            faces.get(minJ).name(regionNames.get(minI));
        }

        for (int i = 0; i < regionNames.size(); i++) {
            if (exifs[i] < 0) {

                System.out.println("not found match for exif face " + i + " " + regionNames.get(i));
                System.out.println(Arrays.toString(diffs[i]));
                //context.stat().exifWithoutMatch(1);
            }
        }
    }

    private static final class FaceMock {
        private final double x;
        private final double y;
        private final double w;
        private final double h;

        public FaceMock(final double x, final double y, final double w, final double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public FaceMock(final String s) {
            String[] split = s.split(" ");
            w = Double.parseDouble(split[0]);
            h = Double.parseDouble(split[1]);
            x = Double.parseDouble(split[2]);
            y = Double.parseDouble(split[3]);
        }

        public double w() {
            return w;
        }

        public double h() {
            return h;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public void name(final String name) {
        }
    }
}
