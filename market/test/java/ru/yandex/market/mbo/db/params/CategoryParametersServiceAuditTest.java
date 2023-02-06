package ru.yandex.market.mbo.db.params;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.core.utils.TransactionTemplateMock;
import ru.yandex.market.mbo.db.KDAuditService;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.db.VisualServiceAudit;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.params.audit.ParameterAuditService;
import ru.yandex.market.mbo.db.params.guru.GuruService;
import ru.yandex.market.mbo.db.params.validators.ParameterValidationService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.ParamOptionsAccessType;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboAuditService;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.mbo.validator.OptionPropertyDuplicationValidator;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author galaev@yandex-team.ru
 * @since 28/11/2018.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategoryParametersServiceAuditTest {

    @Mock
    private ParameterDAO parameterDAO;
    @Mock
    private TransactionTemplate contentTx;
    @Mock
    private NamedParameterJdbcTemplate contentJdbc;
    @Mock
    private IParameterLoaderService parameterLoaderService;
    @Mock
    private TovarTreeForVisualService tovarTree;
    @Mock
    private UserManager userManager;
    @Mock
    private IdGenerator kdepotIdGenerator;
    @Mock
    private GlobalVendorService globalVendorService;
    @Mock
    private MboAuditService mboAuditService;
    @Mock
    private VisualServiceAudit visualServiceAudit;
    @Mock
    private KDAuditService guruAuditService;
    @Mock
    private ParameterValidationService parameterValidationService;
    @Mock
    private OptionPropertyDuplicationValidator optionAliasValidator;
    @Mock
    private OptionPropertyDuplicationValidator optionNameValidator;
    @Mock
    private ValueLinkServiceInterface valueLinkService;
    @Mock
    private GuruService guruService;
    @Mock
    private ParameterAuditService parameterAuditService;

    @InjectMocks
    private ParameterServiceOriginal parameterService;

    @InjectMocks
    private CategoryParametersServiceImpl categoryParametersService = new CategoryParametersServiceImpl();

    @Captor
    private ArgumentCaptor<ParameterSaveContext> contextCaptor;

    private InheritedParameter parameter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ParameterServiceFast  parameterServiceFast = new ParameterServiceFast(
                new TransactionTemplateMock());
        parameterServiceFast.setParameterAuditService(parameterAuditService);
        parameterServiceFast.setParameterLoader(parameterLoaderService);
        parameterServiceFast.setParameterDAO(parameterDAO);

        ParameterServiceReader parameterServiceReader
                = new ParameterServiceReader(tovarTree, null);
        parameterServiceReader.setParameterDAO(parameterDAO);
        parameterServiceReader.setParameterLoader(parameterLoaderService);
        parameterServiceReader.setParameterDAO(parameterDAO);
        parameterServiceReader.setParameterValidationService(parameterValidationService);

        parameterService.setParameterDAO(parameterDAO);
        parameterService.setParameterLoader(parameterLoaderService);
        parameterService.setParameterAuditService(parameterAuditService);
        parameterService.setParameterValidationService(parameterValidationService);
        parameterService.setParameterServiceReader(parameterServiceReader);
        categoryParametersService.setParameterService(
                new ParameterService(parameterService,
                        parameterServiceFast,
                        parameterServiceReader));

        CategoryEntities categoryEntities = Mockito.mock(CategoryEntities.class);

        parameter = Mockito.mock(InheritedParameter.class);
        Mockito.when(parameter.getId()).thenReturn(1L);
        Mockito.when(parameter.getName()).thenReturn("name");
        Mockito.when(parameter.getType()).thenReturn(Param.Type.ENUM);
        Mockito.when(parameter.getXslName()).thenReturn(XslNames.VENDOR);
        Mockito.when(parameter.getAccess()).thenReturn(ParamOptionsAccessType.SIMPLE);
        Mockito.when(categoryEntities.getParameterById(anyLong())).thenReturn(parameter);
        Mockito.when(parameterLoaderService.loadCategoryEntitiesByHid(anyLong())).thenReturn(categoryEntities);
        Mockito.when(parameterLoaderService.loadParameter(anyLong(), anyLong())).thenReturn(parameter);
        Mockito.when(parameterLoaderService.loadParameter(anyLong())).thenReturn(parameter);

        GlobalVendor globalVendor = new GlobalVendor();
        globalVendor.setNames(Collections.singletonList(WordUtil.defaultWord("default name")));
        Mockito.when(globalVendorService.loadVendor(anyLong())).thenReturn(globalVendor);

        TovarTree tree = Mockito.mock(TovarTree.class);
        Mockito.when(tree.findByHid(anyLong())).thenReturn(new TovarCategoryNode(new TovarCategory()));
        Mockito.when(tovarTree.loadSchemeWholeTree()).thenReturn(tree);

        Mockito.doAnswer(i -> {
            Runnable runnable = i.getArgument(1);
            runnable.run();
            return null;
        }).when(parameterDAO).doWithLock(anyCollection(), any(Runnable.class));
    }

    @Test
    public void testOverrideOptions() {
        MboParameters.OverrideOptionsRequest request = MboParameters.OverrideOptionsRequest.newBuilder()
            .setParamId(1L)
            .setCategoryId(1L)
            .setUserId(1L)
            .addOptions(
                MboParameters.OverrideOptionInfo.newBuilder()
                    .setActive(true)
                    .setOptionId(1L)
                    .addName(MboParameters.Word.newBuilder()
                        .setLangId(225)
                        .setName("option")
                        .build()
                    )
            ).build();

        categoryParametersService.overrideOptions(request);

        Mockito.verify(parameterAuditService).optionCreate(contextCaptor.capture(), any(), any());
        Assertions.assertThat(contextCaptor.getValue()).satisfies(context ->
            Assertions.assertThat(context.isBilledOperation()).isFalse());
    }

    @Test
    public void testUpdateOption() {
        OptionImpl option = new OptionImpl();
        option.setId(1);
        Mockito.when(parameter.getOptions()).thenReturn(Collections.singletonList(option));
        MboParameters.UpdateOptionRequest request = MboParameters.UpdateOptionRequest.newBuilder()
            .setParamId(1L)
            .setCategoryId(1L)
            .setUserId(1L)
            .setOptionId(1L)
            .addAlias(MboParameters.EnumAlias.newBuilder()
                .setType(MboParameters.EnumAlias.Type.GENERAL)
                .setAlias(MboParameters.Word.newBuilder()
                    .setLangId(225)
                    .setName("option")
                    .build())
                .build())
            .build();

        categoryParametersService.updateOption(request);

        Mockito.verify(parameterAuditService).optionUpdate(contextCaptor.capture(), any(), any(), any());
        Assertions.assertThat(contextCaptor.getValue()).satisfies(context ->
            Assertions.assertThat(context.isBilledOperation()).isFalse());
    }

    @Test
    public void testOverrideOptionsUsesSourceFromRequest() {
        MboParameters.OverrideOptionsRequest request = MboParameters.OverrideOptionsRequest.newBuilder()
            .setParamId(1L)
            .setCategoryId(1L)
            .setUserId(1L)
            .setSource(MboAudit.Source.YANG_TASK)
            .addOptions(
                MboParameters.OverrideOptionInfo.newBuilder()
                    .setActive(true)
                    .setOptionId(1L)
                    .addName(MboParameters.Word.newBuilder()
                        .setLangId(225)
                        .setName("option")
                        .build()
                    )
            ).build();

        categoryParametersService.overrideOptions(request);

        Mockito.verify(parameterAuditService).optionCreate(contextCaptor.capture(), any(), any());
        Assertions.assertThat(contextCaptor.getValue()).satisfies(context ->
            Assertions.assertThat(context.getSource()).isEqualTo(AuditAction.Source.YANG_TASK));
    }

    @Test
    public void testUpdateOptionUsesSourceFromRequest() {
        OptionImpl option = new OptionImpl();
        option.setId(1);
        Mockito.when(parameter.getOptions()).thenReturn(Collections.singletonList(option));
        MboParameters.UpdateOptionRequest request = MboParameters.UpdateOptionRequest.newBuilder()
            .setParamId(1L)
            .setCategoryId(1L)
            .setUserId(1L)
            .setOptionId(1L)
            .setSource(MboAudit.Source.YANG_TASK)
            .addAlias(MboParameters.EnumAlias.newBuilder()
                .setType(MboParameters.EnumAlias.Type.GENERAL)
                .setAlias(MboParameters.Word.newBuilder()
                    .setLangId(225)
                    .setName("option")
                    .build())
                .build())
            .build();

        categoryParametersService.updateOption(request);

        Mockito.verify(parameterAuditService).optionUpdate(contextCaptor.capture(), any(), any(), any());
        Assertions.assertThat(contextCaptor.getValue()).satisfies(context ->
            Assertions.assertThat(context.getSource()).isEqualTo(AuditAction.Source.YANG_TASK));
    }
}
