package ru.yandex.direct.core.entity.client.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.client.model.AgencyClientRelation;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class AgencyClientRelationServiceTestCanAgencyBindClient {
    private static final ClientId AGENCY1_ID = ClientId.fromLong(1000L);
    private static final ClientId AGENCY2_ID = ClientId.fromLong(1001L);

    private final ClientId agencyId;
    private final List<AgencyClientRelation> relations;
    private final boolean allowedToCreateCamps;
    private final boolean expectedResult;

    public AgencyClientRelationServiceTestCanAgencyBindClient(
            ClientId agencyId,
            List<AgencyClientRelation> relations,
            boolean allowedToCreateCamps,
            boolean expectedResult) {
        this.agencyId = agencyId;
        this.relations = relations;
        this.allowedToCreateCamps = allowedToCreateCamps;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters(name = "agencyId: {0}, relations: {1}, allowedToCreateCamps: {2}, expectedResult: {3}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{AGENCY1_ID, Collections.<AgencyClientRelation>emptyList(), false, true},
                new Object[]{AGENCY1_ID, Collections.<AgencyClientRelation>emptyList(), true, false},
                new Object[]{
                        AGENCY1_ID,
                        singletonList(new AgencyClientRelation().withAgencyClientId(AGENCY1_ID).withBinded(false)),
                        true,
                        false},
                new Object[]{
                        AGENCY1_ID,
                        singletonList(new AgencyClientRelation().withAgencyClientId(AGENCY1_ID).withBinded(false)),
                        false,
                        false},
                new Object[]{
                        AGENCY1_ID,
                        singletonList(new AgencyClientRelation().withAgencyClientId(AGENCY1_ID).withBinded(true)),
                        false,
                        true},
                new Object[]{
                        AGENCY1_ID,
                        singletonList(new AgencyClientRelation().withAgencyClientId(AGENCY1_ID).withBinded(true)),
                        true,
                        true},
                new Object[]{
                        AGENCY1_ID,
                        singletonList(new AgencyClientRelation().withAgencyClientId(AGENCY2_ID)
                                .withBinded(false)),
                        false,
                        true},
                new Object[]{
                        AGENCY1_ID,
                        singletonList(new AgencyClientRelation().withAgencyClientId(AGENCY2_ID)
                                .withBinded(false)),
                        true,
                        false},
                new Object[]{
                        AGENCY1_ID,
                        singletonList(new AgencyClientRelation().withAgencyClientId(AGENCY2_ID)
                                .withBinded(true)),
                        false,
                        false},
                new Object[]{
                        AGENCY1_ID,
                        singletonList(new AgencyClientRelation().withAgencyClientId(AGENCY2_ID)
                                .withBinded(true)),
                        true,
                        false});
    }


    @Test
    public void testCanAgencyBindClient() {
        assertThat(
                AgencyClientRelationService.canAgencyBindClient(agencyId, relations, allowedToCreateCamps),
                equalTo(expectedResult));
    }
}
