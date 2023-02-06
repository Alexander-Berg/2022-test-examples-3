package ru.yandex.market.books.dao;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ru.yandex.market.books.diff.dao.BookCard;
import ru.yandex.market.books.diff.dao.BookCardRandomizer;
import ru.yandex.market.http.ServiceException;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.MboParameters.GetCategoryParametersRequest;
import ru.yandex.market.mbo.http.GlobalVendorsService;
import ru.yandex.market.mbo.http.ModelStorageService;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static ru.yandex.market.mbo.http.ModelStorage.LocalizedString;
import static ru.yandex.market.mbo.http.ModelStorage.Model;
import static ru.yandex.market.mbo.http.ModelStorage.OperationResponse;
import static ru.yandex.market.mbo.http.ModelStorage.OperationStatus;
import static ru.yandex.market.mbo.http.ModelStorage.OperationStatusType;
import static ru.yandex.market.mbo.http.ModelStorage.OperationType;
import static ru.yandex.market.mbo.http.ModelStorage.SaveModelsRequest;

/**
 * @author Alexander Kramarev (pochemuto@gmail.com)
 * @date 02.08.2018
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class ModelProcessorTest {
    private static final long SEED = 2435491234693L;
    private EnhancedRandom enhancedRandom;

    private CardsDiff diff;
    private ModelProcessor modelProcessor;

    @Mock
    private GlobalVendorsService globalVendorsService;

    @Mock
    private ModelStorageService modelStorageService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void before() {
        enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED)
            .randomize(BookCard.class, BookCardRandomizer.aNewRandomizer(SEED))
            .build();

        diff = new CardsDiff();

        CategoryParametersService parametersService = mock(CategoryParametersService.class, withSettings().lenient());

        modelProcessor = new ModelProcessor(parametersService, globalVendorsService, modelStorageService,
                Collections.emptyMap()) {
            @Override
            protected Optional<Model> createModel(Long id, BookCard bookCard) {
                return Optional.of(Model
                    .newBuilder()
                    .addTitles(LocalizedString.newBuilder().setValue(bookCard.getTitle()).build())
                    .build());
            }
        };

        when(modelStorageService.saveModels(any(SaveModelsRequest.class)))
            .then(returnOkStatus());

        when(parametersService.getParameters(any(GetCategoryParametersRequest.class)))
            .thenReturn(MboParameters.GetCategoryParametersResponse.newBuilder().build());
    }

    private Answer<OperationResponse> returnOkStatus() {
        return invocation -> createOkResponse(invocation.getArgument(0));
    }

    private Answer<OperationResponse> returnOkStatusButFailInvocations(int... invocationsToFail) {
        return new Answer<OperationResponse>() {
            private int invocationCount = 0;
            private int[] invocationsToFailSorted;
            {
                invocationsToFailSorted = invocationsToFail;
                Arrays.sort(invocationsToFailSorted);
            }

            @Override
            public OperationResponse answer(InvocationOnMock invocation) throws Throwable {
                if (Arrays.binarySearch(invocationsToFailSorted, ++invocationCount) >= 0) {
                    throw new ServiceException("mock exception");
                } else {
                    return createOkResponse(invocation.getArgument(0));
                }
            }
        };
    }

    private OperationResponse createOkResponse(SaveModelsRequest request) {
        OperationResponse.Builder response = OperationResponse.newBuilder();
        Stream.generate(
            () -> OperationStatus.newBuilder()
                .setStatus(OperationStatusType.OK)
                .setType(OperationType.CHANGE)
                .build())
            .limit(request.getModelsCount())
            .forEach(response::addStatuses);
        return response.build();
    }

    @Test
    public void returnRightSavedCount() {
        diff.getNewCards().addAll(generateBookCards(9));
        diff.getChangedCards().addAll(generateBookCards(15));


        int saved = modelProcessor.saveDiff(diff);


        assertThat(saved).isEqualTo(9 + 15);
    }

    @Test
    public void failEarly() {
        diff.getNewCards().addAll(generateBookCards(ModelProcessor.BATCH_SIZE * 2));
        diff.getChangedCards().addAll(generateBookCards(ModelProcessor.BATCH_SIZE * 3));
        int total = ModelProcessor.BATCH_SIZE * 5;

        when(modelStorageService.saveModels(any(SaveModelsRequest.class))).thenThrow(ServiceException.class);

        expectedException.expectMessage("failed " + ModelProcessor.BATCH_SIZE + " of " + total + " models");


        modelProcessor.saveDiff(diff);
    }

    @Test
    public void dontFailIfErrorsLessThenLimit() {
        int toBeFailed = ModelProcessor.BATCH_SIZE * 2;
        int total = toBeFailed * 20 + 1;
        diff.getNewCards().addAll(generateBookCards(ModelProcessor.BATCH_SIZE));
        diff.getChangedCards().addAll(generateBookCards(total - ModelProcessor.BATCH_SIZE));

        when(modelStorageService.saveModels(any(SaveModelsRequest.class)))
            .then(returnOkStatusButFailInvocations(1, 3));

        int saved = modelProcessor.saveDiff(diff);


        assertThat(saved).isEqualTo(total - toBeFailed);
    }

    private Collection<BookCard> generateBookCards(int count) {
        return enhancedRandom.objects(BookCard.class, count).collect(Collectors.toList());
    }
}
