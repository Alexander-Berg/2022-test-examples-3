package ru.yandex.market.mbo.db.transfer;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.transfer.step.ModelTransferStepInfoService;
import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.SourceCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 09.09.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelTransferServiceImplTest {
    private ModelTransferService modelTransferService;
    private ModelTransferManager modelTransferManager;
    private Map<Long, Long> guruCategories = new HashMap<>();

    LoadingCache<Long, TovarCategory> tovarCategoryCache;

    @Before
    public void setUp() throws Exception {
        ModelTransferDAO modelTransferDAO = mock(ModelTransferDAO.class);
        when(modelTransferDAO.create(any())).thenReturn(-1L);
        UserService userService = mock(UserService.class);
        when(userService.getUser(anyLong())).thenAnswer(args -> {
            long uid = args.getArgument(0);
            return new User(uid, "login" + uid, "name" + uid);
        });
        ModelTransferStepInfoService stepInfoService = mock(ModelTransferStepInfoService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(args -> {
            TransactionCallback<?> action = args.getArgument(0);
            return action.doInTransaction(null);
        });
        TovarTreeService tovarTreeService = mock(TovarTreeService.class);
        when(tovarTreeService.getCategoryByHid(anyLong())).then(args -> {
            long categoryId = args.getArgument(0);
            TovarCategory tovarCategory = mock(TovarCategory.class);
            when(tovarCategory.getGuruCategoryId()).thenReturn(guruCategories.getOrDefault(categoryId, 0L));
            return tovarCategory;
        });
        modelTransferService = new ModelTransferServiceImpl(modelTransferDAO, userService, transactionTemplate);

        modelTransferManager = new ModelTransferManager(
            stepInfoService,
            userService,
            modelTransferDAO,
            transactionTemplate
        );

        tovarCategoryCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<Long, TovarCategory>() {
                @Override
                public TovarCategory load(Long hid) {
                    return tovarTreeService.getCategoryByHid(hid);
                }
            });
    }


    @Test(expected = OperationException.class)
    public void testValidateNoManager() {
        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder()
            .transferDate(now())
            .build();

        modelTransferService.validateModelTransfer(modelTransfer, tovarCategoryCache, 0);
    }

    @Test(expected = OperationException.class)
    public void testValidateNoTransferDate() {
        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder()
            .manager(new User(11L))
            .build();

        modelTransferService.validateModelTransfer(modelTransfer, tovarCategoryCache, 0);
    }

    @Test(expected = OperationException.class)
    public void testValidateIncorrectTransferDate() {
        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder()
            .manager(new User(11L))
            .transferDate(now())
            .build();

        modelTransferService.validateModelTransfer(modelTransfer, tovarCategoryCache, 0);
    }

    @Test(expected = OperationException.class)
    public void testValidateNoSourceCategories() {
        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder()
            .manager(new User(11L))
            .transferDate(nextYear())
            .build();

        modelTransferService.validateModelTransfer(modelTransfer, tovarCategoryCache, 0);
    }

    @Test(expected = OperationException.class)
    public void testValidateNoDestinationCategories() {
        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder()
            .manager(new User(11L))
            .transferDate(nextYear())
            .sourceCategory(new SourceCategory())
            .build();

        modelTransferService.validateModelTransfer(modelTransfer, tovarCategoryCache, 0);
    }

    @Test(expected = OperationException.class)
    public void testValidateSourceCategoryIsNotGuru() {
        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder()
            .manager(new User(11L))
            .transferDate(nextYear())
            .sourceCategory(1L)
            .destinationCategory(2L)
            .build();

        modelTransferService.validateModelTransfer(modelTransfer, tovarCategoryCache, 0);
    }

    @Test(expected = OperationException.class)
    public void testValidateSourceAndDestinationIntersects() {
        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder()
            .manager(new User(11L))
            .transferDate(nextYear())
            .sourceCategory(1L).sourceCategory(2L)
            .destinationCategory(2L).destinationCategory(3L)
            .build();

        guruCategories.put(1L, 100L);
        guruCategories.put(2L, 200L);

        modelTransferService.validateModelTransfer(modelTransfer, tovarCategoryCache, 0);
    }

    @Test
    public void testValidationSuccess() {
        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder()
            .manager(new User(11L))
            .transferDate(nextYear())
            .sourceCategory(1L).sourceCategory(2L)
            .destinationCategory(3L).destinationCategory(4L)
            .build();
        guruCategories.put(1L, 100L);
        guruCategories.put(2L, 200L);

        modelTransferService.validateModelTransfer(modelTransfer, tovarCategoryCache, 0);
        long transferId = modelTransferManager.createModelTransfer(modelTransfer, 10L);
        Assertions.assertThat(transferId).isEqualTo(-1L);
    }

    private LocalDateTime localNow() {
        LocalDateTime now = LocalDateTime.now();
        now.truncatedTo(ChronoUnit.DAYS);
        return now;
    }

    private Date now() {
        return ModelTransferUtils.toDate(localNow());
    }

    private Date nextYear() {
        LocalDateTime now = localNow();
        return ModelTransferUtils.toDate(now.plusYears(1L));
    }
}
