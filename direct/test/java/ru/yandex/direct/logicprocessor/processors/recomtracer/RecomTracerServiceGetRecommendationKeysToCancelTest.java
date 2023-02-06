package ru.yandex.direct.logicprocessor.processors.recomtracer;

import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.recommendation.RecommendationType;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationKey;
import ru.yandex.direct.core.entity.recomtracer.repository.RecomTracerRepository;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecomTracerLogicObject;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecommendationKeyIdentifier;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.core.entity.recommendation.repository.GridRecommendationYtRepository;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;
import ru.yandex.direct.logicprocessor.processors.recomtracer.cancellers.RecommendationCanceller;
import ru.yandex.direct.logicprocessor.processors.recomtracer.utils.RecommendationKeyUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;

class RecomTracerServiceGetRecommendationKeysToCancelTest {

    @Mock
    private GridRecommendationYtRepository recommendationYtRepository;
    @Mock
    private RecomTracerRepository recomTracerRepository;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Тест проверяет, что если для префикса ключа, потенциального для отмены, не нашлось активной рекомендации,
     * то он не будет обработан. Если для префикса ключа нашлись активные рекомендации - они все будут обработаны
     * <p>
     * В тесте замокан вызов {@link GridRecommendationYtRepository#getRecommendationsByKeyPrefixes(Set, Integer)}
     */
    @Test
    void testGetRecommendationKeysToCancel_NotActiveRecommendation() {
        long clientId = 1L;
        long cidActive = 2L;
        long cidNotActive = 3L;

        long pidActive1 = 7L;
        long pidActive2 = 8L;

        int shard = 0;
        RecommendationCanceller testRecommendationCanceller = new AlwaysCancelRecomTracerCanceller();
        RecomTracerService recomTracerService = new RecomTracerService(recomTracerRepository,
                recommendationYtRepository, singletonList(testRecommendationCanceller));

        Long type = testRecommendationCanceller.supportedType().getId();

        RecomTracerLogicObject recomTracerLogicObjectActive = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(type)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CLIENT_ID, clientId)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cidActive)
                .build();

        RecomTracerLogicObject recomTracerLogicObjectNotActive = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(type)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CLIENT_ID, clientId)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cidNotActive)
                .build();

        /* Mock recommendationYtRepository.getRecommendationsByKeyPrefixes method */

        RecommendationKey recommendationPrefixActive =
                new RecommendationKey().withType(type).withClientId(clientId).withCampaignId(cidActive);
        RecommendationKey recommendationPrefixNotActive =
                new RecommendationKey().withType(type).withClientId(clientId).withCampaignId(cidNotActive);

        /* Рекоммендации, актичные для ключа recommendationPrefixActive */
        GdiRecommendation activeRecommendation1 =
                createGdiRecommendation(testRecommendationCanceller.supportedType(), clientId, cidActive, pidActive1);
        GdiRecommendation activeRecommendation2 =
                createGdiRecommendation(testRecommendationCanceller.supportedType(), clientId, cidActive, pidActive2);

        Set<GdiRecommendation> activeRecommendations = ImmutableSet.of(
                activeRecommendation1,
                activeRecommendation2
        );

        when(recommendationYtRepository.getRecommendationsByKeyPrefixes(ImmutableSet.of(recommendationPrefixActive,
                recommendationPrefixNotActive), shard))
                .thenReturn(activeRecommendations);

        /* mock ended */

        Set<RecommendationKey> expectedRecommendationsToCancel = ImmutableSet.of(
                RecommendationKeyUtils.getRecommendationKeyFromGdiRecommendation(activeRecommendation1),
                RecommendationKeyUtils.getRecommendationKeyFromGdiRecommendation(activeRecommendation2)
        );

        Set<RecommendationKey> gotRecommendationsToCancel = recomTracerService.getRecommendationKeysToCancel(shard,
                ImmutableSet.of(recomTracerLogicObjectActive, recomTracerLogicObjectNotActive));

        assertThat(gotRecommendationsToCancel).hasSize(2);
        assertThat(gotRecommendationsToCancel).isEqualTo(expectedRecommendationsToCancel);
    }

    /**
     * Тест проверяет, что если для префикса ключа нашлись активные рекомендации, но пользовательский обработчик
     * вернул для них - не отменять, то они не будут отменены. Если обработчик вернул отменять - отменятся.
     */
    @Test
    void testGetRecommendationKeysToCancel_NotCancelledRecommendation() {
        long clientId = 1L;
        long cid = 2L;
        long pid = 3L;
        int shard = 0;
        RecommendationCanceller alwaysCancelCanceller = new AlwaysCancelRecomTracerCanceller();
        RecommendationCanceller neverCancelCanceller = new NeverCancelRecomTracerCanceller();

        RecomTracerService recomTracerService = new RecomTracerService(recomTracerRepository,
                recommendationYtRepository, ImmutableList.of(alwaysCancelCanceller, neverCancelCanceller));
        Long typeCancel = alwaysCancelCanceller.supportedType().getId();
        Long typeNotCancel = neverCancelCanceller.supportedType().getId();

        RecomTracerLogicObject recomTracerLogicObjectCancelled = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(typeCancel)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CLIENT_ID, clientId)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cid)
                .build();
        RecomTracerLogicObject recomTracerLogicObjectNotCancelled = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(typeNotCancel)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CLIENT_ID, clientId)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cid)
                .build();

        /* Mock recommendationYtRepository.getRecommendationsByKeyPrefixes method */

        RecommendationKey recommendationPrefixCancel =
                new RecommendationKey().withType(typeCancel).withClientId(clientId).withCampaignId(cid);

        RecommendationKey recommendationPrefixNotCancel =
                new RecommendationKey().withType(typeNotCancel).withClientId(clientId).withCampaignId(cid);

        GdiRecommendation activeRecommendationCancel = createGdiRecommendation(alwaysCancelCanceller.supportedType(),
                clientId, cid, pid);
        GdiRecommendation activeRecommendationNotCancel =
                createGdiRecommendation(neverCancelCanceller.supportedType(), clientId, cid, pid);

        Set<GdiRecommendation> activeRecommendations = ImmutableSet.of(
                activeRecommendationCancel,
                activeRecommendationNotCancel
        );
        when(recommendationYtRepository.getRecommendationsByKeyPrefixes(ImmutableSet.of(recommendationPrefixCancel,
                recommendationPrefixNotCancel), shard))
                .thenReturn(activeRecommendations);

        /* mock ended */

        Set<RecommendationKey> expectedRecommendationsToCancel = ImmutableSet.of(
                RecommendationKeyUtils.getRecommendationKeyFromGdiRecommendation(activeRecommendationCancel)
        );
        Set<RecommendationKey> gotRecommendationsToCancel = recomTracerService.getRecommendationKeysToCancel(shard,
                ImmutableSet.of(recomTracerLogicObjectCancelled, recomTracerLogicObjectNotCancelled));
        assertThat(gotRecommendationsToCancel).hasSize(1);
        assertThat(gotRecommendationsToCancel).isEqualTo(expectedRecommendationsToCancel);
    }

    /**
     * Тест проверяет, что если в объекте были AdditionalColumns - то они передадутся с правильным ключом в обработчик
     */
    @Test
    void testGetRecommendationKeysToCancel_RecommendationCheckAdditionalColumns() {
        long clientId = 1L;
        long cid1 = 2L;
        long cid2 = 3L;

        int shard = 0;
        RecommendationCanceller checkAdditionalColumnsCanceller = new RecomTracerCancellerCheckAdditionalColumns();
        RecomTracerService recomTracerService = new RecomTracerService(recomTracerRepository,
                recommendationYtRepository, ImmutableList.of(checkAdditionalColumnsCanceller));

        Long recommendationType = checkAdditionalColumnsCanceller.supportedType().getId();

        /* В объекты добавляются additional column - cid.
         * Когда рекомендация передается для отмены пользовательскому обработчкину, cid из additional columns должен
         * быть равен cid'у из ключа, что значит - для ключа передались правильные допольнительные колонки
         */
        RecomTracerLogicObject recomTracerLogicObject1 = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(recommendationType)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CLIENT_ID, clientId)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cid1)
                .addAdditionalColumn(CAMPAIGNS.CID, cid1)
                .build();

        RecomTracerLogicObject recomTracerLogicObject2 = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(recommendationType)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CLIENT_ID, clientId)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cid2)
                .addAdditionalColumn(CAMPAIGNS.CID, cid2)
                .build();


        /* Mock recommendationYtRepository.getRecommendationsByKeyPrefixes method */

        RecommendationKey recommendationPrefix1 =
                new RecommendationKey().withType(recommendationType).withClientId(clientId).withCampaignId(cid1);
        RecommendationKey recommendationPrefix2 =
                new RecommendationKey().withType(recommendationType).withClientId(clientId).withCampaignId(cid2);

        GdiRecommendation recommendationCancel1 =
                createGdiRecommendation(checkAdditionalColumnsCanceller.supportedType(),
                        clientId, cid1, 0);

        GdiRecommendation recommendationCancel2 =
                createGdiRecommendation(checkAdditionalColumnsCanceller.supportedType(),
                        clientId, cid2, 0);

        Set<GdiRecommendation> activeRecommendations = ImmutableSet.of(
                recommendationCancel1,
                recommendationCancel2
        );

        when(recommendationYtRepository.getRecommendationsByKeyPrefixes(ImmutableSet.of(recommendationPrefix1,
                recommendationPrefix2), shard))
                .thenReturn(activeRecommendations);

        /* mock ended */

        Set<RecommendationKey> expectedRecommendationsToCancel = ImmutableSet.of(
                RecommendationKeyUtils.getRecommendationKeyFromGdiRecommendation(recommendationCancel1),
                RecommendationKeyUtils.getRecommendationKeyFromGdiRecommendation(recommendationCancel2)
        );

        Set<RecommendationKey> gotRecommendationsToCancel = recomTracerService.getRecommendationKeysToCancel(shard,
                ImmutableSet.of(recomTracerLogicObject1, recomTracerLogicObject2));

        assertThat(gotRecommendationsToCancel).hasSize(2);
        assertThat(gotRecommendationsToCancel).isEqualTo(expectedRecommendationsToCancel);
    }

    private GdiRecommendation createGdiRecommendation(RecommendationType type, long clientId, long cid, long pid) {
        return new GdiRecommendation()
                .withClientId(clientId)
                .withType(GdiRecommendationType.fromType(type))
                .withCid(cid)
                .withPid(pid)
                .withBid(0L)
                .withUserKey1("")
                .withUserKey2("")
                .withUserKey3("")
                .withTimestamp(0L);
    }

    /**
     * Класс, который всегда отменят полученную рекомендацию
     */
    private class AlwaysCancelRecomTracerCanceller implements RecommendationCanceller {
        @Override
        public RecommendationType supportedType() {
            return RecommendationType.fromId(1L);
        }

        @Override
        public boolean recommendationsToCancel(RecommendationKeyWithAdditionalColumns recommendationKeyWithAdditionalColumns) {
            return true;
        }
    }

    /**
     * Класс, который никогда не отменят полученную рекомендацию
     */
    private class NeverCancelRecomTracerCanceller implements RecommendationCanceller {
        @Override
        public RecommendationType supportedType() {
            return RecommendationType.fromId(2L);
        }

        @Override
        public boolean recommendationsToCancel(RecommendationKeyWithAdditionalColumns recommendationKeyWithAdditionalColumns) {
            return false;
        }
    }

    /**
     * Класс, отменяющий рекомендацию, если полученный в ней cid из additionalColumns равен cid'у из ключа рекомендации
     */
    private class RecomTracerCancellerCheckAdditionalColumns implements RecommendationCanceller {
        @Override
        public RecommendationType supportedType() {
            return RecommendationType.fromId(1L);
        }

        @Override
        public boolean recommendationsToCancel(RecommendationKeyWithAdditionalColumns recommendationKeyWithAdditionalColumns) {
            return recommendationKeyWithAdditionalColumns.getAdditionalColumns().get(CAMPAIGNS.CID).
                    equals(recommendationKeyWithAdditionalColumns.getRecommendationKey().getCampaignId());
        }
    }
}


