package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.enumaliases.OpenEnumValueAliasesEditorEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.save.PopulateModelSaveSyncEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableValue;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableValues;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractModelTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.EnumValueAliasesEditorStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.EnumValueAlias;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mbo.gwt.models.modelstorage.CommonModelUtils.getBaseModel;

/**
 * Данный тест покрывает различные сценарии взаимодействия пользователя с ENUM-значениями и, главное, их алиасами на
 * странице редактирования SKU. Тезисно касательно текущей реализации:
 * <ul>
 *     <li>
 *         Все алиасы представляют собой пару [Опция] - [Алиасная опция]. Обе опции
 *         берутся из списка всех опций данного CategoryParam. Кроме этой пары значений алиас хранит некоторую
 *         вспомогательную информацию, вроде ИД параметра, даты изменения и т.д.
 *     </li>
 *     <li>
 *         Пользователь может зайти на страницу редактирования SKU, выбрать некоторые значения у интересующего нас
 *         параметра и назначить этим значениям алиасы. Например, для параметра <i>Цвет товара</i> можно выбрать
 *         опцию "Оранжевый" и конкретно этой опции присвоить алиас "Рыжий".
 *     </li>
 *     <li>
 *         Все алиасы хранятся в родительской GURU-модели. Все конкретные опции SKU хранятся в соответствующих SKU. В
 *         примере с Оранжевым цветом получается, что опция "Оранжевый" хранится на конкретной SKU, а алиас
 *         [Оранжевый] - [Рыжий] - в GURU-модели.
 *     </li>
 *     <li>
 *         Возникает задача поддержки консистентности выбранных в различных SKU опций и хранимых в их общей GURU-модели
 *         алиасов. За эту целостность отвечает
 *         {@link ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.EnumValuesAliasesAddon}
 *         и валидаторы.
 *     </li>
 *     <li>
 *         Доступные значения в полях ввода определяются только выбранными в соседних полях опциями (алиасы не влияют).
 *     </li>
 *     <li>
 *         Если пользователь добавляет алиасы в попапе, и значения алиасов пересекаются с другими алиасами, вылезет
 *         попап с предложением убрать найденные алиасы.
 *     </li>
 *     <li>
 *         Если пользователь удаляет опцию со SKU, то мы по умолчанию считаем, что его не интересует судьба алиасов
 *         и он просто хочет убрать значение и всё. Для нас это значит, что мы оставляем алиасы как есть.
 *     </li>
 *     <li>
 *         Если пользователь добавляет или переименовывает опцию так, что она становится равна чьему-то алиасу, то мы
 *         спрашиваем юзера, хочет ли он удалить алиас ради этой опции, или же алиас стоит оставить и опцию не выбирать.
 *     </li>
 * </ul>
 */
public class SkuEnumAliasesTest extends AbstractModelTest {

    private static final long CATEGORY_ID = 93956L;
    private static final long GURU_ID = 1L;
    private static final long SKU_ID1 = 2L;
    private static final long PARAM_ID = 42L;
    private static final int RANDOM_ID = 1505;

    //Ответы на вопрос при удалении единичного алиаса
    private static final boolean YES_REMOVE_IT = true;
    private static final boolean NO_KEEP_IT = false;

    //Опции и алиасы
    private static final long GREEN = 19001L;
    private static final long RED = 19002L;
    private static final long GREEN_APPLE = 19003L;
    private static final long OLIVE = 19004L;
    private static final long SCARLET = 19005L;

    private EditableValues editableValues;
    private CommonModel guruModel;
    private CommonModel currentSku;

    @Before
    public void instantiateModel() {
        generateModelsWithMultivalue();
        editableValues = editableValuesOf(editableModel);
        //Эмуляция userAction = true в методах, вызывающих консумеры. В них при юзер-экшене должен быть рефреш
        editableValues.addEditableValueRemovedConsumer(editableValue -> editableValues.refreshOptions());
        editableValues.addEditableValueAddedConsumer(editableValue -> editableValues.refreshOptions());
    }

    @Test
    public void testOptionsAdded() {
        //Добавили зелёный цвет
        setValueNo(0, GREEN);
        //Добавили красный цвет
        setValueNo(1, RED);

        //прверяем, что может или не может показываться в дропдауне (садджесте)
        validateNotAvailable(GREEN, RED);
        validateAvailable(GREEN_APPLE, OLIVE, SCARLET);

        //Сохраняем
        save();

        //Проверяем
        List<Long> savedOptions = optionsOf(currentSku);
        assertEquals(2, savedOptions.size());
        assertTrue(savedOptions.contains(GREEN) && savedOptions.contains(RED));
    }

    @Test
    public void testOptionsAndAliasesAdded() {
        //Добавили зелёный цвет и алиасы: Зелёное яблоко и Оливковый
        setValueNo(0, GREEN);
        setAliases(0, GREEN, OLIVE, GREEN_APPLE);
        validateNotAvailable(GREEN);
        validateAvailable(RED, SCARLET, OLIVE, GREEN_APPLE);

        //Добавили красный цвет и алиас: Алый
        setValueNo(1, RED);
        setAliases(1, RED, SCARLET);
        validateNotAvailable(GREEN, RED);
        validateAvailable(SCARLET, OLIVE, GREEN_APPLE);

        //Сохраняем и проверяем
        save();
        List<Long> savedOptions = optionsOf(currentSku);
        assertEquals(2, savedOptions.size());
        assertTrue(savedOptions.contains(GREEN) && savedOptions.contains(RED));

        assertAliasExists(guruModel, GREEN, OLIVE);
        assertAliasExists(guruModel, GREEN, GREEN_APPLE);
        assertAliasExists(guruModel, RED, SCARLET);
        validateNotAvailable(GREEN, RED);
        validateAvailable(SCARLET, OLIVE, GREEN_APPLE);
    }

    @Test
    public void testNonSavedOptionRemoved() {
        //Добавили зелёный цвет и алиасы: Зелёное яблоко и Оливковый
        setValueNo(0, GREEN);
        setAliases(0, GREEN, OLIVE, GREEN_APPLE);
        //Добавили красный цвет и алиас: Алый
        setValueNo(1, RED);
        setAliases(1, RED, SCARLET);
        validateNotAvailable(GREEN, RED);
        validateAvailable(OLIVE, GREEN_APPLE, SCARLET);

        //Удаляем красную опцию целиком. Алиасы должны остаться без изменений и не блокировать опции
        dropValueNo(1);
        validateNotAvailable(GREEN);
        validateAvailable(OLIVE, GREEN_APPLE, RED, SCARLET);

        //Сохраняем и проверяем
        save();
        List<Long> savedOptions = optionsOf(currentSku);
        assertEquals(1, savedOptions.size());
        assertTrue(savedOptions.contains(GREEN) && !savedOptions.contains(RED));

        assertAliasExists(guruModel, GREEN, OLIVE);
        assertAliasExists(guruModel, GREEN, GREEN_APPLE);

        validateNotAvailable(GREEN);
        validateAvailable(OLIVE, GREEN_APPLE, RED, SCARLET);
    }

    @Test
    public void testSavedOptionRemoved() {
        //Добавили зелёный цвет и алиасы: Зелёное яблоко и Оливковый
        setValueNo(0, GREEN);
        setAliases(0, GREEN, OLIVE, GREEN_APPLE);

        //Добавили красный цвет и алиас: Алый
        setValueNo(1, RED);
        setAliases(1, RED, SCARLET);

        //Сохраняем и проверяем
        save();
        List<Long> savedOptions = optionsOf(currentSku);
        assertEquals(2, savedOptions.size());
        assertTrue(savedOptions.contains(GREEN) && savedOptions.contains(RED));

        assertAliasExists(guruModel, GREEN, OLIVE);
        assertAliasExists(guruModel, GREEN, GREEN_APPLE);

        validateNotAvailable(GREEN, RED);
        validateAvailable(OLIVE, GREEN_APPLE, SCARLET);

        //Удаляем красную опцию целиком.
        dropValueNo(1);
        validateNotAvailable(GREEN);
        validateAvailable(OLIVE, GREEN_APPLE, SCARLET, RED);

        //Сохраняем и проверяем
        save();
        savedOptions = optionsOf(currentSku);
        assertEquals(1, savedOptions.size());
        assertTrue(savedOptions.contains(GREEN) && !savedOptions.contains(RED));

        assertAliasExists(guruModel, GREEN, OLIVE);
        assertAliasExists(guruModel, GREEN, GREEN_APPLE);

        validateNotAvailable(GREEN);
        validateAvailable(OLIVE, GREEN_APPLE, RED, SCARLET);
    }

    @Test
    public void testNonSavedAliasRemoved() {
        //Добавили зелёный цвет и алиасы: Зелёное яблоко и Оливковый
        setValueNo(0, GREEN);
        setAliases(0, GREEN, OLIVE, GREEN_APPLE);
        validateNotAvailable(GREEN);
        validateAvailable(RED, SCARLET, OLIVE, GREEN_APPLE);

        //Удалим оливковый алиас
        setAliases(0, GREEN, GREEN_APPLE);
        validateNotAvailable(GREEN);
        validateAvailable(RED, SCARLET, OLIVE, GREEN_APPLE);

        //Сохраняем и проверяем
        save();
        List<Long> savedOptions = optionsOf(currentSku);
        assertEquals(1, savedOptions.size());
        assertTrue(savedOptions.contains(GREEN) && !savedOptions.contains(RED));

        assertAliasExists(guruModel, GREEN, GREEN_APPLE);
    }

    @Test
    public void testSavedAliasRemoved() {
        //Добавили зелёный цвет и алиасы: Зелёное яблоко и Оливковый
        setValueNo(0, GREEN);
        setAliases(0, GREEN, OLIVE, GREEN_APPLE);

        //Сохраняем и проверяем
        save();
        List<Long> savedOptions = optionsOf(currentSku);
        assertEquals(1, savedOptions.size());
        assertTrue(savedOptions.contains(GREEN) && !savedOptions.contains(RED));

        assertAliasExists(guruModel, GREEN, GREEN_APPLE);
        assertAliasExists(guruModel, GREEN, OLIVE);

        //Удалим алиас
        setAliases(0, GREEN, GREEN_APPLE);
        assertAliasExists(guruModel, GREEN, GREEN_APPLE);
        validateNotAvailable(GREEN);
        validateAvailable(RED, SCARLET, OLIVE, GREEN_APPLE);

        //Сохраняем и проверяем
        save();
        savedOptions = optionsOf(currentSku);
        assertEquals(1, savedOptions.size());
        assertTrue(savedOptions.contains(GREEN) && !savedOptions.contains(RED));
        assertAliasDoesntExist(guruModel, GREEN, OLIVE);
        assertAliasExists(guruModel, GREEN, GREEN_APPLE);
    }

    @Test
    public void testNewFieldUpdated() {
        //Добавили зелёный цвет и алиасы: Зелёное яблоко и Оливковый
        setValueNo(0, GREEN);
        setAliases(0, GREEN, OLIVE, GREEN_APPLE);

        //Добавили красный цвет и алиас: Алый
        setValueNo(1, RED);
        setAliases(1, RED, SCARLET);

        //Добавим пустое поле и проверим, что в нём недоступны две опции
        editableValues.getOrCreateEditableValue(2);
        validateNotAvailable(GREEN, RED);
        validateAvailable(GREEN_APPLE, OLIVE, SCARLET);

        //Удалим красный и убедимся, что для недавно созданного поля список допустимых значений обновится
        dropValueNo(1);
        validateNotAvailable(GREEN);
        validateAvailable(GREEN_APPLE, OLIVE, SCARLET, RED);
    }

    @Test
    public void testOptionRenamed() {
        //Добавили зелёный цвет и алиас: Оливковый
        setValueNo(0, GREEN);
        setAliases(0, GREEN, OLIVE);

        //Сохраняем и проверяем
        save();
        List<Long> savedOptions = optionsOf(currentSku);
        assertEquals(1, savedOptions.size());
        assertTrue(savedOptions.contains(GREEN) && !savedOptions.contains(RED));

        assertAliasExists(guruModel, GREEN, OLIVE);

        //Переименовываем зелёный в оливковый (т.е. наоборот)
        //должен вылезти попап с вопросом, хотим ли мы оторвать оливковый алиас от зелёного. Говорим "нет".
        letUserSay(NO_KEEP_IT);
        setValueNo(0, OLIVE);
        validateAvailable(RED, SCARLET, GREEN_APPLE, OLIVE);
        validateNotAvailable(GREEN);
        assertAliasExists(guruModel, GREEN, OLIVE);

        //Переименуем ещё раз, но согласимся с удалением алиаса
        letUserSay(YES_REMOVE_IT);
        setValueNo(0, OLIVE);
        validateAvailable(RED, SCARLET, GREEN_APPLE, GREEN);
        validateNotAvailable(OLIVE);
        assertAliasDoesntExist(guruModel, GREEN, OLIVE);

        //Сохраняем и проверяем
        save();
        savedOptions = optionsOf(currentSku);
        assertEquals(1, savedOptions.size());
        assertTrue(savedOptions.contains(OLIVE) && !savedOptions.contains(RED));

        validateAvailable(RED, SCARLET, GREEN_APPLE, GREEN);
        validateNotAvailable(OLIVE);
        assertAliasDoesntExist(guruModel, GREEN, OLIVE);
    }

    @Test
    public void testNewOptionSameAsAlias() {
        //Добавим зелёный + алиас оливковый
        setValueNo(0, GREEN);
        setAliases(0, GREEN, OLIVE);

        validateNotAvailable(GREEN);
        validateAvailable(RED, GREEN_APPLE, OLIVE, SCARLET);

        //Попробуем добавить оливковую опцию, ни на что не соглашаемся
        letUserSay(NO_KEEP_IT);
        setValueNo(1, OLIVE);
        assertAliasExists(guruModel, GREEN, OLIVE);
        validateNotAvailable(GREEN);
        validateAvailable(RED, GREEN_APPLE, OLIVE, SCARLET);

        //Ещё раз, но соглашаемся удалить алиас
        letUserSay(YES_REMOVE_IT);
        setValueNo(1, OLIVE);
        assertAliasDoesntExist(guruModel, GREEN, OLIVE);
        validateAvailable(RED, SCARLET, GREEN_APPLE);
        validateNotAvailable(GREEN, OLIVE);
    }

    @Test
    public void testAddAliasAlreadyInUse() {
        setValueNo(0, GREEN);
        setAliases(0, GREEN, OLIVE);

        //Возьмём уже заюзанный выше алиас. Должно ругнуться и спросить юзера, не удалить ли зелёный-оливковый? Откажем.
        setValueNo(1, GREEN_APPLE);
        letUserSay(NO_KEEP_IT);
        setAliases(1, GREEN_APPLE, OLIVE);
        assertAliasExists(guruModel, GREEN, OLIVE);
        assertAliasDoesntExist(guruModel, GREEN_APPLE, OLIVE);
        validateAvailable(RED, SCARLET, OLIVE);
        validateNotAvailable(GREEN_APPLE, GREEN);

        //Повторим и согласимся
        letUserSay(YES_REMOVE_IT);
        setAliases(1, GREEN_APPLE, OLIVE);
        assertAliasExists(guruModel, GREEN_APPLE, OLIVE);
        assertAliasDoesntExist(guruModel, GREEN, OLIVE);
        validateAvailable(RED, SCARLET, OLIVE);
        validateNotAvailable(GREEN_APPLE, GREEN);
    }

    private void generateModelsWithMultivalue() {
        //Гуру-модель
        guruModel = getAlmostModelWithParams()
            .startModel()
            .id(GURU_ID)
            .category(CATEGORY_ID)
            .source(CommonModel.Source.GURU)
            .currentType(CommonModel.Source.GURU)
            .endModel()
            .getModel();

        //Редактируемая СКУ
        data = getAlmostModelWithParams()
            .startModel()
            .id(SKU_ID1)
            .category(CATEGORY_ID)
            .source(CommonModel.Source.SKU)
            .currentType(CommonModel.Source.SKU)
            .startModelRelation()
            .id(GURU_ID)
            .categoryId(CATEGORY_ID)
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .endModel();
        currentSku = data.getModel();
        currentSku.getRelation(GURU_ID).get().setModel(guruModel);

        editableModel.setOriginalModel(currentSku);
        editableModel.setModel(currentSku);
        rpc.setLoadModel(currentSku, null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=" + SKU_ID1)));
        super.run();
    }

    private ModelDataBuilder getAlmostModelWithParams() {
        return ModelDataBuilder.modelData()
            .startParameters()
            .startParameter()
            .xsl(XslNames.COLOR_VENDOR)
            .type(Param.Type.ENUM)
            .name("Цвет товара")
            .multifield(true)
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .id(PARAM_ID)
            .option(GREEN, "")
            .option(GREEN_APPLE, "")
            .option(OLIVE, "")
            .option(RED, "")
            .option(SCARLET, "")
            .endParameter()
            .endParameters()
            .tovarCategory(RANDOM_ID, RANDOM_ID)
            .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
            .endVendor();
    }

    private void save() {
        rpc.setSaveModel(currentSku.getId(), null);
        //тестовый костыль: во время флаша модели при сохранении создаётся новый объект CommonModel, надо его получить:
        bus.subscribe(PopulateModelSaveSyncEvent.class, event -> {
            currentSku = event.getModel();
            rpc.setLoadModel(currentSku, null);
            guruModel = getBaseModel(currentSku);
        });
        bus.fireEvent(
            new SaveModelRequest(false, false));
    }

    private static void assertAliasExists(CommonModel model, long optionId, long aliasId) {
        List<EnumValueAlias> aliases = model.getEnumValueAliases(PARAM_ID, optionId);
        assertTrue(!aliases.isEmpty());
        Optional<EnumValueAlias> maybeGoodAlias = aliases.stream()
            .filter(eva -> eva.getAliasOptionId() == aliasId)
            .findAny();
        assertTrue(maybeGoodAlias.isPresent());
    }

    private static void assertAliasDoesntExist(CommonModel model, long optionId, long aliasId) {
        List<EnumValueAlias> aliases = model.getEnumValueAliases(PARAM_ID, optionId);
        Optional<EnumValueAlias> maybeBadAlias = aliases.stream()
            .filter(eva -> eva.getAliasOptionId() == aliasId)
            .findAny();
        assertTrue(!maybeBadAlias.isPresent());
    }

    private void validateNotAvailable(long... optionIds) {
        for (EditableValue ev : editableValues.getEditableValues()) {
            List<Option> options =  ev.getOptions();
            for (long idToIgnore : optionIds) {
                //Либо опция должна быть выбрана в виджете, либо должна быть недоступна для выбора руками
                Option selectedOption = (Option) ev.getValueWidget().getValue();
                assertTrue("Option " + idToIgnore + " is available.",
                    (selectedOption != null && selectedOption.getId() == idToIgnore) ||
                    options.stream().noneMatch(opt -> opt.getId() == idToIgnore));
            }
        }
    }

    private void validateAvailable(long... optionIds) {
        for (EditableValue ev : editableValues.getEditableValues()) {
            List<Option> options =  ev.getOptions();
            for (long idToContain : optionIds) {
                //Либо опция должна быть выбрана в виджете, либо должна быть доступна для выбора руками
                Option selectedOption = (Option) ev.getValueWidget().getValue();
                assertTrue("Option " + idToContain + " is not available.",
                    (selectedOption != null && selectedOption.getId() == idToContain) ||
                    options.stream().anyMatch(opt -> opt.getId() == idToContain));
            }
        }
    }

    private EditableValues editableValuesOf(EditableModel model) {
        return model.getEditableParameter(XslNames.COLOR_VENDOR).getEditableValues();
    }

    private List<Long> optionsOf(CommonModel model) {
        return model.getParameterValues(PARAM_ID).getOptionIds();
    }

    private EditableValue editableValueNo(int index) {
        return editableValues.getOrCreateEditableValue(index);
    }

    private ParameterValue parameterValueNo(int index) {
        return editableValueNo(index).getParameterValue();
    }

    private void setValueNo(int index, long value) {
        CategoryParam param = editableValueNo(index).getCategoryParam();
        ParameterValue newParamValue = new ParameterValue(param, value);
        editableValueNo(index).setParameterValue(newParamValue, true);
        editableValues.refreshOptions(); //имитация userAction = true
    }

    private void dropValueNo(int index) {
        editableValues.getValuesWidget().removeValueWidget(index, true);

    }

    private void setAliases(int paramNo, long optionId, long... aliasIds) {
        bus.fireEventSync(new OpenEnumValueAliasesEditorEvent(parameterValueNo(paramNo)));
        EnumValueAliasesEditorStub editor = (EnumValueAliasesEditorStub) view.getDialogWidget();
        editor.getAliases().clear();
        for (long aliasId : aliasIds) {
            editor.getAliases().add(new EnumValueAlias(PARAM_ID, XslNames.COLOR_VENDOR, optionId, aliasId));
        }
        editor.fakeSaveClick();
    }

    private void letUserSay(boolean answer) {
        viewFactory.whenAskedSayThis(answer);
    }
}
