package ru.yandex.market.stat.dicts.parsers;

import org.junit.Test;
import ru.yandex.market.stat.dicts.records.ShopPriceLabsRecord;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 28.08.17.
 */
public class ShopPriceLabsParserTest {

    @Test
    public void testShopPriceLabs() throws IOException, ParseException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Given
        DictionaryParser<ShopPriceLabsRecord> parser = new ShopPriceLabsRecord.ShopPriceLabsParser();
        // When
        List<ShopPriceLabsRecord> records = loadRecords(parser, "/parsers/shop_832.xml");
        // Then
        assertThat(records.size(), equalTo(3));
        assertThat(records.get(0), equalTo(ShopPriceLabsRecord
            .builder()
            .pl_shop_id(333L)
            .campaign_id(1000508453L)
            .domain("pltest15.yandex.ru")
            .created_at(new Timestamp(1503891002000L))
            .has_default_strategy(false)
            .min_price_card(null)
            .has_filter_based_strategy(false)
            .do_card_bids(false)
            .do_search_bids(false)
            .do_fee_bids(false)
            .use_price_monitoring(true)
            .use_analytic_system(true)
            .has_reserve_strategies(false)
            .is_minimal_bids(-1)
            .active_strategies_count(0)
            .last_strategy_update(Timestamp.from(sdf.parse("2016-09-29 17:34:47").toInstant()))
            .has_position_strategy(false)
            .is_pl_active(true)
            .build()
        ));
    }

    @Test
    public void testShopPriceLabsEmpty() throws IOException, ParseException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Given
        DictionaryParser<ShopPriceLabsRecord> parser = new ShopPriceLabsRecord.ShopPriceLabsParser();
        // When
        List<ShopPriceLabsRecord> records = loadRecords(parser, "/parsers/shop_832_empty.xml");
        // Then
        assertThat(records.size(), equalTo(0));

    }
}
