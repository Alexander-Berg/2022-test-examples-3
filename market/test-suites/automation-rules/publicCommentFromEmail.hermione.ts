import {login} from '../../helpers';
import Autocomplete from '../../page-objects/autocomplete';
import Dropdown from '../../page-objects/dropdown';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import CodeEditor from '../../page-objects/codeEditor';
import Button from '../../page-objects/button';
import {checkSavedConfig} from './helpers/checkSavedConfig';
import {archiveAutomationRule} from './helpers/archiveAutomationRule';

const PAGE_URL = '/entity/automationRule$event/automationRule$event/create?parent=service%4030013907';
const FROM_EMAIL_VALUE = 'test@test.ru';
const COMMENT_TEXT_VALUE = 'test';
const TITLE = 'Автотест ocrm-1680';
const EXPECTED_CONFIG =
    '1\n{\n2\n  "rules": [\n3\n    {\n4\n      "actions": [\n5\n        {\n6\n          "type": "edit",\n7\n          "parameter": "ticket",\n8\n          "properties": [\n9\n            {\n10\n              "code": "@comment",\n11\n              "value": {\n12\n                "type": "raw",\n13\n                "value": {\n14\n                  "body": "<p>test</p>",\n15\n                  "fromEmail": "test@test.ru",\n16\n                  "metaclass": "comment$public"\n17\n                }\n18\n              }\n19\n            }\n20\n          ]\n21\n        }\n22\n      ]\n23\n    }\n24\n  ]\n25\n}';

/**
 * Проверяем, что во внешнем комментарии при создании правила автоматизации можно указать обратный адрес
 */
describe(`ocrm-1680: Правила автоматизации - для внешнего комментария можно указать обратный адрес`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('Проверяет, что указание адреса доступно и конфигурация сохраняется', async function() {
        const title = new ContentWithLabel(this.browser, 'body', '[data-ow-test-properties-list-attribute="title"]');

        const addNewRuleButton = new Button(this.browser, 'body', '[data-ow-test-add-new-rule]');

        const ruleGroup = new Autocomplete(
            this.browser,
            'body',
            '[data-ow-test-properties-list-attribute="ruleGroup"]'
        );
        const addNewAction = new Dropdown(
            this.browser,
            'body',
            '[data-ow-test-add-button-dropdown="Добавить действие"]'
        );

        const sourceEditObject = new Autocomplete(this.browser, 'body', '[data-ow-test-rule-action-edit]');

        const config = new CodeEditor(
            this.browser,
            '[data-ow-test-properties-list-attribute="config"]',
            '[data-ow-test-code-editor="json"]'
        );

        await title.isDisplayed();
        await title.setValue(TITLE);

        await ruleGroup.isDisplayed();
        await ruleGroup.selectSingleItem('Изменение обращения');
        await addNewRuleButton.clickButton();

        await addNewAction.isDisplayed();
        await addNewAction.selectItem('[data-ow-test-action="Редактирование объекта"]');

        await sourceEditObject.isDisplayed();
        await sourceEditObject.selectSingleItem('Обращение (ticket)');

        const addCommentButton = await this.browser.$('button=Добавить комментарий');

        await addCommentButton.isEnabled();
        await addCommentButton.click();

        const fromEmailInputParentElement = await this.browser.$('span=Обратный адрес').parentElement();
        const fromEmailInput = await fromEmailInputParentElement.$('input');

        await fromEmailInput.isDisplayed();
        await fromEmailInput.addValue(FROM_EMAIL_VALUE);

        const editor = await this.browser.$(`[data-ow-test-comment-editor] [contenteditable="true"]`);

        await editor.isDisplayed();
        await editor.click();
        await editor.addValue(COMMENT_TEXT_VALUE);

        const saveButton = new Button(this.browser, 'body', '[data-ow-test-jmf-card-toolbar-action="save-добавить"]');

        await saveButton.clickButton();

        await config.isDisplayed();

        await checkSavedConfig(this.browser, EXPECTED_CONFIG);

        await archiveAutomationRule(this.browser);
    });
});
