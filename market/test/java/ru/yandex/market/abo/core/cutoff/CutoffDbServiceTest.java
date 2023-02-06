package ru.yandex.market.abo.core.cutoff;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.core.abo.AboCutoff;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.abo.CloseAboCutoffRequest;
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CutoffDbServiceTest extends EmptyTest {
    private long shopId = 774L;

    @Autowired
    private CutoffDbService cutoffDbService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        shopId = TestHelper.generateIntId();
    }

    @Test
    public void testOpenCloseOpen() {
        // open
        AboCutoff cutoffType = AboCutoff.CPC_QUALITY;
        Cutoff cutoff = generateCpaCutoff(shopId, cutoffType, null);
        Cutoff openedCutoff = openCutoff(cutoff);
        assertNotNull(openedCutoff);
        Long firstCutoffId = openedCutoff.getId();
        assertNull(openedCutoff.getTurnOnTime());

        assertNotNull(cutoffDbService.cutoffCreationTime(cutoffType, Collections.singletonList(shopId)).get(shopId));

        // close
        cutoff.setClosed(CutoffActionStatus.OK);
        var request = CloseAboCutoffRequest.builder().cutoffType(cutoff.getAboCutoffType()).build();
        cutoffDbService.closeLastAboCutoff(shopId, request);
        Cutoff closed = findCutoffByShopAndType(cutoff.getShopId(), cutoff.getAboCutoffType());
        assertEquals(CutoffActionStatus.OK, closed.getClosed());
        assertNotNull(closed.getTurnOnTime());

        // при вставке turn_on_time = current_timestamp
        // сдвинем назад, чтобы выполнялось turn_on_time < current_timestamp
        pgJdbcTemplate.update("UPDATE cpa_cutoff SET turn_on_time = turn_on_time - INTERVAL '1 hour' WHERE id = ?",
                closed.getId());

        Date crTime = cutoffDbService.cutoffCreationTime(cutoffType, Collections.singletonList(shopId)).get(shopId);
        assertNull(crTime);

        openNewAndCheck(cutoff, firstCutoffId);
    }

    @Test
    public void testOpenExpireOpen() throws InterruptedException {
        // open
        AboCutoff cutoffType = AboCutoff.CPC_QUALITY;
        Cutoff cutoff = generateCpaCutoff(shopId, cutoffType, DateUtils.addSeconds(new Date(), 1));
        Cutoff openedCutoff = openCutoff(cutoff);
        Long firstCutoffId = openedCutoff.getId();

        assertNotNull(openedCutoff);
        assertTrue(openedCutoff.getTurnOnTime().after(new Date()));
        assertNotNull(cutoffDbService.cutoffCreationTime(cutoffType, Collections.singletonList(shopId)).get(shopId));

        // wait for cutoff to expire
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        assertTrue(openedCutoff.getTurnOnTime().before(new Date()));

        openNewAndCheck(cutoff, firstCutoffId);
    }

    private void openNewAndCheck(Cutoff cutoff, Long firstCutoffId) {
        cutoff.setClosed(null);
        cutoff.setTurnOnTime(null);
        cutoff.setId(null);
        cutoff.setOpened(CutoffActionStatus.OK);
        Cutoff openedCutoff = openCutoff(cutoff);
        assertNotNull(openedCutoff);
        assertNotEquals(openedCutoff.getId(), firstCutoffId);
    }

    @Test
    public void testOpenCutoff() {
        Cutoff cutoff = generateCpaCutoff(shopId);
        assertNotNull(openCutoff(cutoff));
    }

    @Test
    public void testCloseCutoff() {
        Cutoff cutoff = generateCpaCutoff(shopId);

        assertNotNull(openCutoff(cutoff));

        var request = CloseAboCutoffRequest.builder().cutoffType(cutoff.getAboCutoffType()).build();
        cutoffDbService.closeLastAboCutoff(shopId, request);
        assertEquals(CutoffActionStatus.OK, findCutoffByShopAndType(cutoff.getShopId(), cutoff.getAboCutoffType()).getClosed());
    }

    @Test
    public void openAboCutoff() {
        var cutoff = generateCpaCutoff(shopId);
        cutoff.setAboCutoffType(AboCutoff.CPC_QUALITY);

        Date now = new Date();
        assertTrue(cutoffDbService.shopsWithActualAboCutoff(AboCutoff.CPC_QUALITY, now).isEmpty());
        openCutoff(cutoff);
        assertFalse(cutoffDbService.shopsWithActualAboCutoff(AboCutoff.CPC_QUALITY, now).isEmpty());
    }

    private Cutoff openCutoff(Cutoff cutoff) {
        var request = OpenAboCutoffRequest.builder()
                .cutoffType(cutoff.getAboCutoffType())
                .cutoffPeriod(cutoff.getTurnOnTime())
                .build();
        cutoffDbService.openCutoff(cutoff.getShopId(), request, cutoff.getReason());
        return findCutoffByShopAndType(cutoff.getShopId(), cutoff.getAboCutoffType());
    }

    private Cutoff findCutoffByShopAndType(long shopId, AboCutoff type) {
        return cutoffDbService.loadLast(shopId, type);
    }

    private static Cutoff generateCpaCutoff(long shopId, AboCutoff type, Date turnOnTime) {
        Cutoff cutoff = generateCpaCutoff(shopId);
        cutoff.setAboCutoffType(type);
        cutoff.setTurnOnTime(turnOnTime);
        return cutoff;
    }

    private static Cutoff generateCpaCutoff(long shopId) {
        Cutoff result = new Cutoff();

        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrow = calendar.getTime();

        result.setShopId(shopId);
        result.setCrTime(today);
        result.setModifTime(today);
        result.setTurnOnTime(tomorrow);
        result.setType(CutoffType.byId(RND.nextInt(CutoffType.values().length)));
        result.setAboCutoffType(AboCutoff.values()[RND.nextInt(AboCutoff.values().length)]);
        result.setUid((long) RND.nextInt(10000));
        result.setNeedTesting(RND.nextBoolean());
        result.setTid(RND.nextInt(10000));
        result.setInfo(LONG_TEXT);
        result.setComment(null);
        result.setOpened(CutoffActionStatus.OK);
        result.setReason(CutoffReason.values()[RND.nextInt(CutoffReason.values().length)]);
        // ставим ERROR и это значет, что отключение еще не закрыто
        result.setClosed(CutoffActionStatus.ERROR);
        result.setClosedUid((long) RND.nextInt(10000));
        result.setClosedTid(RND.nextInt(10000));
        result.setClosedInfo("s");
        result.setClosedReason(CutoffReason.values()[RND.nextInt(CutoffReason.values().length)]);
        result.setTurnOnTime(DateUtils.addDays(new Date(), 1));
        return result;
    }

    private static final String LONG_TEXT = "<request-date>02.09.2016 17:10:31</request-date>\n" +
            " <cart-diffs>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>съемник-чашка масляного фильтра JTC JTC-4611</title>\n" +
            "   <price>646</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>светодидный фонарик Camelion LED 5102-5</title>\n" +
            "   <price>345</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>сверло Bosch STANDARD 2608585863</title>\n" +
            "   <price>619</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>дрель Metabo be 751 600581810</title>\n" +
            "   <price>14399</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>алюминиевый рельс для рельсовой системы крепления крюков ESSE HR 48L</title>\n" +
            "   <price>1890</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>садовый светильник Camelion 4505 10537</title>\n" +
            "   <price>1166</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>сушилка для рук Electrolux EHDA/N - 2500</title>\n" +
            "   <price>9690</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>термос NOVA TOUR Сильвер 500</title>\n" +
            "   <price>890</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>универсальный шлифовальный диск Makita D-27034</title>\n" +
            "   <price>283</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>сушилка для рук Electrolux EHDA/N - 2500</title>\n" +
            "   <price>9690</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>термос NOVA TOUR Сильвер 500</title>\n" +
            "   <price>890</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            "  <cart-diff>\n" +
            "   <reason>стоимость доставки, передаваемая в ответе API, отличается от стоимости доставки, передаваемой в прайс-листе или указанной в Партнёрском интерфейсе</reason>\n" +
            "   <title>универсальный шлифовальный диск Makita D-27034</title>\n" +
            "   <price>283</price>\n" +
            "   <currency>RUR</currency>\n" +
            "   <region>Москва</region>\n" +
            "  </cart-diff>\n" +
            " </cart-diffs>\n" +
            "</abo-info>\n" +
            "\n";
}
