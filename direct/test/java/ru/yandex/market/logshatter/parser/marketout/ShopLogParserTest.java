package ru.yandex.market.logshatter.parser.marketout;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author kukabara
 */
public class ShopLogParserTest {
    @Test
    public void testParse() throws Exception {
        System.out.println(new Date(1453342000 * 1000L));
        LogParserChecker checker = new LogParserChecker(new ShopLogParser());
        checker.check("53332176550635673915001\t1\t\t\t1453342000\thttp://tms12.ru/catalog/mixer/for_washbasin/hansgrohe_talis_smesitel_dlya_rakoviny_khrom_71710000/?r1=yandext&r2=&ymclid=53332176550635673915001" +
                "\tHansgrohe Talis Ñìåñèòåëü äëÿ ðàêîâèíû, õðîì 71710000\t\t\t2625771051453315229\t89.222.164.124\t\t870\t8383\ttms12.ru:Ñìåñèòåëè\\Ñìåñèòåëè äëÿ ðàêîâèíû\tTMS\t\t\t\t" +
                "6\t\t100\t153623\t502\t1\t12576053\t\t\t12\t20160120_2300\t16.1.1.1\t100\t1\t\t53332176550635673915001\tmsh01e.market.yandex.net\t\t30278102\t\t517762881\t\t\t\t-1\t\t\t6zGSdxKoss-Cig0dkWiBLA\t9c7d3882fcf59b2d75c444a9d690fb14\t\t15093,0,84\t1453330859611512-9016470002576284828198928-sas1-5651\t\t10748\t\t\t\t\t0\t\t0\t225\t56379\t0.6629309545051834\t\t1\t100\t\t53332176550635673915\t0.003099\t0\t0\t0\t\tMNA_Microcard_201507_W_Adjustment_0034\t0\t0",
            new Date(1453342000 * 1000L), 6
        );
        checker.check("53332176550635673915002\t1\t\t\t1453332176\thttp://www.santehnica.ru/catalog/12297-176413.html?_openstat=bWFya2V0LnlhbmRleC5ydTvQodC80LXRgdC40YLQtdC70LggSGFuc2dyb2hlIEhhbnNUYWxpcyBFIDcxNzEwMDAwO2gxak11UURkclY2NmpMUU16NXV3aEE7&frommarket=https%3A//market.yandex.ru/product/12576053%3Fhid%3D91610%26nid%3D56379%26text%3D71710000%26srnum%3D13&utm_source=market.yandex.ru&utm_term=176413&ymclid=53332176550635673915002\tÑìåñèòåëè Hansgrohe HansTalis E 71710000\t\t\t2625771051453315229\t89.222.164.124\t\t870\t8950\tsantehnica.ru:Êàòàëîã òîâàðîâ\\Ñìåñèòåëè\\Hansgrohe\\Talis E\tSantehnica.ru\t\t\t\t6\t\t39\t57452\t502\t0\t12576053\t\t\t12\t20160120_2300\t16.1.1.1\t100\t1\t\t53332176550635673915002\tmsh01e.market.yandex.net\t\t30278102\t\t517762881\t\t\t\t-1\t\t\th1jMuQDdrV66jLQMz5uwhA\t9c7d3882fcf59b2d75c444a9d690fb14\t\t15093,0,84\t1453330859611512-9016470002576284828198928-sas1-5651\t\t10748\t\t\t\t\t0\t\t0\t225\t56379\t0.6629309545051834\t\t2\t100\t\t53332176550635673915\t0.003734\t0\t0\t0\t\tMNA_Microcard_201507_W_Adjustment_0034\t0\t0");
        checker.check("53332176550635673915003\t1\t\t\t1453332176\thttp://santehnika-nonstop.ru/catalog/smesiteli-dlya-vannoy/hansgrohe-talis-e-71710000/?_openstat=bWFya2V0LnlhbmRleC5ydTvQodC80LXRgdC40YLQtdC70YwgSGFuc2dyb2hlIFRhbGlzIEUgNzE3MTAwMDAg0YEg0LTQvtC90L3Ri9C8INC60LvQsNC_0LDQvdC-0Lwg0LTQu9GPINGA0LDQutC-0LLQuNC90Ys7bXIyZ25HNDFvUG5fZzhHd0xTNXdnZzs&frommarket=https%3A//market.yandex.ru/product/12576053%3Fhid%3D91610%26nid%3D56379%26text%3D71710000%26srnum%3D13&r1=yandext&r2=&ymclid=53332176550635673915003\tÑìåñèòåëü Hansgrohe Talis E 71710000 ñ äîííûì êëàïàíîì äëÿ ðàêîâèíû\t\t\t2625771051453315229\t89.222.164.124\t\t870\t8650\tsantehnika-nonstop.ru:Ñìåñèòåëè\\Ñìåñèòåëè äëÿ âàííîé\tÑàíòåõíèêà-Nonstop\t\t\t\t6\t\t38\t242296\t502\t1\t12576053\t\t\t12\t20160120_2300\t16.1.1.1\t38\t1\t\t53332176550635673915003\tmsh01e.market.yandex.net\t\t30278102\t\t517762881\t\t\t\t-1\t\t\tmr2gnG41oPn_g8GwLS5wgg\t9c7d3882fcf59b2d75c444a9d690fb14\t\t15093,0,84\t1453330859611512-9016470002576284828198928-sas1-5651\t\t10748\t\t\t\t\t0\t\t0\t225\t56379\t0.6629309545051834\t\t3\t38\t\t53332176550635673915\t0.002963\t0\t0\t0\t\tMNA_Microcard_201507_W_Adjustment_0034\t0\t0");
        checker.check("62952838027716289592001\t0\t\t\t1462952838\thttp://www.pleer.ru/_171689_samsung_sm_a500_galaxy_a5.html?frommarket=&ymclid=62952838027716289592001\t������� ������� Samsung SM-A500F/DS Galaxy A5 Duos Black\t\t\t\tlocalipaddr\t\t444\t16948\tPleer.ru:��������, ����� ����(�����) � ��������, VoIP, ����������...\\������� �������\tPleer.ru\t\t\t\t-1\t\t10\t720\t\t1\t12323116\t\tfeed_offer_ids:\"1480-171689\"\t\t20160511_0738\t16.2.7.3\t10\t1\t\t62952838027716289592001\tmsh22d.market.yandex.net\t\t76539472\t\t3346153099\t\t\t\t-1\t10\tb956d5f0d5c4dopaliha666 0\t5MsM6uPwmDu7vghuqYa9QA\t\t1\t\t\t\t213\t\t\t\t\t0\t\t0\t225\t54726\t\t\t1\t\t\t62952838027716289592\t1\t0\t0\t0\t\t0\t0\t0");
    }
}