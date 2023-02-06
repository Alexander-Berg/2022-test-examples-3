package ru.yandex.market.mbo.redis.categories;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.redis.common.model.RedisObject;
import ru.yandex.market.mbo.redis.common.service.RedisObjectType;
import ru.yandex.market.mbo.redis.common.service.RedisWriter;
import ru.yandex.market.mbo.redis.common.service.repo.RedisRepositoryMock;

import java.util.Optional;

@SuppressWarnings("checkstyle:magicnumber")
public class CategoryRedisWriterTest {

    private CategoryRedisWriter writer;
    private RedisRepositoryMock repo;
    private CategoryRedisDataConverter converter;

    @Before
    public void setUp() {
        repo = new RedisRepositoryMock();
        converter = new CategoryRedisDataConverter();
        writer = new CategoryRedisWriter(
            new RedisWriter<>(repo, RedisObjectType.CATEGORY_PROTO, new CategoryRedisDataConverter()));
    }

    @Test
    public void shouldStoreNewCategory() {
        int hid = 15;
        MboParameters.Category before = MboParameters.Category.newBuilder()
            .setHid(hid)
            .addName(MboParameters.Word.newBuilder()
                .setName("Test category")
                .build())
            .build();
        long ts = writer.putCategory(16, before);
        Assertions.assertThat(ts).isEqualTo(16);
        Assertions.assertThat(repo.get(RedisObjectType.CATEGORY_PROTO.getRedisPrefix(), String.valueOf(hid), converter))
            .isEqualTo(Optional.of(new RedisObject<>(16, before)));
    }


    @Test
    public void shouldOverrideOldTimestamp() {
        int hid = 15;
        int tsBefore = 16;
        MboParameters.Category before = MboParameters.Category.newBuilder()
            .setHid(hid)
            .addName(MboParameters.Word.newBuilder()
                .setName("Test category")
                .build())
            .build();
        writer.putCategory(tsBefore, before);

        MboParameters.Category newValue = MboParameters.Category.newBuilder()
            .setHid(hid)
            .addName(MboParameters.Word.newBuilder()
                .setName("Test category 2")
                .build())
            .build();
        long ts = writer.putCategory(tsBefore, newValue);
        Assertions.assertThat(ts).isEqualTo(tsBefore);
        Assertions.assertThat(repo.get(RedisObjectType.CATEGORY_PROTO.getRedisPrefix(), String.valueOf(hid), converter))
            .isEqualTo(Optional.of(new RedisObject<>(tsBefore, before)));

        int tsAfter = 17;
        ts = writer.putCategory(tsAfter, newValue);
        Assertions.assertThat(ts).isEqualTo(tsAfter);
        Assertions.assertThat(repo.get(RedisObjectType.CATEGORY_PROTO.getRedisPrefix(), String.valueOf(hid), converter))
            .isEqualTo(Optional.of(new RedisObject<>(tsAfter, newValue)));
    }

    @Test
    public void shouldRemoveCategory() {
        int hid = 15;
        MboParameters.Category before = MboParameters.Category.newBuilder()
            .setHid(hid)
            .addName(MboParameters.Word.newBuilder()
                .setName("Test category")
                .build())
            .build();
        writer.putCategory(16, before);
        Assertions.assertThat(repo.get(RedisObjectType.CATEGORY_PROTO.getRedisPrefix(), String.valueOf(hid), converter))
            .isEqualTo(Optional.of(new RedisObject<>(16, before)));
        writer.removeCategory(before.getHid());
        Assertions.assertThat(repo.get(RedisObjectType.CATEGORY_PROTO.getRedisPrefix(), String.valueOf(hid), converter))
            .isEqualTo(Optional.empty());
    }
}
