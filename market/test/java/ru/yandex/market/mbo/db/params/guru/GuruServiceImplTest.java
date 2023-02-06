package ru.yandex.market.mbo.db.params.guru;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import ru.yandex.market.mbo.http.MboGuruService;
import ru.yandex.market.mbo.http.MboGuruService.GetGuruCategoryByHidResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ayratgdl
 * @since 09.11.18
 */
public class GuruServiceImplTest {
    private static final long CATEGORY_WITH_GURU_ID = 101;
    private static final long CATEGORY_WITHOUT_GURU_ID = 102;
    private static final long GURU_ID = 201;

    private GuruServiceImpl guruServiceImpl;

    private Map<Long, Long> categoryToGuru = new HashMap<>();


    @Before
    public void setUp() throws Exception {
        GuruService guruService = Mockito.mock(GuruService.class);
        Mockito.when(guruService.getGuruCategoryByHid(Mockito.anyLong())).thenAnswer(
            new Answer<Long>() {
                @Override
                public Long answer(InvocationOnMock invocation) throws Throwable {
                    Long categoryId = invocation.getArgument(0);
                    if (categoryToGuru.containsKey(categoryId)) {
                        return categoryToGuru.get(categoryId);
                    } else {
                        throw new IncorrectResultSizeDataAccessException(1);
                    }
                }
            }
        );
        guruServiceImpl = new GuruServiceImpl(guruService, null);

        categoryToGuru.put(CATEGORY_WITH_GURU_ID, GURU_ID);
    }

    @Test
    public void ifGuruExistedThenReturnGuruId() {
        GetGuruCategoryByHidResponse response = guruServiceImpl.getGuruCategoryByHid(
            MboGuruService.GetGuruCategoryByHidRequest
                .newBuilder()
                .setCategoryId(CATEGORY_WITH_GURU_ID)
                .build()
        );
        Assert.assertTrue(response.hasGuruCategoryId());
        Assert.assertEquals(GURU_ID, response.getGuruCategoryId());
    }

    @Test
    public void ifGuruNotExistedThenNoReturnGuruId() {
        GetGuruCategoryByHidResponse response = guruServiceImpl.getGuruCategoryByHid(
            MboGuruService.GetGuruCategoryByHidRequest
                .newBuilder()
                .setCategoryId(CATEGORY_WITHOUT_GURU_ID)
                .build()
        );
        Assert.assertFalse(response.hasGuruCategoryId());
    }
}
