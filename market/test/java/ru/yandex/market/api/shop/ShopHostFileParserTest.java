package ru.yandex.market.api.shop;

import it.unimi.dsi.fastutil.longs.LongCollection;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * Created by fettsery on 15.01.19.
 */
public class ShopHostFileParserTest extends UnitTestBase {

    @Test
    public void parseSingleEntry() {
        ShopsFileParser<ShopHostRepository> parser = new ShopsFileParser<>(new ShopHostRepositoryConsumer());

        ShopHostRepository shopHostRepository = parser.parse(ResourceHelpers.getResource("shops_single_entry.dat"));

        Assert.assertEquals(1, shopHostRepository.getAll().size());

        LongCollection ids = shopHostRepository.get("autotest-post-02.yandex.ru", 213);
        Assert.assertEquals(1, ids.size());
        Assert.assertEquals(10263886L, ids.iterator().nextLong());
    }

    @Test
    public void parseMultipleEntry() {
        ShopsFileParser<ShopHostRepository> parser = new ShopsFileParser<>(new ShopHostRepositoryConsumer());

        ShopHostRepository shopHostRepository = parser.parse(ResourceHelpers.getResource("shops_multiple_entries.dat"));

        Assert.assertEquals(4, shopHostRepository.getAll().size());

        LongCollection ids = shopHostRepository.get("test.ru");
        Assert.assertThat(ids, Matchers.containsInAnyOrder(10279208L, 10279209L));

        ids = shopHostRepository.get("test.ru", 969);
        Assert.assertThat(ids, Matchers.containsInAnyOrder(10279208L));

        ids = shopHostRepository.get("test.ru", 10981);
        Assert.assertThat(ids, Matchers.containsInAnyOrder(10279209L));

        ids = shopHostRepository.get("ёжик.в.тумане.рф");
        Assert.assertThat(ids, Matchers.containsInAnyOrder(11000821L));

        ids = shopHostRepository.get("братсктест.рф");
        Assert.assertThat(ids, Matchers.containsInAnyOrder(10279210L));
    }

    @Test
    public void parseWithoutEmptyHosts() {
        ShopsFileParser<ShopHostRepository> parser = new ShopsFileParser<>(new ShopHostRepositoryConsumer());

        ShopHostRepository shopHostRepository = parser.parse(ResourceHelpers.getResource("shops_with_empty_entries.dat"));

        Assert.assertEquals(2, shopHostRepository.getAll().size());

        LongCollection ids = shopHostRepository.get("test.ru");
        Assert.assertThat(ids, Matchers.containsInAnyOrder( 10279209L));

        ids = shopHostRepository.get("ёжик.в.тумане.рф");
        Assert.assertThat(ids, Matchers.containsInAnyOrder(11000821L));
    }

    @Test
    public void parseWithoutDuplicates() {
        ShopsFileParser<ShopHostRepository> parser = new ShopsFileParser<>(new ShopHostRepositoryConsumer());

        ShopHostRepository shopHostRepository = parser.parse(ResourceHelpers.getResource("shops_duplicate_entries.dat"));

        Assert.assertEquals(3, shopHostRepository.getAll().size());

        LongCollection ids = shopHostRepository.get("test.ru");
        Assert.assertThat(ids, Matchers.containsInAnyOrder( 10279209L));

        ids = shopHostRepository.get("ёжик.в.тумане.рф");
        Assert.assertThat(ids, Matchers.containsInAnyOrder(11000821L));

        ids = shopHostRepository.get("братсктест.рф");
        Assert.assertThat(ids, Matchers.containsInAnyOrder(10279210L));
    }
}
