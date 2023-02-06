package ru.yandex.market.antifraud.orders.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.storage.dao.AntifraudDao;
import ru.yandex.market.antifraud.orders.storage.dao.PersonalRestrictionsDao;
import ru.yandex.market.antifraud.orders.storage.entity.restrictions.PersonalRestriction;
import ru.yandex.market.antifraud.orders.util.DateProvider;
import ru.yandex.market.antifraud.orders.web.dto.PersonalRestrictionsPojo;
import ru.yandex.market.antifraud.orders.web.dto.PersonalRestrictionsRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.PersonalRestrictionsUidsPojo;
import ru.yandex.market.antifraud.orders.web.dto.RestrictionsPojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PersonalRestrictionsServiceTest {

    @Mock
    private DateProvider dateProvider;
    @Mock
    private PersonalRestrictionsDao dao;
    @InjectMocks
    private PersonalRestrictionsService service;
    @Captor
    private ArgumentCaptor<Collection<PersonalRestriction>> captor;

    @Before
    public void setUp() throws Exception {
        when(dateProvider.nowLocalDate()).thenReturn(LocalDate.of(2022, 2, 9));
    }

    @Test
    public void saveRestriction() {
        List<PersonalRestrictionsPojo> restrictionsPojos =
                List.of(buildPojo("1", new AntifraudAction[]{AntifraudAction.PREPAID_ONLY}));
        var requestDto = buildRequest(restrictionsPojos);

        service.saveRestrictions(requestDto);

        verify(dao).saveRestrictions(captor.capture(), anyLong(), anyString());
        List<PersonalRestriction> restrictions = (List<PersonalRestriction>) captor.getValue();
        assertThat(restrictions.size()).isEqualTo(1);
        PersonalRestriction restriction = restrictions.get(0);
        assertThat(restriction.getValue()).isEqualTo(1L);
        assertThat(restriction.getAction()).isEqualTo(AntifraudAction.PREPAID_ONLY);
        assertThat(restriction.getExpiryAt()).isEqualTo(AntifraudDao.DEFAULT_EXPIRE_DATE.toLocalDate());
    }

    @Test
    public void saveRestrictionManyActions() {
        List<PersonalRestrictionsPojo> restrictionsPojos =
                List.of(buildPojo("1", new AntifraudAction[]{AntifraudAction.PREPAID_ONLY, AntifraudAction.ROBOCALL}));
        var requestDto = buildRequest(restrictionsPojos);

        service.saveRestrictions(requestDto);

        verify(dao).saveRestrictions(captor.capture(), anyLong(), anyString());
        List<PersonalRestriction> restrictions = (List<PersonalRestriction>) captor.getValue();
        assertThat(restrictions.size()).isEqualTo(2);
        List<Long> uids = restrictions.stream().map(PersonalRestriction::getValue).collect(Collectors.toList());
        List<AntifraudAction> actions =
                restrictions.stream().map(PersonalRestriction::getAction).collect(Collectors.toList());
        assertThat(uids).containsExactlyInAnyOrder(1L, 1L);
        assertThat(actions).containsOnly(AntifraudAction.PREPAID_ONLY, AntifraudAction.ROBOCALL);
    }

    @Test
    public void saveManyRestrictions() {
        List<PersonalRestrictionsPojo> restrictionsPojos = List.of(
                buildPojo("1", new AntifraudAction[]{AntifraudAction.PREPAID_ONLY}),
                buildPojo("2", new AntifraudAction[]{AntifraudAction.PREPAID_ONLY}));
        var requestDto = buildRequest(restrictionsPojos);

        service.saveRestrictions(requestDto);

        verify(dao).saveRestrictions(captor.capture(), anyLong(), anyString());
        List<PersonalRestriction> restrictions = (List<PersonalRestriction>) captor.getValue();
        assertThat(restrictions.size()).isEqualTo(2);
        List<Long> uids = restrictions.stream().map(PersonalRestriction::getValue).collect(Collectors.toList());
        List<AntifraudAction> actions =
                restrictions.stream().map(PersonalRestriction::getAction).collect(Collectors.toList());
        assertThat(uids).containsExactlyInAnyOrder(1L, 2L);
        assertThat(actions).containsExactlyInAnyOrder(AntifraudAction.PREPAID_ONLY, AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void saveManyRestrictionsManyActions() {
        List<PersonalRestrictionsPojo> restrictionsPojos = List.of(
                buildPojo("1", new AntifraudAction[]{AntifraudAction.PREPAID_ONLY, AntifraudAction.ROBOCALL}),
                buildPojo("2", new AntifraudAction[]{AntifraudAction.PREPAID_ONLY}));
        var requestDto = buildRequest(restrictionsPojos);

        service.saveRestrictions(requestDto);

        verify(dao).saveRestrictions(captor.capture(), anyLong(), anyString());
        List<PersonalRestriction> restrictions = (List<PersonalRestriction>) captor.getValue();
        assertThat(restrictions.size()).isEqualTo(3);
        List<Long> uids = restrictions.stream().map(PersonalRestriction::getValue).collect(Collectors.toList());
        List<AntifraudAction> actions =
                restrictions.stream().map(PersonalRestriction::getAction).collect(Collectors.toList());
        assertThat(uids).containsExactly(1L, 1L, 2L);
        assertThat(actions)
                .containsExactly(AntifraudAction.PREPAID_ONLY, AntifraudAction.ROBOCALL, AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void saveRestrictionForManyUids() {
        List<PersonalRestrictionsUidsPojo> restrictionsPojos =
                List.of(buildUidsPojo(List.of(1L, 2L), AntifraudAction.PREPAID_ONLY));
        var requestDto = buildRequest(restrictionsPojos);

        service.saveRestrictionsForManyUids(requestDto);

        verify(dao).saveRestrictions(captor.capture(), anyLong(), anyString());
        List<PersonalRestriction> restrictions = (List<PersonalRestriction>) captor.getValue();
        assertThat(restrictions.size()).isEqualTo(2);
        List<Long> uids = restrictions.stream().map(PersonalRestriction::getValue).collect(Collectors.toList());
        List<AntifraudAction> actions =
                restrictions.stream().map(PersonalRestriction::getAction).collect(Collectors.toList());
        assertThat(uids).containsExactly(1L, 2L);
        assertThat(actions)
                .containsExactly(AntifraudAction.PREPAID_ONLY, AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void saveManyRestrictionsForManyUids() {
        List<PersonalRestrictionsUidsPojo> restrictionsPojos = List.of(
                buildUidsPojo(List.of(1L, 2L), AntifraudAction.PREPAID_ONLY),
                buildUidsPojo(List.of(3L, 4L), AntifraudAction.ROBOCALL));
        var requestDto = buildRequest(restrictionsPojos);

        service.saveRestrictionsForManyUids(requestDto);

        verify(dao).saveRestrictions(captor.capture(), anyLong(), anyString());
        List<PersonalRestriction> restrictions = (List<PersonalRestriction>) captor.getValue();
        assertThat(restrictions.size()).isEqualTo(4);
        List<Long> uids = restrictions.stream().map(PersonalRestriction::getValue).collect(Collectors.toList());
        List<AntifraudAction> actions =
                restrictions.stream().map(PersonalRestriction::getAction).collect(Collectors.toList());
        assertThat(uids).containsExactly(1L, 2L, 3L, 4L);
        assertThat(actions)
                .containsExactly(AntifraudAction.PREPAID_ONLY, AntifraudAction.PREPAID_ONLY, AntifraudAction.ROBOCALL, AntifraudAction.ROBOCALL);
    }

    @Test
    public void getRestrictions() {
        Long uid = 1L;
        PersonalRestriction restriction =
                buildRestriction(uid, AntifraudAction.PREPAID_ONLY, LocalDate.of(2030, 1, 1));
        when(dao.getRestrictions(List.of(uid))).thenReturn(List.of(restriction));
        List<PersonalRestriction> restrictions = service.getRestrictions(List.of(uid));
        assertThat(restrictions.size()).isEqualTo(1);
    }

    @Test
    public void getNotExpiredRestrictions() {
        Long uid = 1L;
        PersonalRestriction restriction1 =
                buildRestriction(uid, AntifraudAction.PREPAID_ONLY, LocalDate.of(2030, 1, 1));
        PersonalRestriction restriction2 =
                buildRestriction(uid, AntifraudAction.ROBOCALL, LocalDate.of(2022, 1, 1));
        when(dao.getRestrictions(List.of(uid))).thenReturn(List.of(restriction1, restriction2));

        List<PersonalRestriction> restrictions = service.getRestrictions(List.of(uid));

        assertThat(restrictions.size()).isEqualTo(1);
        assertThat(restrictions.get(0).getAction()).isEqualTo(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void deleteRestrictions() {
        List<PersonalRestrictionsPojo> restrictionsPojos = List.of(PersonalRestrictionsPojo.builder().uid("1").build());
        var requestDto = buildRequest(restrictionsPojos);
        service.deleteRestrictions(requestDto);
        verify(dao).deleteRestrictions(restrictionsPojos, 1L, "Test reason");
    }

    private <T extends RestrictionsPojo> PersonalRestrictionsRequestDto<T> buildRequest(List<T> restrictions) {
        return PersonalRestrictionsRequestDto.<T>builder()
                .restrictions(restrictions)
                .authorUid(1L)
                .reason("Test reason")
                .build();
    }

    private PersonalRestrictionsPojo buildPojo(String uid, AntifraudAction[] actions) {
        return PersonalRestrictionsPojo.builder()
                .uid(uid)
                .actions(actions)
                .build();
    }

    private PersonalRestrictionsUidsPojo buildUidsPojo(List<Long> uids, AntifraudAction action) {
        return PersonalRestrictionsUidsPojo.builder()
                .uids(uids)
                .action(action)
                .build();
    }

    private PersonalRestriction buildRestriction(Long uid, AntifraudAction action, LocalDate expiryAt) {
        return PersonalRestriction.builder()
                .value(uid)
                .action(action)
                .expiryAt(expiryAt)
                .build();
    }
}
