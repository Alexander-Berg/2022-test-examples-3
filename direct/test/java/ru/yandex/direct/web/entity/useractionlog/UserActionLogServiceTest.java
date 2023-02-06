package ru.yandex.direct.web.entity.useractionlog;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.useractionlog.AdGroupId;
import ru.yandex.direct.useractionlog.CampaignId;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.reader.FilterLogRecordsByCampaignTypeBuilder;
import ru.yandex.direct.useractionlog.reader.UserActionLogReader;
import ru.yandex.direct.useractionlog.schema.AdId;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

@ParametersAreNonnullByDefault
@RunWith(MockitoJUnitRunner.class)
public class UserActionLogServiceTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    @Mock
    private UserActionLogReader userActionLogReader;
    @Mock
    private ShardHelper shardHelper;
    @Mock
    private ObjectPathResolverService objectPathResolverService;
    @Mock
    private PpcRbac ppcRbac;
    @Mock
    private RbacService rbacService;
    @Mock
    private DirectWebAuthenticationSource authenticationSource;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CampaignTypeSourceCache campaignTypeSourceCache;
    @Mock
    private FilterLogRecordsByCampaignTypeBuilder filterLogRecordsByCampaignTypeBuilder;
    @Mock
    private ClientService clientService;
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    /**
     * Преобразование идентификаторов в коллекцию {@link ObjectPath} в соответствии с DIRECT-78030.
     * <p>
     * Копипаста из тикета:
     * <p>
     * <blockquote>
     * Как должно быть:<br>
     * все фильтры через И:<br>
     * №кампании, №группы и №объявления, "Кто изменил", Настройки и Статусы<br>
     * <p>
     * Сценарии, как должно быть:<br>
     * Допустим есть<br>
     * кампания 1, в ней группа 1, в ней объявление 1<br>
     * кампания 2, в ней группа 2, в ней объявление 2<br>
     * <ol>
     * <li>Если в фильтре написано  объявление 1, 2, кампания 1 , то должно показаться только объявление 1.
     * <li>Если в фильтре  кампания 1 , то должны показаться изменения по кампании 1, группе 1 и объявлению 1.
     * <li>Если в фильтре  кампания 1, группа 2 , то ничего не должно показаться.
     * </ol>
     * </blockquote>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void filterIds() {
        Map<Long, ObjectPath.AdGroupPath> possibleAdGroupPaths = Stream.of(
                new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(10), new AdGroupId(100)),
                new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(10), new AdGroupId(101)),
                new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(11), new AdGroupId(110)),
                new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(11), new AdGroupId(111))
        ).collect(Collectors.toMap(p -> p.getId().toLong(), Function.identity()));

        Map<Long, ObjectPath.AdPath> possibleAdPaths = Stream.of(
                new ObjectPath.AdPath(new ClientId(1), new CampaignId(10), new AdGroupId(100), new AdId(1000)),
                new ObjectPath.AdPath(new ClientId(1), new CampaignId(10), new AdGroupId(100), new AdId(1001)),
                new ObjectPath.AdPath(new ClientId(1), new CampaignId(10), new AdGroupId(101), new AdId(1010)),
                new ObjectPath.AdPath(new ClientId(1), new CampaignId(10), new AdGroupId(101), new AdId(1011)),
                new ObjectPath.AdPath(new ClientId(1), new CampaignId(11), new AdGroupId(110), new AdId(1100)),
                new ObjectPath.AdPath(new ClientId(1), new CampaignId(11), new AdGroupId(110), new AdId(1101)),
                new ObjectPath.AdPath(new ClientId(1), new CampaignId(11), new AdGroupId(111), new AdId(1110)),
                new ObjectPath.AdPath(new ClientId(1), new CampaignId(11), new AdGroupId(111), new AdId(1111))
        ).collect(Collectors.toMap(p -> p.getId().toLong(), Function.identity()));

        Mockito.when(objectPathResolverService.resolve(Mockito.anyCollection(), Mockito.anyCollection()))
                .then(invocation -> Stream.concat(
                        ((Collection<Long>) invocation.getArgument(0)).stream()
                                .map(possibleAdGroupPaths::get)
                                .filter(Objects::nonNull),
                        ((Collection<Long>) invocation.getArgument(1)).stream()
                                .map(possibleAdPaths::get)
                                .filter(Objects::nonNull)
                ).collect(Collectors.toList()));

        UserActionLogService userActionLogService = new UserActionLogService(userActionLogReader,
                shardHelper, objectPathResolverService, ppcRbac, rbacService, authenticationSource,
                filterLogRecordsByCampaignTypeBuilder, campaignTypeSourceCache, clientService,
                ppcPropertiesSupport);

        softly.assertThat(
                userActionLogService.filterIds(
                        1L, ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of()))
                .describedAs("Just client id. Note that no checking of client existence performs.")
                .containsExactly(
                        new ObjectPath.ClientPath(new ClientId(1)));

        softly.assertThat(
                userActionLogService.filterIds(
                        1L, ImmutableSet.of(10L), ImmutableSet.of(), ImmutableSet.of()))
                .describedAs("Just one campaign id")
                .containsExactly(
                        new ObjectPath.CampaignPath(new ClientId(1), new CampaignId(10)));

        softly.assertThat(
                userActionLogService.filterIds(
                        1L, ImmutableSet.of(10L, 20L), ImmutableSet.of(), ImmutableSet.of()))
                .describedAs("Two campaign ids. Note that no checking of campaign existence performs.")
                .containsExactly(
                        new ObjectPath.CampaignPath(new ClientId(1), new CampaignId(10)),
                        new ObjectPath.CampaignPath(new ClientId(1), new CampaignId(20)));

        softly.assertThat(
                userActionLogService.filterIds(
                        1L, ImmutableSet.of(), ImmutableSet.of(100L, 999L), ImmutableSet.of()))
                .describedAs("One existent adgroup id and one not existent, without campaign id")
                .containsExactly(
                        new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(10), new AdGroupId(100)));

        softly.assertThat(
                userActionLogService.filterIds(
                        1L, ImmutableSet.of(10L), ImmutableSet.of(100L, 999L), ImmutableSet.of()))
                .describedAs("One existent adgroup id and one not existent, with campaign id")
                .containsExactly(
                        new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(10), new AdGroupId(100)));

        softly.assertThat(
                userActionLogService.filterIds(
                        1L, ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(1000L, 9999L)))
                .describedAs("One existent ad id and one not existent, without campaign and adgroup id")
                .containsExactly(
                        new ObjectPath.AdPath(new ClientId(1), new CampaignId(10), new AdGroupId(100), new AdId(1000)));

        softly.assertThat(
                userActionLogService.filterIds(
                        1L, ImmutableSet.of(10L), ImmutableSet.of(), ImmutableSet.of(1000L, 9999L)))
                .describedAs("One existent ad id and one not existent, with campaign id and without adgroup id")
                .containsExactly(
                        new ObjectPath.AdPath(new ClientId(1), new CampaignId(10), new AdGroupId(100), new AdId(1000)));

        softly.assertThat(
                userActionLogService.filterIds(
                        1L, ImmutableSet.of(10L), ImmutableSet.of(100L), ImmutableSet.of(1000L, 9999L)))
                .describedAs("One existent ad id and one not existent, with campaign and adgroup id")
                .containsExactly(
                        new ObjectPath.AdPath(new ClientId(1), new CampaignId(10), new AdGroupId(100), new AdId(1000)));

        softly.assertThat(
                userActionLogService.filterIds(1L,
                        ImmutableSet.of(10L, 11L),
                        ImmutableSet.of(100L, 101L, 110L, 111L),
                        ImmutableSet.of(1000L, 1111L)))
                .describedAs("When adIds specified, only AdPaths may be emitted,"
                        + " even when all other ids fits specified client")
                .containsExactly(
                        new ObjectPath.AdPath(new ClientId(1), new CampaignId(10), new AdGroupId(100), new AdId(1000)),
                        new ObjectPath.AdPath(new ClientId(1), new CampaignId(11), new AdGroupId(111), new AdId(1111)));

        softly.assertThat(
                userActionLogService.filterIds(1L,
                        ImmutableSet.of(10L, 11L),
                        ImmutableSet.of(100L),
                        ImmutableSet.of()))
                .describedAs("When adGroupIds specified and adIds not specified, only AdGroupPaths may be emitted,"
                        + " even when all other ids fits specified client")
                .containsExactly(
                        new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(10), new AdGroupId(100)));

        softly.assertThat(
                userActionLogService.filterIds(1L,
                        ImmutableSet.of(99L),
                        ImmutableSet.of(100L),
                        ImmutableSet.of()))
                .describedAs("If campaign id does not exist but adgroup id exists, no result expected")
                .isEmpty();

        softly.assertThat(
                userActionLogService.filterIds(1L,
                        ImmutableSet.of(99L),
                        ImmutableSet.of(100L),
                        ImmutableSet.of(1000L)))
                .describedAs("If campaign id does not exist but ad id exists, no result expected")
                .isEmpty();

        softly.assertThat(
                userActionLogService.filterIds(1L,
                        ImmutableSet.of(10L),
                        ImmutableSet.of(999L),
                        ImmutableSet.of(1000L)))
                .describedAs("If adgroup id does not exist but ad id exists, no result expected")
                .isEmpty();
    }
}
