package ru.yandex.market.crm.platform.reader.yt;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.crm.environment.Environment;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.crm.environment.Environment.PRODUCTION;
import static ru.yandex.market.crm.platform.config.raw.StorageType.HDD;
import static ru.yandex.market.crm.platform.config.raw.StorageType.SSD;
import static ru.yandex.market.crm.platform.reader.yt.TableDescription.Medium.DEFAULT;
import static ru.yandex.market.crm.platform.reader.yt.TableDescription.Medium.SSD_BLOBS;

/**
 * @author vtarasoff
 * @since 09.11.2020
 */
public class MediumResolverTest {
    @Test
    public void shouldResolveSsdInProductionByDefault() {
        assertThat(resolverIn(PRODUCTION).resolve(SSD), is(SSD_BLOBS));
    }

    @Test
    public void shouldResolveDefaultInNotProductionByDefault() {
        Arrays.stream(Environment.values())
                .filter(env -> !env.equals(PRODUCTION))
                .forEach(env -> assertThat(resolverIn(env).resolve(SSD), is(DEFAULT)));
    }

    @Test
    public void shouldResolveDefaultIfHddStorage() {
        assertThat(resolverIn(PRODUCTION).resolve(HDD), is(DEFAULT));
    }

    @Test
    public void shouldResolveSsdInProductionIfSsdStorage() {
        assertThat(resolverIn(PRODUCTION).resolve(SSD), is(SSD_BLOBS));
    }

    @Test
    public void shouldResolveDefaultInNotProductionIfSsdStorage() {
        Arrays.stream(Environment.values())
                .filter(env -> !env.equals(PRODUCTION))
                .forEach(env -> assertThat(resolverIn(env).resolve(SSD), is(DEFAULT)));
    }

    private MediumResolver resolverIn(Environment env) {
        return new MediumResolver(() -> env);
    }
}
