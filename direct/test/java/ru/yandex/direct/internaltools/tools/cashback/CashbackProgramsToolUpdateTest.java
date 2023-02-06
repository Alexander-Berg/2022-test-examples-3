package ru.yandex.direct.internaltools.tools.cashback;

import java.math.BigDecimal;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.cashback.model.CashbackCategory;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgram;
import ru.yandex.direct.core.entity.cashback.service.CashbackProgramsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.steps.CashbackSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;
import ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramKey;
import ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramsParams;
import ru.yandex.direct.internaltools.tools.cashback.model.InternalToolsCashbackProgram;
import ru.yandex.direct.internaltools.tools.cashback.service.CashbackClientsWriteService;
import ru.yandex.direct.internaltools.tools.cashback.tool.CashbackProgramsTool;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.cashback.model.CashbackHistoryStateChange.IN;
import static ru.yandex.direct.core.entity.cashback.model.CashbackHistoryStateChange.OUT;
import static ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramsParams.Action.CREATE;
import static ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramsParams.Action.UPDATE;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConverter.getCategoryKey;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CashbackProgramsToolUpdateTest {

    @Autowired
    public CashbackProgramsTool tool;

    @Autowired
    public CashbackClientsWriteService clientsService;

    @Autowired
    public CashbackProgramsService programsService;

    @Autowired
    public CashbackSteps steps;

    @Autowired
    public UserSteps userSteps;

    private CashbackCategory category;

    private User operator;

    @Before
    public void before() {
        steps.createTechnicalEntities();
        category = new CashbackCategory()
                .withNameRu("Тест")
                .withNameEn("Test")
                .withDescriptionRu("Тестовое описание")
                .withDescriptionEn("Test description");
        steps.createCategory(category);

        operator = userSteps.createDefaultUser().getUser();
    }

    @After
    public void after() {
        steps.clear();
    }

    @Test
    public void publicDisabledToPrivateDisabled() {
        var createParams = defaultCreateParams()
                .withIsPublic(true)
                .withIsEnabled(false);
        var createResult = tool.process(createParams);
        var updateParams = resultToUpdateParams(createResult)
                .withIsPublic(false)
                .withIsEnabled(false);
        var programId = extractProgramIdFromResult(createResult);

        tool.process(updateParams);

        // Как "Публичная" эта программа уже выключена, в истории публичной программы должен быть выход
        var state = clientsService.getPublicProgramState(programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isFalse();

        var history = clientsService.collectPublicProgramHistory(programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(OUT);

        // Проверим, что сама программа при этом не публичная и выключена в БД
        var updatedProgram = programsService.getProgramById(programId);
        assertThat(updatedProgram).isNotNull();
        assertThat(updatedProgram.getIsEnabled()).isFalse();
        assertThat(updatedProgram.getIsPublic()).isFalse();
    }

    @Test
    public void publicDisabledToPrivateEnabled() {
        var createParams = defaultCreateParams()
                .withIsPublic(true)
                .withIsEnabled(false);
        var createResult = tool.process(createParams);
        var updateParams = resultToUpdateParams(createResult)
                .withIsPublic(true)
                .withIsEnabled(true);
        var programId = extractProgramIdFromResult(createResult);

        tool.process(updateParams);

        var state = clientsService.getPublicProgramState(programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isTrue();

        var history = clientsService.collectPublicProgramHistory(programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(IN);
    }

    @Test
    public void publicEnabledToPublicDisabled() {
        var createParams = defaultCreateParams()
                .withIsPublic(true)
                .withIsEnabled(true);
        var createResult = tool.process(createParams);
        var updateParams = resultToUpdateParams(createResult)
                .withIsPublic(true)
                .withIsEnabled(false);
        var programId = extractProgramIdFromResult(createResult);

        tool.process(updateParams);

        var state = clientsService.getPublicProgramState(programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isFalse();

        var history = clientsService.collectPublicProgramHistory(programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(OUT);
    }

    @Test
    public void publicEnabledToPrivateEnabled() {
        var createParams = defaultCreateParams()
                .withIsPublic(true)
                .withIsEnabled(true);
        var createResult = tool.process(createParams);
        var updateParams = resultToUpdateParams(createResult)
                .withIsPublic(false)
                .withIsEnabled(true);
        var programId = extractProgramIdFromResult(createResult);

        tool.process(updateParams);

        // Как "Публичная" эта программа уже выключена, в истории публичной программы должен быть выход
        var state = clientsService.getPublicProgramState(programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isFalse();

        var history = clientsService.collectPublicProgramHistory(programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(OUT);

        // Проверим, что сама программа при этом включена в БД
        var updatedProgram = programsService.getProgramById(programId);
        assertThat(updatedProgram).isNotNull();
        assertThat(updatedProgram.getIsEnabled()).isTrue();
        assertThat(updatedProgram.getIsPublic()).isFalse();
    }

    @Test
    public void privateDisabledToPublicDisabled() {
        var createParams = defaultCreateParams()
                .withIsPublic(false)
                .withIsEnabled(false);
        var createResult = tool.process(createParams);
        var updateParams = resultToUpdateParams(createResult)
                .withIsPublic(true)
                .withIsEnabled(false);
        var programId = extractProgramIdFromResult(createResult);
        addOperatorToDefaultProgram(programId);

        tool.process(updateParams);

        var state = clientsService.getProgramState(operator.getClientId(), programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isFalse();

        var history = clientsService.collectProgramHistory(operator.getClientId(), programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(OUT);
    }

    @Test
    public void privateDisabledToPrivateEnabled() {
        var createParams = defaultCreateParams()
                .withIsPublic(false)
                .withIsEnabled(false);
        var createResult = tool.process(createParams);
        var updateParams = resultToUpdateParams(createResult)
                .withIsPublic(false)
                .withIsEnabled(true);
        var programId = extractProgramIdFromResult(createResult);
        addOperatorToDefaultProgram(programId);

        tool.process(updateParams);

        // В пользовательских настройках программа будет включена
        var state = clientsService.getProgramState(operator.getClientId(), programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isTrue();

        var history = clientsService.collectProgramHistory(operator.getClientId(), programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(IN);

        // И сама программа тоже будет включена
        var updatedProgram = programsService.getProgramById(programId);
        assertThat(updatedProgram).isNotNull();
        assertThat(updatedProgram.getIsPublic()).isFalse();
        assertThat(updatedProgram.getIsEnabled()).isTrue();
    }

    @Test
    public void privateEnabledToPrivateDisabled() {
        var createParams = defaultCreateParams()
                .withIsPublic(false)
                .withIsEnabled(true);
        var createResult = tool.process(createParams);
        var updateParams = resultToUpdateParams(createResult)
                .withIsPublic(false)
                .withIsEnabled(false);
        var programId = extractProgramIdFromResult(createResult);
        addOperatorToDefaultProgram(programId);

        tool.process(updateParams);

        // В пользовательских настройках программа будет выключена
        var state = clientsService.getProgramState(operator.getClientId(), programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isFalse();

        var history = clientsService.collectProgramHistory(operator.getClientId(), programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(OUT);

        // И сама программа тоже будет выключена
        var updatedProgram = programsService.getProgramById(programId);
        assertThat(updatedProgram).isNotNull();
        assertThat(updatedProgram.getIsPublic()).isFalse();
        assertThat(updatedProgram.getIsEnabled()).isFalse();
    }

    @Test
    public void privateEnabledToPublicDisabled() {
        var createParams = defaultCreateParams()
                .withIsPublic(false)
                .withIsEnabled(true);
        var createResult = tool.process(createParams);
        var updateParams = resultToUpdateParams(createResult)
                .withIsPublic(true)
                .withIsEnabled(false);
        var programId = extractProgramIdFromResult(createResult);
        addOperatorToDefaultProgram(programId);

        tool.process(updateParams);

        // В пользовательских настройках программа будет выключена
        var state = clientsService.getProgramState(operator.getClientId(), programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isFalse();

        var history = clientsService.collectProgramHistory(operator.getClientId(), programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(OUT);

        // А сама программа станет выключенной публичной
        var updatedProgram = programsService.getProgramById(programId);
        assertThat(updatedProgram).isNotNull();
        assertThat(updatedProgram.getIsPublic()).isTrue();
        assertThat(updatedProgram.getIsEnabled()).isFalse();
    }

    @Test
    public void complexCase() {
        // Создадим приватную программу и добавим туда пользоваателя
        var createParams = defaultCreateParams()
                .withIsPublic(false)
                .withIsEnabled(true);
        var createResult = tool.process(createParams);
        var programId = extractProgramIdFromResult(createResult);
        addOperatorToDefaultProgram(programId);

        // Сделаем программу публичной выключенной
        var updateParams = resultToUpdateParams(createResult)
                .withIsPublic(true)
                .withIsEnabled(false);
        tool.process(updateParams);

        // Теперь у добавленного пользователя программа выключена
        var state = clientsService.getProgramState(operator.getClientId(), programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isFalse();

        var history = clientsService.collectProgramHistory(operator.getClientId(), programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(OUT);

        // Включим программу
        updateParams
                .withIsPublic(true)
                .withIsEnabled(true);
        tool.process(updateParams);

        // Программа включена на всех
        state = clientsService.getPublicProgramState(programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isTrue();

        history = clientsService.collectPublicProgramHistory(programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(IN);

        // Выключим её опять
        updateParams
                .withIsPublic(true)
                .withIsEnabled(false);
        tool.process(updateParams);

        // Программа выключена для всех
        state = clientsService.getPublicProgramState(programId);
        assertThat(state).isNotNull();
        assertThat(state.getIsEnabled()).isFalse();

        history = clientsService.collectPublicProgramHistory(programId);
        assertThat(history).isNotEmpty();
        assertThat(history.get(history.size() - 1).getStateChange()).isEqualTo(OUT);
    }

    private CashbackProgram defaultProgram() {
        return new CashbackProgram()
                .withCategoryId(category.getId())
                .withCategoryNameRu(category.getNameRu())
                .withCategoryNameEn(category.getNameEn())
                .withCategoryDescriptionRu(category.getDescriptionRu())
                .withCategoryDescriptionEn(category.getDescriptionEn())
                .withPercent(new BigDecimal("0.125"))
                .withIsPublic(true)
                .withIsEnabled(true)
                .withIsTechnical(false)
                .withIsNew(false);
    }

    private CashbackProgramsParams defaultCreateParams() {
        var defaultProgram = defaultProgram();
        var params = new CashbackProgramsParams()
                .withAction(CREATE)
                .withCategoryKey(getCategoryKey(defaultProgram.getCategoryId(), defaultProgram.getCategoryNameRu()))
                .withPercent("12.5")
                .withIsEnabled(defaultProgram.getIsEnabled())
                .withIsPublic(defaultProgram.getIsPublic())
                .withIsTechnical(defaultProgram.getIsTechnical())
                .withIsNew(defaultProgram.getIsNew());
        params.setOperator(requireNonNull(operator));
        return params;
    }

    private Long extractProgramIdFromResult(InternalToolMassResult<InternalToolsCashbackProgram> result) {
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
        return result.getData().get(0).getId();
    }

    private CashbackProgramsParams resultToUpdateParams(InternalToolMassResult<InternalToolsCashbackProgram> result) {
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
        var resultData = result.getData().get(0);
        var program = new CashbackProgram()
                .withId(resultData.getId())
                .withCategoryNameRu(resultData.getCategoryName())
                .withPercent(resultData.getPercent())
                .withIsEnabled(resultData.getIsEnabled())
                .withIsPublic(resultData.getIsPublic())
                .withIsTechnical(resultData.getIsTechnical())
                .withIsNew(resultData.getIsNew());
        var params = new CashbackProgramsParams()
                .withAction(UPDATE)
                .withProgramKey(CashbackProgramKey.fromCashbackProgram(program).toDisplayString())
                .withIsPublic(resultData.getIsPublic())
                .withIsEnabled(resultData.getIsEnabled())
                .withIsTechnical(resultData.getIsTechnical())
                .withIsNew(resultData.getIsNew());
        params.setOperator(operator);
        return params;
    }

    private void addOperatorToDefaultProgram(Long programId) {
        clientsService.addClientsToProgram(operator, defaultProgram().withId(programId),
                List.of(operator.getClientId()));
    }
}
