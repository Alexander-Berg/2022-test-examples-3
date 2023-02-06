package ru.yandex.market.antifraud.orders.yt;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.mockito.Mockito;

import ru.yandex.market.antifraud.orders.config.YtClientConfig;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.storage.dao.ConfigurationDao;
import ru.yandex.market.antifraud.orders.storage.dao.yt.CrmPlatformDao;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.yt.ytclient.proxy.YtClient;

/**
 * Есть предположение, что этот код ещё пригодится для исследований, поэтому он отключён, а не удалён
 *
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 17.10.2019
 */
@Log4j2
public class YtClientPerformanceTestClass {
    private final CrmPlatformDao crmPlatformDao;

    public YtClientPerformanceTestClass(CrmPlatformDao crmPlatformDao) {
        this.crmPlatformDao = crmPlatformDao;
    }

    public static void main(String[] args) throws InvalidProtocolBufferException, InterruptedException {
        YtClient ytClient = new YtClientConfig().ytClient(
            new String[]{"seneca-sas"},
            "robot-mrkt-antfrd",
            "");
        YtClientPerformanceTestClass ytClientPerformanceTestClass =
            new YtClientPerformanceTestClass(new CrmPlatformDao(ytClient, new ConfigurationService(Mockito.mock(ConfigurationDao.class))));
        ytClientPerformanceTestClass.load();
//        ytClientPerformanceTestClass.testGetCrmOrdersForIds();
    }

    public void testGetCrmOrdersForIds(){
        List<Order> orders = crmPlatformDao.getCrmOrdersForIds(
                List.of(
                        MarketUserId.fromUid(311870044L),
                        MarketUserId.fromUuid("799328191547550679")
                        ),
                LocalDate.of(2019, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
        orders.forEach(System.out::println);
    }

    public void load() throws InterruptedException, InvalidProtocolBufferException {
        // ---------------
        String[] ids = new String[]{"433348306", "480518583", "265396087", "458397987", "422776819", "458401605",
                "506719899", "426305734", "453922836", "212702026", "440207504", "453922259", "438590929", "229381953"
                , "136980367", "510273667", "494956271", "20440412", "446868889", "576978534", "592998178",
                "461182014", "441859819", "348747937", "431733580", "492499751", "429704710", "923654771", "459864297"
                , "420889755", "40413619", "74887312", "425281170", "443059085", "483949340", "416652745", "727320273"
                , "494359711", "834268803", "862219021", "453827652", "726394757", "438322447", "486780399",
                "510295528", "436299895", "304902804", "438610037", "531082688", "505914998", "734928379", "483343897"
                , "463868571", "458427790", "488873356", "393386093", "330724451", "473841507", "293955897",
                "479264153", "847593415", "418902032", "127753653", "802535568", "834497717", "671404750", "758033101"
                , "444560692", "463082608", "438039607", "589986944", "697339790", "466223870", "16836430",
                "429720880", "779468655", "505586570", "463345266", "501278343", "501274226", "749053113", "122471583"
                , "469143405", "1130000023609003", "834265468", "439907367", "463767662", "719343105", "155060794",
                "411770436", "456117495", "730977502", "886862004", "403128691", "803467500", "56432063", "331027279"
                , "275561782", "413164105", "166826313", "455242238"};
        int tries = 1;
        double fullTime = 0.0;
        double maxTime = -100_000_000.0d;
        double minTime = Double.MAX_VALUE;
        ArrayList<Double> values = new ArrayList<>();
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < tries; i++) {
            for (String id : ids) {
                stopWatch.reset();
                stopWatch.start();
                var crmOrders = crmPlatformDao.getCrmOrdersForUid(Long.parseLong(id));

                stopWatch.stop();
                double time = stopWatch.getTime(TimeUnit.MILLISECONDS);
                values.add(time);
                fullTime += time;
                maxTime = Double.max(maxTime, time);
                minTime = Double.min(minTime, time);

                log.info("Request time: {} ms", time);

                for (var crmOrder : crmOrders) {
                    log.info(crmOrder.getId());
                }
            }
        }
        Collections.sort(values);

        double percentile0 = 0.5;
        double percentile1 = 0.9;
        double percentile2 = 0.95;
        double percentile3 = 0.99;
        log.info("Average time: {} ms", String.format("%f", fullTime / (tries * ids.length)));
        log.info("{} percentile time: {} ms", percentile0,
                String.format("%f", values.get((int) Math.round(values.size() * percentile0))));
        log.info("{} percentile time: {} ms", percentile1,
                String.format("%f", values.get((int) Math.round(values.size() * percentile1))));
        log.info("{} percentile time: {} ms", percentile2,
                String.format("%f", values.get((int) Math.round(values.size() * percentile2))));
        log.info("{} percentile time: {} ms", percentile3,
                String.format("%f", values.get((int) Math.round(values.size() * percentile3))));
        log.info("Max time: {} s", String.format("%f", maxTime));
        log.info("Min time: {} s", String.format("%f", minTime));
    }
}
