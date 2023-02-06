import {expect} from 'chai';

import {login} from '../../helpers';
import Autocomplete from '../../page-objects/autocomplete';
import Button from '../../page-objects/button';
import Dropdown from '../../page-objects/dropdown';

const PAGE_URL_1 = '/entity/automationRule$event/automationRule$event/create?parent=service%40388349901';

/**
 * Проверяем, что при выборе соответствующих атрибутов при создании правил автоматизации
 * появляется поле для ввода JSON
 */
describe(`ocrm-1583: Правила автоматизации - выбор атрибутов типа JSON`, () => {
    beforeEach(function() {
        return login(PAGE_URL_1, this);
    });
    it('работает корректно', async function() {
        const ruleGroup = new Autocomplete(
            this.browser,
            'body',
            '[data-ow-test-properties-list-attribute="ruleGroup"]'
        );
        const addNewRuleButton = new Button(this.browser, 'body', '[data-ow-test-add-new-rule]');
        const addNewCondition = new Dropdown(
            this.browser,
            'body',
            '[data-ow-test-add-button-dropdown="Добавить условие"]'
        );

        const predicateSourceObject = new Autocomplete(
            this.browser,
            'body',
            '[data-ow-test-rule-predicate-edit="source-object"]'
        );

        const predicateAttributePath = new Autocomplete(
            this.browser,
            'body',
            '[data-ow-test-rule-predicate-edit="attribute"]'
        );

        const thenPredicate = new Dropdown(
            this.browser,
            '[data-ow-test-rule-predicate-edit="positive-brunch"]',
            '[data-ow-test-add-button-dropdown="Добавить действие"]'
        );

        const thenActionSourceObject = new Autocomplete(
            this.browser,
            '[data-ow-test-rule-predicate-edit="positive-brunch"]',
            '[data-ow-test-rule-action-edit]'
        );

        await ruleGroup.isDisplayed();
        await ruleGroup.selectSingleItem('Изменение обращения');

        await addNewRuleButton.isDisplayed();
        await addNewRuleButton.clickButton();

        const addNewConditionBlockButton = await this.browser.$('span=Добавить новый блок с условием');

        await addNewConditionBlockButton.isDisplayed();
        await addNewConditionBlockButton.click();

        await addNewCondition.isDisplayed();
        await addNewCondition.selectItem('div=Проверка значения атрибута');

        await predicateSourceObject.isDisplayed();
        await predicateSourceObject.selectSingleItem('Обращение (ticket)');
        await predicateAttributePath.isDisplayed();
        await predicateAttributePath.selectSingleItem('Результат ассесмента (assessmentResult)');

        const codeEditor = await this.browser.$('[data-ow-test-code-editor="json"]');
        const codeEditorIsDisplayed = await codeEditor.isDisplayed();

        expect(codeEditorIsDisplayed).to.equal(true, 'Поле для ввода JSON не появилось');

        await thenPredicate.isDisplayed();
        await thenPredicate.selectItem('[data-ow-test-action="Редактирование объекта"]');

        await thenActionSourceObject.isDisplayed();
        await thenActionSourceObject.selectSingleItem('Обращение (ticket)');

        const addAttributeButton = await this.browser.$('span=Добавить атрибут');

        await addAttributeButton.isDisplayed();
        await addAttributeButton.click();
        const thenActionAttributePath = new Autocomplete(
            this.browser,
            '[data-ow-test-rule-predicate-edit="positive-brunch"]',
            '[data-ow-test-rule-action-edit="properties"]'
        );

        await thenActionAttributePath.isDisplayed();
        await thenActionAttributePath.selectSingleItem('Результат ассесмента (assessmentResult)');

        const thenCodeEditor = await this.browser.$(
            '[data-ow-test-rule-predicate-edit="positive-brunch"] [data-ow-test-code-editor="json"]'
        );
        const thenCodeEditorIsDisplayed = await thenCodeEditor.isDisplayed();

        expect(thenCodeEditorIsDisplayed).to.equal(true, 'Поле для ввода JSON в блоке "Тогда" не появилось');
    });
});
