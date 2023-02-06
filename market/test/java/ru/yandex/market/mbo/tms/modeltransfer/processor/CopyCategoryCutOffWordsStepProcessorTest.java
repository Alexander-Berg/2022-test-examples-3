package ru.yandex.market.mbo.tms.modeltransfer.processor;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.catalogue.MarketDepotServiceMock;
import ru.yandex.market.mbo.category.mappings.CategoryMappingServiceMock;
import ru.yandex.market.mbo.cutoff.GuruCategoryCutOffService;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.CutOffTransferList;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfCutWordsConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.TextResult;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.Arrays;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CopyCategoryCutOffWordsStepProcessorTest {
    private static final int USER_ID = 1;
    private static final AutoUser AUTO_USER = new AutoUser(USER_ID);

    private CopyCategoryCutOffWordsStepProcessor stepProcessor;
    private CategoryMappingServiceMock categoryMappingService;
    private GuruCategoryCutOffService guruCategoryCutOffService;
    private TovarTreeServiceMock tovarTreeServiceMock;
    private MarketDepotServiceMock marketDepotService;

    @Before
    public void before() throws Exception {
        this.categoryMappingService = new CategoryMappingServiceMock();
        this.marketDepotService = new MarketDepotServiceMock();
        this.guruCategoryCutOffService = new GuruCategoryCutOffService(categoryMappingService, marketDepotService);
        this.tovarTreeServiceMock = new TovarTreeServiceMock();
        this.stepProcessor = new CopyCategoryCutOffWordsStepProcessor(guruCategoryCutOffService, tovarTreeServiceMock,
            AUTO_USER);

        this.categoryMappingService.addMapping(1, 10L);
        this.categoryMappingService.addMapping(2, 20L);
        this.categoryMappingService.addMapping(3, 30L);

        this.guruCategoryCutOffService.saveCutOffWords(1, Arrays.asList("111", "aaa", "bbb", "ccc"), USER_ID);
        this.guruCategoryCutOffService.saveCutOffWords(2, Arrays.asList("222"), USER_ID);
        this.guruCategoryCutOffService.saveCutOffWords(3, Arrays.asList("333"), USER_ID);

        this.tovarTreeServiceMock.addCategory(TovarCategoryBuilder.newBuilder(1, 1).setName("Пистолеты").create());
        this.tovarTreeServiceMock.addCategory(TovarCategoryBuilder.newBuilder(2, 2).setName("Пистолетики").create());
        this.tovarTreeServiceMock.addCategory(TovarCategoryBuilder.newBuilder(3, 3).setName("Пистолетища").create());
        this.tovarTreeServiceMock.addCategory(TovarCategoryBuilder.newBuilder(4, 4).setName("Не-гуру-категория")
            .create());
    }

    @Test
    public void testSuccessfulTransfer() throws Exception {
        ResultInfo resultInfo = new ResultInfo();
        ListOfCutWordsConfig listOfCutWordsConfig = new ListOfCutWordsConfig();
        listOfCutWordsConfig.setListsOfModels(Arrays.asList(
            new CutOffTransferList(1, 2, "aaa", "bbb"),
            new CutOffTransferList(1, 3, "ccc")
        ));

        TextResult textResult = stepProcessor.executeStep(resultInfo, wrap(listOfCutWordsConfig));

        Assertions.assertThat(resultInfo.getStatus()).isEqualTo(ResultInfo.Status.COMPLETED);
        Assertions.assertThat(textResult.getText())
            .contains("Копирование отсекающий слов в категорию 'Пистолетики' (2) " +
                "из категории(ий) 'Пистолеты' (1) прошло успешно!")
            .contains("Копирование отсекающий слов в категорию 'Пистолетища' (3) " +
                "из категории(ий) 'Пистолеты' (1) прошло успешно!");

        Assertions.assertThat(guruCategoryCutOffService.loadCutOffWords(2))
            .containsExactly("222", "aaa", "bbb");
        Assertions.assertThat(guruCategoryCutOffService.loadCutOffWords(3))
            .containsExactly("333", "ccc");
    }

    @Test
    public void testSuccessfulTransferToOneTargetCategoryId() {
        ResultInfo resultInfo = new ResultInfo();
        ListOfCutWordsConfig listOfCutWordsConfig = new ListOfCutWordsConfig();
        listOfCutWordsConfig.setListsOfModels(Arrays.asList(
            new CutOffTransferList(2, 1, "222"),
            new CutOffTransferList(3, 1, "333")
        ));

        TextResult textResult = stepProcessor.executeStep(resultInfo, wrap(listOfCutWordsConfig));

        Assertions.assertThat(resultInfo.getStatus()).isEqualTo(ResultInfo.Status.COMPLETED);
        Assertions.assertThat(textResult.getText())
            .contains("Копирование отсекающий слов в категорию 'Пистолеты' (1) " +
                "из категории(ий) 'Пистолетики' (2), 'Пистолетища' (3) прошло успешно!");

        Assertions.assertThat(guruCategoryCutOffService.loadCutOffWords(1))
            .containsExactly("111", "222", "333", "aaa", "bbb", "ccc");
    }

    @Test
    public void testFailedTransfer() {
        ResultInfo resultInfo = new ResultInfo();
        ListOfCutWordsConfig listOfCutWordsConfig = new ListOfCutWordsConfig();
        listOfCutWordsConfig.setListsOfModels(Arrays.asList(
            new CutOffTransferList(1, 4, "aaa", "bbb"),
            new CutOffTransferList(1, 3, "ccc")
        ));

        TextResult textResult = stepProcessor.executeStep(resultInfo, wrap(listOfCutWordsConfig));

        Assertions.assertThat(resultInfo.getStatus()).isEqualTo(ResultInfo.Status.FAILED);
        Assertions.assertThat(textResult.getText())
            .contains("Внутреняя ошибка при копировании отсекающих слов в категорию 'Не-гуру-категория' (4)" +
                " из категории(ий) 'Пистолеты' (1).")
            .contains("Копирование отсекающий слов в категорию 'Пистолетища' (3) " +
                "из категории(ий) 'Пистолеты' (1) прошло успешно!");

        Assertions.assertThat(guruCategoryCutOffService.loadCutOffWords(3))
            .containsExactly("333", "ccc");
    }

    @SuppressWarnings("unchecked")
    private ModelTransferJobContext<ListOfCutWordsConfig> wrap(ListOfCutWordsConfig config) {
        ModelTransferJobContext<ListOfCutWordsConfig> mock = Mockito.mock(ModelTransferJobContext.class);
        Mockito.when(mock.getStepConfig()).thenReturn(config);
        return mock;
    }
}
