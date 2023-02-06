package ru.yandex.market.mbo.tms.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionReason;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.db.navigation.ModelListService;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.db.repo.ModelTransitionsRepository;
import ru.yandex.market.mbo.gwt.models.model_list.ModelList;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.utils.BaseDbTest;

@SuppressWarnings("checkstyle:MagicNumber")
public class SyncNavigationTreeModelsExecutorTest extends BaseDbTest {

    @Autowired
    private ModelTransitionsRepository modelTransitionsRepository;

    private ModelListService modelListService;
    private ModelListService modelListServiceDraft;
    private NavigationTreeService navigationTreeService;
    private NavigationTreeService navigationTreeServiceDraft;
    private SyncNavigationTreeModelsExecutor executor;

    @Before
    public void setUp() throws Exception {
        modelListService = Mockito.mock(ModelListService.class);
        modelListServiceDraft = Mockito.mock(ModelListService.class);

        navigationTreeService = Mockito.mock(NavigationTreeService.class);
        Mockito.when(navigationTreeService.getModelListService()).thenReturn(modelListService);
        navigationTreeServiceDraft = Mockito.mock(NavigationTreeService.class);
        Mockito.when(navigationTreeServiceDraft.getModelListService()).thenReturn(modelListServiceDraft);

        executor = new SyncNavigationTreeModelsExecutor(
            modelTransitionsRepository,
            navigationTreeService,
            navigationTreeServiceDraft,
            Mockito.mock(AuditService.class),
            Mockito.mock(AutoUser.class));
    }


    @Test
    public void testErrorTransition() throws Exception {
        modelTransitionsRepository.save(Arrays.asList(
            createModelTransition(ModelTransitionType.ERROR, 5L, null),
            createModelTransition(ModelTransitionType.ERROR, 7L, null)
        ));
        List<ModelList> modelLists = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 2L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 5L))
        );
        Mockito.when(modelListService.getAllModelLists()).thenReturn(modelLists);

        List<ModelList> modelListsDraft = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 5L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 10L))
        );
        Mockito.when(modelListServiceDraft.getAllModelLists()).thenReturn(modelListsDraft);

        executor.doRealJob(null);

        ArgumentCaptor<ModelList> argumentCaptor1 = ArgumentCaptor.forClass(ModelList.class);
        ArgumentCaptor<ModelList> argumentCaptor2 = ArgumentCaptor.forClass(ModelList.class);

        ModelList expectedToSave = createModelList(2L, Arrays.asList(3L, 2L));
        ModelList expectedToSaveDraft = createModelList(1L, Arrays.asList(1L, 3L));
        Mockito.verify(modelListService).saveModelList(argumentCaptor1.capture());
        Mockito.verify(modelListServiceDraft).saveModelList(argumentCaptor2.capture());

        checkCallResult(argumentCaptor1, expectedToSave);
        checkCallResult(argumentCaptor2, expectedToSaveDraft);
    }

    @Test
    public void testErrorSeveralTransition() throws Exception {
        modelTransitionsRepository.save(Arrays.asList(
            createModelTransition(ModelTransitionType.ERROR, 2L, null),
            createModelTransition(ModelTransitionType.ERROR, 7L, null)
        ));
        List<ModelList> modelLists = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 2L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 5L))
        );
        Mockito.when(modelListService.getAllModelLists()).thenReturn(modelLists);

        List<ModelList> modelListsDraft = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 2L, 7L)),
            createModelList(2L, Arrays.asList(7L, 2L, 5L))
        );
        Mockito.when(modelListServiceDraft.getAllModelLists()).thenReturn(modelListsDraft);

        executor.doRealJob(null);

        ArgumentCaptor<ModelList> argumentCaptor1 = ArgumentCaptor.forClass(ModelList.class);
        ArgumentCaptor<ModelList> argumentCaptor2 = ArgumentCaptor.forClass(ModelList.class);

        ModelList expectedToSave1 = createModelList(1L, Arrays.asList(1L, 3L));
        ModelList expectedToSave2 = createModelList(2L, Arrays.asList(3L, 5L));

        ModelList expectedToSaveDraft1 = createModelList(1L, Arrays.asList(1L));
        ModelList expectedToSaveDraft2 = createModelList(2L, Arrays.asList(5L));

        Mockito.verify(modelListService, Mockito.times(2))
            .saveModelList(argumentCaptor1.capture());
        Mockito.verify(modelListServiceDraft, Mockito.times(2))
            .saveModelList(argumentCaptor2.capture());

        checkCallResult(argumentCaptor1, expectedToSave1, expectedToSave2);
        checkCallResult(argumentCaptor2, expectedToSaveDraft1, expectedToSaveDraft2);
    }

    @Test
    public void testDublicateTransition() throws Exception {
        modelTransitionsRepository.save(Arrays.asList(
            createModelTransition(ModelTransitionType.DUPLICATE, 5L, 10L),
            createModelTransition(ModelTransitionType.DUPLICATE, 7L, 20L)
        ));
        List<ModelList> modelLists = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 2L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 5L))
        );
        Mockito.when(modelListService.getAllModelLists()).thenReturn(modelLists);

        List<ModelList> modelListsDraft = Arrays.asList(
            createModelList(1L, Arrays.asList(7L, 2L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 5L))
        );
        Mockito.when(modelListServiceDraft.getAllModelLists()).thenReturn(modelListsDraft);

        executor.doRealJob(null);

        ArgumentCaptor<ModelList> argumentCaptor1 = ArgumentCaptor.forClass(ModelList.class);
        ArgumentCaptor<ModelList> argumentCaptor2 = ArgumentCaptor.forClass(ModelList.class);

        ModelList expectedToSave = createModelList(2L, Arrays.asList(3L, 2L, 10L));
        ModelList expectedToSaveDraft1 = createModelList(1L, Arrays.asList(2L, 3L, 20L));
        ModelList expectedToSaveDraft2 = createModelList(2L, Arrays.asList(3L, 2L, 10L));

        Mockito.verify(modelListService).saveModelList(argumentCaptor1.capture());
        Mockito.verify(modelListServiceDraft, Mockito.times(2))
            .saveModelList(argumentCaptor2.capture());

        checkCallResult(argumentCaptor1, expectedToSave);
        checkCallResult(argumentCaptor2, expectedToSaveDraft1, expectedToSaveDraft2);
    }

    @Test
    public void testDublicateSeveralTransition() throws Exception {
        modelTransitionsRepository.save(Arrays.asList(
            createModelTransition(ModelTransitionType.DUPLICATE, 2L, 10L),
            createModelTransition(ModelTransitionType.DUPLICATE, 7L, 20L)
        ));
        List<ModelList> modelLists = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 2L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 5L))
        );
        Mockito.when(modelListService.getAllModelLists()).thenReturn(modelLists);

        List<ModelList> modelListsDraft = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 4L, 7L)),
            createModelList(2L, Arrays.asList(7L, 4L, 5L))
        );
        Mockito.when(modelListServiceDraft.getAllModelLists()).thenReturn(modelListsDraft);

        executor.doRealJob(null);

        ArgumentCaptor<ModelList> argumentCaptor1 = ArgumentCaptor.forClass(ModelList.class);
        ArgumentCaptor<ModelList> argumentCaptor2 = ArgumentCaptor.forClass(ModelList.class);

        ModelList expectedToSave1 = createModelList(1L, Arrays.asList(1L, 3L, 10L));
        ModelList expectedToSave2 = createModelList(2L, Arrays.asList(3L, 5L, 10L));

        ModelList expectedToSaveDraft1 = createModelList(1L, Arrays.asList(1L, 4L, 20L));
        ModelList expectedToSaveDraft2 = createModelList(2L, Arrays.asList(4L, 5L, 20L));

        Mockito.verify(modelListService, Mockito.times(2))
            .saveModelList(argumentCaptor1.capture());
        Mockito.verify(modelListServiceDraft, Mockito.times(2))
            .saveModelList(argumentCaptor2.capture());

        checkCallResult(argumentCaptor1, expectedToSave1, expectedToSave2);
        checkCallResult(argumentCaptor2, expectedToSaveDraft1, expectedToSaveDraft2);
    }

    @Test
    public void testSplitTransition() throws Exception {
        modelTransitionsRepository.save(Arrays.asList(
            createModelTransition(ModelTransitionType.SPLIT, 5L, 10L),
            createModelTransition(ModelTransitionType.SPLIT, 5L, 15L),
            createModelTransition(ModelTransitionType.SPLIT, 7L, 20L),
            createModelTransition(ModelTransitionType.SPLIT, 7L, 23L)
        ));
        List<ModelList> modelLists = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 2L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 5L))
        );
        Mockito.when(modelListService.getAllModelLists()).thenReturn(modelLists);

        List<ModelList> modelListsDraft = Arrays.asList(
            createModelList(1L, Arrays.asList(7L, 2L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 4L))
        );
        Mockito.when(modelListServiceDraft.getAllModelLists()).thenReturn(modelListsDraft);

        executor.doRealJob(null);

        ArgumentCaptor<ModelList> argumentCaptor1 = ArgumentCaptor.forClass(ModelList.class);
        ArgumentCaptor<ModelList> argumentCaptor2 = ArgumentCaptor.forClass(ModelList.class);

        ModelList expectedToSave = createModelList(2L, Arrays.asList(3L, 2L, 10L, 15L));
        ModelList expectedToSaveDraft = createModelList(1L, Arrays.asList(2L, 3L, 20L, 23L));

        Mockito.verify(modelListService).saveModelList(argumentCaptor1.capture());
        Mockito.verify(modelListServiceDraft).saveModelList(argumentCaptor2.capture());

        checkCallResult(argumentCaptor1, expectedToSave);
        checkCallResult(argumentCaptor2, expectedToSaveDraft);
    }

    @Test
    public void testSplitSeveralTransition() throws Exception {
        modelTransitionsRepository.save(Arrays.asList(
            createModelTransition(ModelTransitionType.SPLIT, 2L, 10L),
            createModelTransition(ModelTransitionType.SPLIT, 2L, 15L),
            createModelTransition(ModelTransitionType.SPLIT, 7L, 20L),
            createModelTransition(ModelTransitionType.SPLIT, 7L, 23L)
        ));
        List<ModelList> modelLists = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 2L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 5L))
        );
        Mockito.when(modelListService.getAllModelLists()).thenReturn(modelLists);

        List<ModelList> modelListsDraft = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 4L, 7L)),
            createModelList(2L, Arrays.asList(7L, 4L, 5L))
        );
        Mockito.when(modelListServiceDraft.getAllModelLists()).thenReturn(modelListsDraft);

        executor.doRealJob(null);

        ArgumentCaptor<ModelList> argumentCaptor1 = ArgumentCaptor.forClass(ModelList.class);
        ArgumentCaptor<ModelList> argumentCaptor2 = ArgumentCaptor.forClass(ModelList.class);

        ModelList expectedToSave1 = createModelList(1L, Arrays.asList(1L, 3L, 10L, 15L));
        ModelList expectedToSave2 = createModelList(2L, Arrays.asList(3L, 5L, 10L, 15L));

        ModelList expectedToSaveDraft1 = createModelList(1L, Arrays.asList(1L, 4L, 20L, 23L));
        ModelList expectedToSaveDraft2 = createModelList(2L, Arrays.asList(4L, 5L, 20L, 23L));

        Mockito.verify(modelListService, Mockito.times(2))
            .saveModelList(argumentCaptor1.capture());
        Mockito.verify(modelListServiceDraft, Mockito.times(2))
            .saveModelList(argumentCaptor2.capture());

        checkCallResult(argumentCaptor1, expectedToSave1, expectedToSave2);
        checkCallResult(argumentCaptor2, expectedToSaveDraft1, expectedToSaveDraft2);
    }

    @Test
    public void testRemoveModelListId() throws Exception {
        modelTransitionsRepository.save(Arrays.asList(
            createModelTransition(ModelTransitionType.ERROR, 5L, null),
            createModelTransition(ModelTransitionType.ERROR, 7L, null)
        ));
        List<ModelList> modelLists = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 2L, 3L)),
            createModelList(2L, Arrays.asList(5L))
        );
        Mockito.when(modelListService.getAllModelLists()).thenReturn(modelLists);

        List<ModelList> modelListsDraft = Arrays.asList(
            createModelList(1L, Arrays.asList(1L, 5L, 3L)),
            createModelList(2L, Arrays.asList(3L, 2L, 10L))
        );
        Mockito.when(modelListServiceDraft.getAllModelLists()).thenReturn(modelListsDraft);

        NavigationNode node = new SimpleNavigationNode();
        node.setId(77L);
        node.setModelListId(2L);
        NavigationNode nodeDraft = new SimpleNavigationNode();
        nodeDraft.setId(88L);
        nodeDraft.setModelListId(1L);
        Mockito.when(navigationTreeService.getNavigationNodeIdsByModelList(Mockito.any()))
            .thenReturn(Arrays.asList(node.getId()));
        Mockito.when(navigationTreeService.getNavigationNode(Mockito.any())).thenReturn(node);
        Mockito.when(navigationTreeServiceDraft.getNavigationNodeIdsByModelList(Mockito.any()))
            .thenReturn(Arrays.asList(nodeDraft.getId()));
        Mockito.when(navigationTreeServiceDraft.getNavigationNode(Mockito.any())).thenReturn(nodeDraft);

        executor.doRealJob(null);

        ArgumentCaptor<ModelList> argumentCaptor1 = ArgumentCaptor.forClass(ModelList.class);
        ArgumentCaptor<ModelList> argumentCaptor2 = ArgumentCaptor.forClass(ModelList.class);

        ModelList expectedToSave = createModelList(2L, Collections.emptyList());
        ModelList expectedToSaveDraft = createModelList(1L, Arrays.asList(1L, 3L));

        Mockito.verify(modelListService).saveModelList(argumentCaptor1.capture());
        Mockito.verify(modelListServiceDraft).saveModelList(argumentCaptor2.capture());

        checkCallResult(argumentCaptor1, expectedToSave);
        checkCallResult(argumentCaptor2, expectedToSaveDraft);

        NavigationNode nodeExpected = new SimpleNavigationNode();
        nodeExpected.setId(77L);
        Mockito.verify(navigationTreeService)
            .saveNavigationNodes(Mockito.any(), Mockito.eq(Arrays.asList(nodeExpected)));
        Mockito.verify(navigationTreeServiceDraft, Mockito.never()).saveNavigationNodes(Mockito.any(), Mockito.any());
    }

    private ModelList createModelList(Long id, List<Long> modelIds) {
        return new ModelList().setId(id).setModelIds(modelIds);
    }

    private ModelTransition createModelTransition(ModelTransitionType type, Long oldId, Long newId) {
        return new ModelTransition()
            .setDate(LocalDateTime.now())
            .setReason(ModelTransitionReason.MODEL_SPLIT)
            .setOldEntityDeleted(type == ModelTransitionType.ERROR)
            .setEntityType(EntityType.MODEL)
            .setType(type)
            .setOldEntityId(oldId)
            .setNewEntityId(newId);
    }

    private void checkCallResult(ArgumentCaptor<ModelList> argumentCaptor, ModelList... expectedModelLists) {
        List<ModelList> captured = argumentCaptor.getAllValues();
        Assertions.assertThat(captured.size()).isEqualTo(expectedModelLists.length);
        for (int i = 0; i < captured.size(); i++) {
            Assertions.assertThat(captured.get(i).getId()).isEqualTo(expectedModelLists[i].getId());
            Assertions.assertThat(captured.get(i).getModelIds())
                .containsExactlyInAnyOrderElementsOf(expectedModelLists[i].getModelIds());
        }
    }
}
