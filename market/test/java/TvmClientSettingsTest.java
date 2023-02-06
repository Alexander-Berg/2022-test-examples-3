package ru.yandex.market.starter.tvm;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import ru.yandex.market.starter.integration.tvm.TvmConfigurerAdapter;
import ru.yandex.market.starter.tvm.factory.TvmClientSettings;
import ru.yandex.market.starter.tvmblackbox.BlackboxInfo;
import ru.yandex.passport.tvmauth.BlackboxEnv;

public class TvmClientSettingsTest {

    @Test
    public void selfTvmIdTest() {
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setTvmId(1)
            .build();
        Assertions.assertEquals(1, tvmClientSettings.getSelfTvmId());
    }

    @Test
    public void selfTvmId_Overrides_LocalTvmId_Test() {
        final TvmClientSettings tvmClientSettingsLocalEnabled = TvmClientSettings.builder()
            .setTvmId(1)
            .setUseDefaultLocalTvmId(true)
            .build();
        Assertions.assertEquals(1, tvmClientSettingsLocalEnabled.getSelfTvmId());
    }

    @Test
    public void secretTest() {
        final String secret = "secret";
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setTvmSecret(secret)
            .build();
        Assertions.assertEquals(secret, tvmClientSettings.getTvmSecret());
    }

    @Test
    public void fallbackSecretTest() {
        final String secret = "secret";
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setFallbackTvmSecret(secret)
            .build();
        Assertions.assertEquals(secret, tvmClientSettings.getTvmSecret());
    }

    @Test
    public void secret_And_FallbackSecret_Test() {
        final String secret = "secret";
        final String fallbackSecret = "fallbackSecret";
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setTvmSecret(secret)
            .setFallbackTvmSecret(fallbackSecret)
            .build();
        Assertions.assertEquals(secret, tvmClientSettings.getTvmSecret());
    }

    @Test
    public void localTvmTest() {
        final TvmClientSettings tvmClientSettingsLocalDisabled = TvmClientSettings.builder()
            .build();
        Assertions.assertNull(tvmClientSettingsLocalDisabled.getSelfTvmId());
        Assertions.assertNull(tvmClientSettingsLocalDisabled.getTvmSecret());

        final String localSecret = "localSecret";
        final TvmClientSettings tvmClientSettingsLocalEnabled = TvmClientSettings.builder()
            .setUseDefaultLocalTvmId(true)
            .setLocalTvmSecretSupplier(() -> localSecret)
            .build();
        Assertions.assertEquals(TvmClientSettings.LOCAL_TVM_ID, tvmClientSettingsLocalEnabled.getSelfTvmId());
        Assertions.assertEquals(localSecret, tvmClientSettingsLocalEnabled.getTvmSecret());

        // Manually specified secret overrides fetched local tvm secret
        // (need to specify secret locally and not to go every time in yav)
        final String secret = "secret";
        final TvmClientSettings tvmClientSettingsLocalEnabledWithSecret = TvmClientSettings.builder()
            .setUseDefaultLocalTvmId(true)
            .setTvmSecret(secret)
            .setLocalTvmSecretSupplier(() -> localSecret)
            .build();
        Assertions.assertEquals(TvmClientSettings.LOCAL_TVM_ID, tvmClientSettingsLocalEnabledWithSecret.getSelfTvmId());
        Assertions.assertEquals(secret, tvmClientSettingsLocalEnabledWithSecret.getTvmSecret());
    }

    @Test
    public void blackBoxEnvTest() {
        final BlackboxEnv blackboxEnv = BlackboxEnv.PROD;
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setBlackboxEnv("PROD")
            .build();
        Assertions.assertEquals(blackboxEnv, tvmClientSettings.getBlackboxEnv());

        final BlackboxEnv blackboxEnvForInfo = BlackboxEnv.TEST;
        final BlackboxInfo blackboxInfo = new BlackboxInfo(blackboxEnvForInfo.name(), 1);

        // Info overrides manually specified blackboxEnv, because it get env right from the blackbox client
        final TvmClientSettings tvmClientSettingsBlackBoxInfo = TvmClientSettings.builder()
            .setBlackboxEnv("TEST")
            .setBlackboxInfo(blackboxInfo)
            .build();
        Assertions.assertEquals(blackboxEnvForInfo, tvmClientSettingsBlackBoxInfo.getBlackboxEnv());
    }

    @Test
    public void sourcesTest() {
        final HashSet<Integer> sources = new HashSet<>();
        sources.add(1);
        sources.add(2);
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setSources(sources)
            .build();
        Assertions.assertEquals(Set.of(1, 2), tvmClientSettings.getSources());
    }

    @Test
    public void additionalSourcesFromConfigurerTest() {
        final HashSet<Integer> sources = new HashSet<>();
        sources.add(1);
        sources.add(2);
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setSources(sources)
            .setTvmConfigurers(Collections.singletonList(new TvmConfigurerAdapter() {
                @Override
                public Set<Integer> getSources() {
                    final HashSet<Integer> sources = new HashSet<>();
                    sources.add(3);
                    sources.add(4);
                    return sources;
                }
            }))
            .build();
        Assertions.assertEquals(Set.of(1, 2, 3, 4), tvmClientSettings.getSources());
    }

    @Test
    public void destinationsTest() {
        final HashSet<Integer> destinations = new HashSet<>();
        destinations.add(1);
        destinations.add(2);
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setTvmSecret("testSecret")
            .setDestinations(destinations)
            .build();
        Assertions.assertArrayEquals(new int[]{1, 2}, tvmClientSettings.getDestinations());
    }

    @Test
    public void destinationsWithoutSecretTest() {
        final HashSet<Integer> destinations = new HashSet<>();
        destinations.add(1);
        destinations.add(2);
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setDestinations(destinations)
            .build();
        Assertions.assertNull(tvmClientSettings.getDestinations());
    }

    @Test
    public void additionalDestinationsFromConfigurerTest() {
        final HashSet<Integer> destinations = new HashSet<>();
        destinations.add(1);
        destinations.add(2);
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setTvmSecret("testSecret")
            .setDestinations(destinations)
            .setTvmConfigurers(Collections.singletonList(new TvmConfigurerAdapter() {
                @Override
                public Set<Integer> getDestinations() {
                    final HashSet<Integer> destinations = new HashSet<>();
                    destinations.add(3);
                    destinations.add(4);
                    return destinations;
                }
            }))
            .build();
        Assertions.assertArrayEquals(new int[]{1, 2, 3, 4}, tvmClientSettings.getDestinations());
    }

    @Test
    public void multipleConfigurersTest() {
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setTvmSecret("testSecret")
            .setTvmConfigurers(List.of(
                    new TvmConfigurerAdapter() {
                        @Override
                        public Set<Integer> getSources() {
                            final HashSet<Integer> sources = new HashSet<>();
                            sources.add(1);
                            sources.add(2);
                            return sources;
                        }

                        @Override
                        public Set<Integer> getDestinations() {
                            final HashSet<Integer> destinations = new HashSet<>();
                            destinations.add(5);
                            destinations.add(6);
                            return destinations;
                        }

                        @Override
                        public Set<AntPathRequestMatcher> getUserTicketProtectedPaths() {
                            final HashSet<AntPathRequestMatcher> userTicketProtectedPaths = new HashSet<>();
                            userTicketProtectedPaths.add(
                                new AntPathRequestMatcher("/test1", HttpMethod.GET.name())
                            );
                            return userTicketProtectedPaths;
                        }
                    },
                    new TvmConfigurerAdapter() {
                        @Override
                        public Set<Integer> getSources() {
                            final HashSet<Integer> sources = new HashSet<>();
                            sources.add(3);
                            sources.add(4);
                            return sources;
                        }

                        @Override
                        public Set<Integer> getDestinations() {
                            final HashSet<Integer> destinations = new HashSet<>();
                            destinations.add(7);
                            destinations.add(8);
                            return destinations;
                        }

                        @Override
                        public Set<AntPathRequestMatcher> getUserTicketProtectedPaths() {
                            final HashSet<AntPathRequestMatcher> userTicketProtectedPaths = new HashSet<>();
                            userTicketProtectedPaths.add(
                                new AntPathRequestMatcher("/test2", HttpMethod.POST.name())
                            );
                            return userTicketProtectedPaths;
                        }
                    }
                )
            )
            .build();

        Assertions.assertEquals(Set.of(1, 2, 3, 4), tvmClientSettings.getSources());
        Assertions.assertArrayEquals(new int[]{5, 6, 7, 8}, tvmClientSettings.getDestinations());
        Assertions.assertEquals(
            Set.of(
                new AntPathRequestMatcher("/test1", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/test2", HttpMethod.POST.name())
            ),
            tvmClientSettings.getUserTicketProtectedPaths()
        );
    }

    @Test
    public void userTicketProtectedPathsTest() {
        final TvmClientSettings tvmClientSettings = TvmClientSettings.builder()
            .setTvmSecret("testSecret")
            .setTvmConfigurers(List.of(
                    new TvmConfigurerAdapter() {
                        @Override
                        public Set<AntPathRequestMatcher> getUserTicketProtectedPaths() {
                            final HashSet<AntPathRequestMatcher> userTicketProtectedPaths = new HashSet<>();
                            userTicketProtectedPaths.add(
                                new AntPathRequestMatcher("/test1", HttpMethod.GET.name())
                            );
                            return userTicketProtectedPaths;
                        }
                    }
                )
            )
            .build();

        Assertions.assertEquals(
            Set.of(new AntPathRequestMatcher("/test1", HttpMethod.GET.name())),
            tvmClientSettings.getUserTicketProtectedPaths()
        );
    }
}
