package ru.yandex.direct.core.entity.auction.utils;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.auction.container.bs.Block;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordBidBsAuctionData;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordTrafaretData;
import ru.yandex.direct.core.entity.auction.container.bs.Position;
import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.bsauction.BsTrafaretClient.PLACE_TRAFARET_MAPPING;

public class BsAuctionConverterTest {

    private final Logger logger =
            LoggerFactory.getLogger(BsAuctionConverterTest.class);

    private Currency currency;
    private Keyword keyword;

    @Before
    public void setUp() {
        long keywordId = 1L;
        currency = CurrencyRub.getInstance();
        keyword = new Keyword().withId(keywordId);
    }

    @Test
    public void allPositions() {
        KeywordTrafaretData
                source = new KeywordTrafaretDataBuilder(keyword, currency)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM1), 170., 170.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM2), 160., 160.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM3), 150., 150.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM4), 140., 140.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE1), 130., 130.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE2), 120., 120.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE3), 110., 110.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE4), 100., 100.)
                .build();

        KeywordBidBsAuctionData
                expected = new KeywordBidBsAuctionDataBuilder(keyword, currency)
                .add(BsTrafaretClient.Place.PREMIUM1, 170., 170.)
                .add(BsTrafaretClient.Place.PREMIUM2, 160., 160.)
                .add(BsTrafaretClient.Place.PREMIUM3, 150., 150.)
                .add(BsTrafaretClient.Place.PREMIUM4, 140., 140.)
                .add(BsTrafaretClient.Place.GUARANTEE1, 130., 130.)
                .add(BsTrafaretClient.Place.GUARANTEE2, 120., 120.)
                .add(BsTrafaretClient.Place.GUARANTEE3, 110., 110.)
                .add(BsTrafaretClient.Place.GUARANTEE4, 100., 100.)
                .build();

        List<KeywordBidBsAuctionData> actualList =
                BsAuctionConverter.convertToPositionsAuctionData(singletonList(source), currency);

        Assertions.assertThat(actualList).first()
                .usingComparatorForType(new BlockComparator(), Block.class)
                .isEqualToComparingFieldByField(expected);
    }

    @Test
    public void convertToPositionsAuctionDataSingle_allPositions() {
        KeywordTrafaretData
                source = new KeywordTrafaretDataBuilder(keyword, currency)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM1), 170., 170.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM2), 160., 160.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM3), 150., 150.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM4), 140., 140.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE1), 130., 130.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE2), 120., 120.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE3), 110., 110.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE4), 100., 100.)
                .build();

        KeywordBidBsAuctionData
                expected = new KeywordBidBsAuctionDataBuilder(keyword, currency)
                .add(BsTrafaretClient.Place.PREMIUM1, 170., 170.)
                .add(BsTrafaretClient.Place.PREMIUM2, 160., 160.)
                .add(BsTrafaretClient.Place.PREMIUM3, 150., 150.)
                .add(BsTrafaretClient.Place.PREMIUM4, 140., 140.)
                .add(BsTrafaretClient.Place.GUARANTEE1, 130., 130.)
                .add(BsTrafaretClient.Place.GUARANTEE2, 120., 120.)
                .add(BsTrafaretClient.Place.GUARANTEE3, 110., 110.)
                .add(BsTrafaretClient.Place.GUARANTEE4, 100., 100.)
                .build();

        KeywordBidBsAuctionData actualList =
                BsAuctionConverter.convertToPositionsAuctionData(source, currency);

        Assertions.assertThat(actualList)
                .usingComparatorForType(new BlockComparator(), Block.class)
                .isEqualToComparingFieldByField(expected);
    }

    @Test
    public void twoPositionsUpper() {
        KeywordTrafaretData source = new KeywordTrafaretDataBuilder(keyword, currency)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM1) + 10000, 170., 170.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE1) + 10000, 130., 130.)
                .build();

        KeywordBidBsAuctionData expected = new KeywordBidBsAuctionDataBuilder(keyword, currency)
                .add(BsTrafaretClient.Place.PREMIUM1, 170., 170.)
                .add(BsTrafaretClient.Place.PREMIUM2, 170., 170.)
                .add(BsTrafaretClient.Place.PREMIUM3, 170., 170.)
                .add(BsTrafaretClient.Place.PREMIUM4, 170., 170.)
                .add(BsTrafaretClient.Place.GUARANTEE1, 130., 130.)
                .add(BsTrafaretClient.Place.GUARANTEE2, 130., 130.)
                .add(BsTrafaretClient.Place.GUARANTEE3, 130., 130.)
                .add(BsTrafaretClient.Place.GUARANTEE4, 130., 130.)
                .build();

        List<KeywordBidBsAuctionData> actualList =
                BsAuctionConverter.convertToPositionsAuctionData(singletonList(source), currency);

        Assertions.assertThat(actualList).first()
                .usingComparatorForType(new BlockComparator(), Block.class)
                .isEqualToComparingFieldByField(expected);
    }

    @Test
    public void twoPositionsLower() {
        KeywordTrafaretData source = new KeywordTrafaretDataBuilder(keyword, currency)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE4) - 10000, 130., 130.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM4) - 10000, 170., 170.)
                .build();

        KeywordBidBsAuctionData expected = new KeywordBidBsAuctionDataBuilder(keyword, currency)
                .add(BsTrafaretClient.Place.PREMIUM1, 25_000.1, 25_000.1)
                .add(BsTrafaretClient.Place.PREMIUM2, 25_000.1, 25_000.1)
                .add(BsTrafaretClient.Place.PREMIUM3, 25_000.1, 25_000.1)
                .add(BsTrafaretClient.Place.PREMIUM4, 25_000.1, 25_000.1)
                .add(BsTrafaretClient.Place.GUARANTEE1, 170., 170.)
                .add(BsTrafaretClient.Place.GUARANTEE2, 170., 170.)
                .add(BsTrafaretClient.Place.GUARANTEE3, 170., 170.)
                .add(BsTrafaretClient.Place.GUARANTEE4, 170., 170.)
                .build();


        List<KeywordBidBsAuctionData> actualList =
                BsAuctionConverter.convertToPositionsAuctionData(singletonList(source), currency);

        Assertions.assertThat(actualList).first()
                .usingComparatorForType(new BlockComparator(), Block.class)
                .isEqualToComparingFieldByField(expected);
    }

    @Test
    public void without100() {

        KeywordTrafaretData source = new KeywordTrafaretDataBuilder(keyword, currency)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM2), 160., 160.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM3), 150., 150.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM4), 140., 140.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE1), 130., 130.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE2), 120., 120.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE3), 110., 110.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE4), 100., 100.)
                .build();

        KeywordBidBsAuctionData expected = new KeywordBidBsAuctionDataBuilder(keyword, currency)
                .add(BsTrafaretClient.Place.PREMIUM1, 25_000.1, 25_000.1)
                .add(BsTrafaretClient.Place.PREMIUM2, 160., 160.)
                .add(BsTrafaretClient.Place.PREMIUM3, 150., 150.)
                .add(BsTrafaretClient.Place.PREMIUM4, 140., 140.)
                .add(BsTrafaretClient.Place.GUARANTEE1, 130., 130.)
                .add(BsTrafaretClient.Place.GUARANTEE2, 120., 120.)
                .add(BsTrafaretClient.Place.GUARANTEE3, 110., 110.)
                .add(BsTrafaretClient.Place.GUARANTEE4, 100., 100.)
                .build();

        List<KeywordBidBsAuctionData> actualList =
                BsAuctionConverter.convertToPositionsAuctionData(singletonList(source), currency);
        Assertions.assertThat(actualList).first()
                .usingComparatorForType(new BlockComparator(), Block.class)
                .isEqualToComparingFieldByField(expected);
    }

    @Test
    public void without75() {

        KeywordTrafaretData source = new KeywordTrafaretDataBuilder(keyword, currency)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM4), 140., 140.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE1), 130., 130.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE2), 120., 120.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE3), 110., 110.)
                .add(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.GUARANTEE4), 100., 100.)
                .build();

        KeywordBidBsAuctionData expected = new KeywordBidBsAuctionDataBuilder(keyword, currency)
                .add(BsTrafaretClient.Place.PREMIUM1, 25_000.1, 25_000.1)
                .add(BsTrafaretClient.Place.PREMIUM2, 25_000.1, 25_000.1)
                .add(BsTrafaretClient.Place.PREMIUM3, 25_000.1, 25_000.1)
                .add(BsTrafaretClient.Place.PREMIUM4, 140., 140.)
                .add(BsTrafaretClient.Place.GUARANTEE1, 130., 130.)
                .add(BsTrafaretClient.Place.GUARANTEE2, 120., 120.)
                .add(BsTrafaretClient.Place.GUARANTEE3, 110., 110.)
                .add(BsTrafaretClient.Place.GUARANTEE4, 100., 100.)
                .build();

        List<KeywordBidBsAuctionData> actualList =
                BsAuctionConverter.convertToPositionsAuctionData(singletonList(source), currency);
        Assertions.assertThat(actualList).first()
                .usingComparatorForType(new BlockComparator(), Block.class)
                .isEqualToComparingFieldByField(expected);
    }

    private Comparator<Position> positionComparator =
            (p1, p2) -> Comparator.comparing(Position::getBidPrice)
                    .thenComparing(Position::getAmnestyPrice).compare(p1, p2);

    private class BlockComparator implements Comparator<Block> {
        @Override
        public int compare(Block o1, Block o2) {
            Iterator<Position> a = o1.allPositions();
            Iterator<Position> b = o2.allPositions();
            while (a.hasNext() && b.hasNext()) {
                Position p1 = a.next();
                Position p2 = b.next();
                int res = positionComparator.compare(p1, p2);
                if (res != 0) {
                    logger.error("p1: bid={} price={}, p2: bid={} price={}",
                            p1.getBidPrice().bigDecimalValue(),
                            p1.getAmnestyPrice().bigDecimalValue(),
                            p2.getBidPrice().bigDecimalValue(),
                            p2.getAmnestyPrice().bigDecimalValue());
                    return res;
                }
            }
            if (a.hasNext() || b.hasNext()) {
                return -1;
            }
            return 0;
        }
    }

    private static class KeywordTrafaretDataBuilder {
        private List<TrafaretBidItem> trafaretData = new ArrayList<>();
        private final Keyword keyword;
        private final Currency currency;

        private KeywordTrafaretDataBuilder(Keyword keyword, Currency currency) {
            this.keyword = keyword;
            this.currency = currency;
        }

        public KeywordTrafaretDataBuilder add(long trafficVolume, Double bid, Double price) {
            trafaretData.add(new TrafaretBidItem().withBid(Money.valueOf(bid, currency.getCode()))
                    .withPrice(Money.valueOf(price, currency.getCode())).withPositionCtrCorrection(trafficVolume));
            return this;
        }

        public KeywordTrafaretData build() {
            return new KeywordTrafaretData().withBidItems(trafaretData).withKeyword(keyword);
        }
    }

    private static class KeywordBidBsAuctionDataBuilder {
        private Map<BsTrafaretClient.Place, Position> positionMap = new HashMap<>();
        private final Keyword keyword;
        private final Currency currency;


        private KeywordBidBsAuctionDataBuilder(Keyword keyword, Currency currency) {
            this.keyword = keyword;
            this.currency = currency;
        }

        public KeywordBidBsAuctionDataBuilder add(BsTrafaretClient.Place place, Double bid, Double price) {
            positionMap.put(place,
                    new Position(Money.valueOf(price, currency.getCode()), Money.valueOf(bid, currency.getCode())));
            return this;
        }

        public KeywordBidBsAuctionData build() {
            Block premium = new Block(
                    asList(positionMap.get(BsTrafaretClient.Place.PREMIUM1),
                            positionMap.get(BsTrafaretClient.Place.PREMIUM2),
                            positionMap.get(BsTrafaretClient.Place.PREMIUM3),
                            positionMap.get(BsTrafaretClient.Place.PREMIUM4)
                    )
            );
            Block guarantee = new Block(
                    asList(positionMap.get(BsTrafaretClient.Place.GUARANTEE1),
                            positionMap.get(BsTrafaretClient.Place.GUARANTEE2),
                            positionMap.get(BsTrafaretClient.Place.GUARANTEE3),
                            positionMap.get(BsTrafaretClient.Place.GUARANTEE4)
                    )
            );
            return new KeywordBidBsAuctionData().withKeyword(keyword)
                    .withPremium(premium)
                    .withGuarantee(guarantee);
        }
    }

}
