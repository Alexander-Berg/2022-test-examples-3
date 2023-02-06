package ru.yandex.market.mbo.tms.report;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.configs.yt.YtPoolConfig;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.ParameterService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorDBInterface;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.user.AutoUser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PskuVendorDefinitionExecutorTest {

    private static final Long CATEGORY_OK = 123L;
    private static final Long CATEGORY_TWO = 333L;
    private static final Long CATEGORY_WITH_GV = 444L;

    private static final Long VENDOR_ONE_ID = 1L;
    private static final Long VENDOR_TWO_ID = 2L;
    private static final Long VENDOR_THREE_ID = 3L;
    private static final Long VENDOR_FOUR_ID = 4L;

    private final Random random = new Random();

    @Mock
    private YtPoolConfig ytPoolConfig;
    @Mock
    private GlobalVendorDBInterface globalVendorDBInterface;
    @Mock
    private IParameterLoaderService parameterLoaderService;
    @Mock
    private ParameterService parameterService;
    @Mock
    private AutoUser autoUser;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TovarTreeService tovarTreeService;
    @InjectMocks
    private PskuVendorDefinitionExecutor pskuVendorDefinitionExecutor;


    private final List<GlobalVendor> vendors = Stream.of(
        generateGlobalVendor(VENDOR_ONE_ID, "Adidas", "адидас", "спорт товары"),
        generateGlobalVendor(VENDOR_TWO_ID, "forbidden", "zxc", "Не Наследовать в категории"),
        generateGlobalVendor(VENDOR_THREE_ID, "Lasd", "test", ""),
        generateGlobalVendor(VENDOR_FOUR_ID, "Lasd", "test", "")
    ).collect(Collectors.toList());

    private final Map<Long, CategoryEntities> categoryEntitiesMap = Stream.of(
        generateCategoryEntity(CATEGORY_OK, null),
        generateCategoryEntity(CATEGORY_TWO, null),
        generateCategoryEntity(CATEGORY_WITH_GV, VENDOR_ONE_ID)
    ).collect(Collectors.toMap(CategoryEntities::getHid, Function.identity()));

    @Before
    public void setUp() {
        when(ytPoolConfig.commonYqlJdbcTemplate()).thenReturn(jdbcTemplate);
        when(globalVendorDBInterface.loadAllVendors()).thenReturn(vendors);
        categoryEntitiesMap.forEach((hid, value) -> {
                when(parameterLoaderService.loadCategoryEntitiesByHid(hid))
                    .thenReturn(value);
                Parameter parent = new Parameter();
                parent.setId(random.nextInt());
                InheritedParameter inheritedParameter = new InheritedParameter(parent);
                inheritedParameter.setId(random.nextInt());
                when(parameterLoaderService.loadParameter(KnownIds.VENDOR_PARAM_ID, hid))
                    .thenReturn(inheritedParameter);
            }
        );
        TovarCategory tovarCategory = new TovarCategory();
        tovarCategory.setLeaf(true);
        when(tovarTreeService.getCategoryByHid(anyLong())).thenReturn(tovarCategory);
    }

    @Test
    public void globalVendorWasFoundAmongNamesTest() throws Exception {
        List<YTData> currentData = Stream.of(
            new YTData(CATEGORY_OK, "adidas")
        ).collect(Collectors.toList());

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            currentData.forEach(data -> {
                ResultSet rs = Mockito.mock(ResultSet.class);
                try {
                    when(rs.getString(eq("category_id"))).thenReturn(data.getCategoryId().toString());
                    when(rs.getString(eq("raw_vendor"))).thenReturn(data.getRawVendor());
                    rch.processRow(rs);
                } catch (SQLException e) {
                }
            });
            return null;
        }).when(jdbcTemplate).query(Mockito.anyString(), any(RowCallbackHandler.class));


        pskuVendorDefinitionExecutor.doRealJob(null);

        verify(parameterService).addLocalVendor(
            any(), eq(CATEGORY_OK), any(InheritedParameter.class), any()
        );
    }

    @Test
    public void globalVendorWasFoundAmongAliasesTest() throws Exception {
        List<YTData> currentData = Stream.of(
            new YTData(CATEGORY_OK, "адидас")
        ).collect(Collectors.toList());

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            currentData.forEach(data -> {
                ResultSet rs = Mockito.mock(ResultSet.class);
                try {
                    when(rs.getString(eq("category_id"))).thenReturn(data.getCategoryId().toString());
                    when(rs.getString(eq("raw_vendor"))).thenReturn(data.getRawVendor());
                    rch.processRow(rs);
                } catch (SQLException e) {
                }
            });
            return null;
        }).when(jdbcTemplate).query(Mockito.anyString(), any(RowCallbackHandler.class));


        pskuVendorDefinitionExecutor.doRealJob(null);

        verify(parameterService).addLocalVendor(
            any(), eq(CATEGORY_OK), any(InheritedParameter.class), any()
        );
    }

    @Test
    public void moreThanOneSatisfyingVendorTest() throws Exception {
        List<YTData> currentData = Stream.of(
            new YTData(CATEGORY_OK, "test")
        ).collect(Collectors.toList());

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            currentData.forEach(data -> {
                ResultSet rs = Mockito.mock(ResultSet.class);
                try {
                    when(rs.getString(eq("category_id"))).thenReturn(data.getCategoryId().toString());
                    when(rs.getString(eq("raw_vendor"))).thenReturn(data.getRawVendor());
                    rch.processRow(rs);
                } catch (SQLException e) {
                }
            });
            return null;
        }).when(jdbcTemplate).query(Mockito.anyString(), any(RowCallbackHandler.class));


        pskuVendorDefinitionExecutor.doRealJob(null);

        verify(parameterService, times(0)).addLocalVendor(
            any(), anyLong(), any(InheritedParameter.class), any()
        );
    }

    @Test
    public void foundVendorWithForbiddenWordsTest() throws Exception {
        List<YTData> currentData = Stream.of(
            new YTData(CATEGORY_OK, "forbidden")
        ).collect(Collectors.toList());

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            currentData.forEach(data -> {
                ResultSet rs = Mockito.mock(ResultSet.class);
                try {
                    when(rs.getString(eq("category_id"))).thenReturn(data.getCategoryId().toString());
                    when(rs.getString(eq("raw_vendor"))).thenReturn(data.getRawVendor());
                    rch.processRow(rs);
                } catch (SQLException e) {
                }
            });
            return null;
        }).when(jdbcTemplate).query(Mockito.anyString(), any(RowCallbackHandler.class));


        pskuVendorDefinitionExecutor.doRealJob(null);

        verify(parameterService, times(0)).addLocalVendor(
            any(), anyLong(), any(InheritedParameter.class), any()
        );
    }

    @Test
    public void categoryAlreadyHasGlobalVendorTest() throws Exception {
        List<YTData> currentData = Stream.of(
            new YTData(CATEGORY_WITH_GV, "adidas")
        ).collect(Collectors.toList());

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            currentData.forEach(data -> {
                ResultSet rs = Mockito.mock(ResultSet.class);
                try {
                    when(rs.getString(eq("category_id"))).thenReturn(data.getCategoryId().toString());
                    when(rs.getString(eq("raw_vendor"))).thenReturn(data.getRawVendor());
                    rch.processRow(rs);
                } catch (SQLException e) {
                }
            });
            return null;
        }).when(jdbcTemplate).query(Mockito.anyString(), any(RowCallbackHandler.class));


        pskuVendorDefinitionExecutor.doRealJob(null);

        verify(parameterService, times(0)).addLocalVendor(
            any(), anyLong(), any(InheritedParameter.class), any()
        );
    }

    private GlobalVendor generateGlobalVendor(Long id, String name, String alias, String comment) {
        GlobalVendor globalVendor = new GlobalVendor();
        globalVendor.setId(id);
        globalVendor.setPublished(true);
        globalVendor.addName(new Word(Word.DEFAULT_LANG_ID, name));
        globalVendor.addAliases(new Word(Word.DEFAULT_LANG_ID, alias));
        globalVendor.setComment(comment);
        return globalVendor;
    }

    private static class YTData {
        private final Long categoryId;
        private final String rawVendor;

        YTData(Long categoryId, String rawVendor) {
            this.categoryId = categoryId;
            this.rawVendor = rawVendor;
        }

        public Long getCategoryId() {
            return categoryId;
        }


        public String getRawVendor() {
            return rawVendor;
        }
    }

    private CategoryEntities generateCategoryEntity(Long hid, Long vendorId) {
        CategoryEntities categoryEntities = new CategoryEntities();

        if (vendorId != null) {
            Option option = new OptionImpl();
            option.setId(vendorId);

            CategoryParam parameter = new Parameter();
            parameter.setId(KnownIds.VENDOR_PARAM_ID);
            parameter.addOption(option);
            categoryEntities.addParameter(parameter);
        }

        categoryEntities.setHid(hid);

        return categoryEntities;
    }

}

