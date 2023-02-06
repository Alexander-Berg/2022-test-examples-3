package ru.yandex.market.pers.author;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.functional.Comparators;
import ru.yandex.market.pers.author.agitation.model.Agitation;
import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.cache.AgitationCache;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.author.agitation.model.Agitation.buildAgitationId;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 08.07.2020
 */
public class AbstractAgitationControllerTest extends PersAuthorTest {
    protected static final int PAGE_SIZE = 10;
    protected Random random = Mockito.spy(new Random());

    @Autowired
    private AgitationCache agitationCache;

    @BeforeEach
    public void prepareRandom() {
        agitationCache.changeRandomForTests(random);
        Mockito.reset(random);
        Mockito.when(random.nextInt(ArgumentMatchers.intThat(x -> x <= 0) + 1)).thenThrow(new IllegalArgumentException());
        Mockito.when(random.nextInt(ArgumentMatchers.intThat(x -> x > 0) + 1)).thenReturn(0);
    }

    protected void assertAgitations(Map<AgitationType, DtoPager<Agitation>> dataMap,
                                  AgitationType type,
                                  int count,
                                  String... entityIds) {
        assertAgitations(dataMap.get(type).getData(), type, entityIds);
        assertPager(dataMap, type, count);
    }

    protected void assertPager(Map<AgitationType, DtoPager<Agitation>> dataMap, AgitationType type, int count) {
        assertEquals(dataMap.get(type).getPager().getCount(), count);
    }

    protected void assertAgitations(List<Agitation> agitations, AgitationType type, String... entityIds) {
        assertEquals(List.of(entityIds), ListUtils.toList(agitations, Agitation::getEntityId));
        assertTrue(agitations.stream().allMatch(x -> x.getTypeEnum() == type));
        assertTrue(agitations.stream().allMatch(x -> x.getId().equals(buildAgitationId(type, x.getEntityId()))));
    }

    protected void assertAgitation(Agitation agitation, AgitationType type, String entityId) {
        assertEquals(type, agitation.getTypeEnum());
        assertEquals(entityId, agitation.getEntityId());
    }

    protected void assertAgitationIds(List<Agitation> agitations, String... agitationIds) {
        assertEquals(List.of(agitationIds), ListUtils.toList(agitations, Agitation::getId));
    }

    protected void assertAgitationIdsInAnyOrder(List<Agitation> agitations, String... agitationIds) {
        List<String> expected = new ArrayList<>(List.of(agitationIds));
        expected.sort(Comparators.STRING_ASC);
        List<String> actual = new ArrayList<>(ListUtils.toList(agitations, Agitation::getId));
        actual.sort(Comparators.STRING_ASC);
        assertEquals(actual, expected);
    }

    protected void disableAgitationCancel(AgitationUser user, String agitationId) {
        jdbcTemplate.update(
            "update pers.agitation_cancel " +
                "set end_time = now() - interval '1' day " +
                "where user_type = ? and user_id = ? and agitation_id = ?",
            user.getType().getValue(),
            user.getUserId(),
            agitationId
        );
    }

    protected void shiftCancelEndDateForAll(int time) {
        jdbcTemplate.update("update pers.agitation_cancel set end_time = end_time - interval '" + time + "' day");
    }

    protected void shiftCreateDateForAllDays(long days) {
        shiftCreateDateForAll(TimeUnit.DAYS.toHours(days));
    }

    protected void shiftCreateDateForAll(long hours) {
        jdbcTemplate.update("update pers.agitation_add set " +
            "end_time = end_time - interval '" + hours + "' hour, " +
            "cr_time = cr_time - interval '" + hours + "' hour");
    }
}
