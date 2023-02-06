package ru.yandex.direct.internaltools.tools.bsresyncqueue;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncItem;
import ru.yandex.direct.core.entity.bs.resync.queue.service.BsResyncService;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.core.exception.InternalToolValidationException;
import ru.yandex.direct.internaltools.tools.bsresyncqueue.container.AddObjectInfo;
import ru.yandex.direct.internaltools.tools.bsresyncqueue.model.AddObjectsToBsResyncQueueParameters;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.internaltools.tools.bsresyncqueue.AddObjectsToBsResyncQueueTool.FAILED_MESSAGE_FORMAT;
import static ru.yandex.direct.internaltools.tools.bsresyncqueue.AddObjectsToBsResyncQueueTool.SUCCESS_MESSAGE_FORMAT;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.TsvParserUtils.getBeansList;
import static ru.yandex.direct.validation.defect.CollectionDefects.minCollectionSize;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AddObjectsToBsResyncQueueToolMethodTest {

    private static final String LS = "\n";
    private static final String LS_MAC = "\r";
    private static final String LS_WIN = "\r\n";
    private static final String FILE_HEADER = "cid\tbid\tpid\tpriority";
    private static final String VALID_FILE_DATA = String.join(LS, FILE_HEADER, "123\t0\t0\t0", "124\t125\t126\t127");
    private static final String MAC_FILE_DATA = String.join(LS_MAC, FILE_HEADER, "123\t0\t0\t0", "124\t125\t126\t127");
    private static final String WIN_FILE_DATA = String.join(LS_WIN, FILE_HEADER, "123\t0\t0\t0", "124\t125\t126\t127");

    private AddObjectsToBsResyncQueueTool tool;
    private AddObjectsToBsResyncQueueParameters request;

    @Mock
    private BsResyncService bsResyncService;

    @Captor
    private ArgumentCaptor<List<BsResyncItem>> captor;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        initMocks(this);
        tool = new AddObjectsToBsResyncQueueTool(bsResyncService);
        request = new AddObjectsToBsResyncQueueParameters();
    }


    @Test
    public void checkValidation() {
        ValidationResult<AddObjectsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withFile(VALID_FILE_DATA.getBytes()));

        assertThat("нет ошибок валидации", validate.hasAnyErrors(), is(false));
    }

    @Test
    public void checkValidationMacFile() {
        ValidationResult<AddObjectsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withFile(MAC_FILE_DATA.getBytes()));

        assertThat("нет ошибок валидации", validate, hasNoDefectsDefinitions());
    }

    @Test
    public void checkValidationWinFile() {
        ValidationResult<AddObjectsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withFile(WIN_FILE_DATA.getBytes()));

        assertThat("нет ошибок валидации", validate, hasNoDefectsDefinitions());
    }

    @Test(expected = InternalToolValidationException.class)
    public void checkThrownException_whenInvalidFileData() {
        String invalidFileData = FILE_HEADER + LS + "123\t0\tnull\t0\n";
        tool.validate(request.withFile(invalidFileData.getBytes()));
    }

    @Test(expected = InternalToolValidationException.class)
    public void checkThrownException_whenPriorityColumnMissed() {
        String invalidFileData = "cid\tbid\tpid\n" + "123\t0\t0\n";
        tool.validate(request.withFile(invalidFileData.getBytes()));
    }

    @Test
    public void checkValidationErrors_whenEmptyFileData() {
        ValidationResult<AddObjectsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withFile((FILE_HEADER + LS).getBytes()));

        assertThat(validate, hasDefectDefinitionWith(validationError(path(field("file")), minCollectionSize(1))));
    }

    @Test
    public void checkValidationErrors_invalidPriority() {
        byte[] data = (FILE_HEADER + LS + "123\t0\t0\t999").getBytes();
        ValidationResult<AddObjectsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withFile(data));

        Path errorPath = path(field("file"), index(2), field("priority"));
        assertThat(validate, hasDefectDefinitionWith(validationError(errorPath, MUST_BE_IN_THE_INTERVAL_INCLUSIVE)));
    }

    @Test
    public void checkValidationErrors_invalidBannerId() {
        byte[] data = (FILE_HEADER + LS + "123\t-1\t0\t0").getBytes();
        ValidationResult<AddObjectsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withFile(data));

        Path errorPath = path(field("file"), index(2), field("bid"));
        assertThat(validate, hasDefectDefinitionWith(validationError(errorPath, MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN)));
    }

    @Test
    public void checkValidationErrors_invalidAdGroupId() {
        byte[] data = (FILE_HEADER + LS + "123\t0\t-1\t0").getBytes();
        ValidationResult<AddObjectsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withFile(data));

        Path errorPath = path(field("file"), index(2), field("pid"));
        assertThat(validate, hasDefectDefinitionWith(validationError(errorPath, MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN)));
    }

    @Test
    public void checkValidationErrors_invalidCampaignId() {
        byte[] data = (FILE_HEADER + LS + "0\t0\t0\t0").getBytes();
        ValidationResult<AddObjectsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withFile(data));

        Path errorPath = path(field("file"), index(2), field("cid"));
        assertThat(validate, hasDefectDefinitionWith(validationError(errorPath, validId())));
    }

    @Test
    public void checkCallBsResync() {
        List<AddObjectInfo> addObjectInfos = getBeansList(VALID_FILE_DATA.getBytes(), AddObjectInfo.class);
        List<BsResyncItem> bsResyncItems = addObjectInfos.stream()
                .map(o -> new BsResyncItem(o.getPriority(), o.getCampaignId(), o.getBannerId(), o.getAdGroupId()))
                .collect(toList());

        tool.process(request.withFile(VALID_FILE_DATA.getBytes()));
        verify(bsResyncService).addObjectsToResync(captor.capture());
        assertThat("параметры соответствуют ожиданиям", captor.getValue(), beanDiffer(bsResyncItems));
    }

    @Test
    public void checkCallBsResyncWinFormat() {

        List<BsResyncItem> bsResyncItems = asList(
                new BsResyncItem(0L, 123L, 0L, 0L),
                new BsResyncItem(127L, 124L, 125L, 126L)
        );

        tool.process(request.withFile(WIN_FILE_DATA.getBytes()));
        verify(bsResyncService).addObjectsToResync(captor.capture());
        assertThat("параметры соответствуют ожиданиям", captor.getValue(), beanDiffer(bsResyncItems));
    }

    @Test
    public void checkSuccessProcessResult() {
        long objectsCount = 2;
        doReturn(objectsCount).when(bsResyncService).addObjectsToResync(any());

        InternalToolResult result = tool.process(request.withFile(VALID_FILE_DATA.getBytes()));

        InternalToolResult expectedResult = new InternalToolResult()
                .withMessage(format(SUCCESS_MESSAGE_FORMAT, objectsCount));
        assertThat("результат соответствуют ожиданиям", result, beanDiffer(expectedResult));
    }

    @Test
    public void checkFailedProcessResult() {
        long objectsCount = 2;
        long addedObjectsCount = objectsCount - 1;
        doReturn(addedObjectsCount).when(bsResyncService).addObjectsToResync(any());

        InternalToolResult result = tool.process(request.withFile(VALID_FILE_DATA.getBytes()));

        InternalToolResult expectedResult = new InternalToolResult()
                .withMessage(format(FAILED_MESSAGE_FORMAT, addedObjectsCount, objectsCount));
        assertThat("результат соответствуют ожиданиям", result, beanDiffer(expectedResult));
    }
}
