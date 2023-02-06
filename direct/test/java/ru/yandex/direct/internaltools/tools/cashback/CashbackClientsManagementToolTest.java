package ru.yandex.direct.internaltools.tools.cashback;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.cashback.model.CashbackCategory;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgram;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.steps.CashbackSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.cashback.model.ManageClientsCashbackParams;
import ru.yandex.direct.internaltools.tools.cashback.repository.InternalToolsCashbackClientsRepository;
import ru.yandex.direct.internaltools.tools.cashback.tool.CashbackClientsManageTool;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramKey.fromCashbackProgram;
import static ru.yandex.direct.internaltools.tools.cashback.model.ManageClientsCashbackParams.Action.ADD;
import static ru.yandex.direct.internaltools.tools.cashback.model.ManageClientsCashbackParams.Action.REMOVE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CashbackClientsManagementToolTest {

    @Autowired
    private CashbackClientsManageTool tool;

    @Autowired
    private InternalToolsCashbackClientsRepository readRepository;

    @Autowired
    private CashbackSteps cashbackSteps;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private DslContextProvider dslContextProvider;

    private CashbackCategory category;
    private CashbackProgram program;

    private User user;

    @Before
    public void before() {
        cashbackSteps.createTechnicalEntities();
        category = new CashbackCategory()
                .withNameRu("Тест")
                .withNameEn("Test")
                .withDescriptionRu("Тестовое описание")
                .withDescriptionEn("Test description");
        cashbackSteps.createCategory(category);

        program = new CashbackProgram()
                .withPercent(BigDecimal.ONE)
                .withIsEnabled(true)
                .withIsPublic(false)
                .withCategoryId(category.getId())
                .withCategoryNameRu(category.getNameRu());
        cashbackSteps.createProgram(program);

        user = userSteps.createDefaultUser().getUser();
    }

    @After
    public void after() {
        cashbackSteps.clear();
    }

    @Test
    public void validateSuccess() {
        var vr = tool.validate(defaultParams());
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_programIsSelected() {
        var params = defaultParams().withProgramKey(null);
        var vr = tool.validate(params);

        var expected = validationError(path(field("program key")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_programExists() {
        var program = new CashbackProgram()
                .withId(1234L)
                .withPercent(BigDecimal.ONE)
                .withIsPublic(true)
                .withIsEnabled(true)
                .withCategoryNameRu(category.getNameRu());
        var params = defaultParams()
                .withProgramKey(fromCashbackProgram(program).toDisplayString());
        var vr = tool.validate(params);

        var expected = validationError(path(field("program")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void addToProgram() {
        var result = tool.process(defaultParams());

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getMessage()).isEqualTo("Клиенты добавлены в программу");

        var clientIdsInDb = collectClientIdsInProgram();
        assertThat(clientIdsInDb).contains(user.getClientId());
    }

    @Test
    public void removeFromProgram() {
        tool.process(defaultParams()); // Сначала добавим в программу
        var params = defaultParams().withAction(REMOVE);
        var result = tool.process(params);

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getMessage()).isEqualTo("Клиенты удалены из программы");

        var clientIdsInDb = collectClientIdsInProgram();
        assertThat(clientIdsInDb).isEmpty();
    }

    private List<ClientId> collectClientIdsInProgram() {
        List<ClientId> result = new ArrayList<>();
        shardHelper.forEachShard(shard -> result.addAll(
                readRepository.collectClientsByProgramId(dslContextProvider.ppc(shard), program.getId(), true)));
        return result;
    }

    private ManageClientsCashbackParams defaultParams() {
        var params = new ManageClientsCashbackParams()
                .withAction(ADD)
                .withClientIds(user.getClientId().toString())
                .withProgramKey(fromCashbackProgram(program).toDisplayString());
        params.setOperator(user);
        return params;
    }
}
