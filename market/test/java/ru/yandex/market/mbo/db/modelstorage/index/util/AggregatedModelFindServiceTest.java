package ru.yandex.market.mbo.db.modelstorage.index.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.mbo.db.modelstorage.index.FilterHelper;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.ModelIndexPayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 29.08.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class AggregatedModelFindServiceTest {

    private AggregatedModelFindService service;

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    private StorageFacadeStub storageFacade;

    @Before
    public void setUp() {
        storageFacade = Mockito.spy(new StorageFacadeStub());
        service = new AggregatedModelFindService(storageFacade);
    }

    @Test
    public void findAll() {
        List<Model> models = Arrays.asList(
            model(0, true),
            model(1, false),
            model(2, true,
                model(21, true),
                model(22, false)
            ),
            model(3, false,
                model(31, true),
                model(32, false)
            ),
            model(4, true,
                model(41, false)
            ),
            model(5, false,
                model(51, true)
            )
        );
        storageFacade.setModels(models);

        Set<Long> all = service.collectMatchedAll(MboIndexesFilter::new,
            Collections.emptyList()
        );

        assertThat(all).containsExactlyInAnyOrder(0L, 2L, 21L, 4L);
    }

    @Test
    public void findAny() {
        List<Model> models = Arrays.asList(
            model(0, true),
            model(1, false),
            model(2, true,
                model(21, true),
                model(22, false)
            ),
            model(3, false,
                model(31, true),
                model(32, false)
            ),
            model(4, true,
                model(41, false)
            ),
            model(5, false,
                model(51, true)
            )
        );
        storageFacade.setModels(models);

        Set<Long> all = service.collectMatchedAny(MboIndexesFilter::new,
            parentModelsWithModifications(models),
            Collections.emptyList()
        );

        assertThat(all).containsExactlyInAnyOrder(0L, 2L, 21L, 22L, 31L, 4L, 41L, 51L);
    }

    @Test
    public void testInputFilter() {
        List<Model> models = Arrays.asList(
            model(0, true),
            model(1, false)
        );
        storageFacade.setModels(models);

        List<MboIndexesFilter> filters = Arrays.asList(
            FilterHelper.createDoesntHaveParameterValue(100),
            FilterHelper.createHasParameterValue(101)
        );
        Set<Long> all = service.collectMatchedAny(MboIndexesFilter::new,
            parentModelsWithModifications(models),
            filters
        );

        ArgumentCaptor<MboIndexesFilter> captor = ArgumentCaptor.forClass(MboIndexesFilter.class);
        Mockito.verify(storageFacade, times(1)).processQueryModels(captor.capture(), Mockito.any());

        System.out.println(MboIndexesFilter.newFilter().merge(filters.get(0)).merge(filters.get(1)));
        Assertions.assertThat(captor.getValue()).isEqualTo(
            MboIndexesFilter.newFilter().merge(filters.get(0)).merge(filters.get(1))
        );
    }

    private static Set<Long> parentModelsWithModifications(List<Model> models) {
        return models.stream()
            .filter(model -> !model.getModifications().isEmpty())
            .map(Model::getId)
            .collect(Collectors.toSet());
    }

    private static Stream<Model> flatModels(List<Model> models) {
        return models.stream()
            .flatMap(m -> Stream.concat(Stream.of(m), m.modifications.stream()));
    }


    private static Model model(long id, boolean hasFeature, Model... modifications) {
        return new Model(id, hasFeature, modifications);
    }

    private static class Model {
        private Long parentId;
        private final long id;
        private final boolean hasFeature;
        private final List<Model> modifications;

        Model(long id, boolean hasFeature, Model... modifications) {
            this.id = id;
            this.hasFeature = hasFeature;
            this.modifications = Arrays.asList(modifications);
            this.modifications.forEach(m -> m.parentId = id);
        }

        public Long getParentId() {
            return parentId;
        }

        public long getId() {
            return id;
        }

        public boolean hasFeature() {
            return hasFeature;
        }

        public List<Model> getModifications() {
            return modifications;
        }

        public ModelIndexPayload toDocument() {
            return new ModelIndexPayload(id, 0L, parentId);
        }
    }

    private static class StorageFacadeStub implements AggregatedModelFindService.StorageFacade {
        private List<Model> models;

        public StorageFacadeStub setModels(List<Model> models) {
            this.models = models;
            return this;
        }

        @Override
        public void processQueryModels(MboIndexesFilter filter, Consumer<ModelIndexPayload> consumer) {
            flatModels(models)
                .filter(Model::hasFeature)
                .map(Model::toDocument)
                .forEach(consumer);
        }

        @Override
        public Set<Long> getModifications(List<Long> modelIds) {
            return models.stream()
                .filter(m -> modelIds.contains(m.getId()))
                .flatMap(m -> m.getModifications().stream())
                .map(Model::getId)
                .collect(Collectors.toSet());
        }
    }
}
