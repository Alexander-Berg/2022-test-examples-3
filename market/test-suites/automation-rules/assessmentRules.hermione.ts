import {expect} from 'chai';

import Button from '../../page-objects/button';
import Autocomplete from '../../page-objects/autocomplete';
import Dropdown from '../../page-objects/dropdown';
import {login, pause, waitForReactRootLoaded} from '../../helpers';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import {checkSavedConfig} from './helpers/checkSavedConfig';
import {archiveAutomationRule} from './helpers/archiveAutomationRule';

const PAGE_URL = '/entity/automationRule$event/automationRule$event/create?parent=service%40136838084';
const TITLE = 'test';
const EXPECTED_CONFIG =
    '1\n{\n2\n  "rules": [\n3\n    {\n4\n      "actions": [\n5\n        {\n6\n          "type": "create",\n7\n          "metaclass": "ticket$beruYouScan",\n8\n          "properties": [\n9\n            {\n10\n              "code": "@sendTicketToAssessment",\n11\n              "value": {\n12\n                "type": "raw",\n13\n                "value": {\n14\n                  "assessmentRule": {\n15\n                    "gid": "assessmentRule@161186084",\n16\n                    "title": "Тестовое правило для регресса (не трогать)",\n17\n                    "archived": false,\n18\n                    "metaclass": "assessmentRule",\n19\n                    "_permissions": {\n20\n                      "@edit": true,\n21\n                      "@view": true\n22\n                    }\n23\n                  },\n24\n                  "assessmentTicketFormatter": "firstComment"\n25\n                }\n26\n              }\n27\n            }\n28\n          ]\n29\n        }\n30\n      ]\n31\n    }\n32\n  ]\n33\n}';

const fillRuleData = async (ctx, sourceObjectName) => {
    const ruleGroup = new Autocomplete(ctx.browser, 'body', '[data-ow-test-properties-list-attribute="ruleGroup"]');
    const addNewRuleButton = new Button(ctx.browser, 'body', '[data-ow-test-add-new-rule]');

    const addNewAction = new Dropdown(ctx.browser, 'body', '[data-ow-test-add-button-dropdown="Добавить действие"]');

    const sourceCreateObject = new Autocomplete(ctx.browser, 'body', '[data-ow-test-rule-action-edit]');
    const title = new ContentWithLabel(ctx.browser, 'body', '[data-ow-test-properties-list-attribute="title"]');

    await title.isDisplayed();
    await title.setValue(TITLE);

    await ruleGroup.isDisplayed();
    await ruleGroup.selectSingleItem('Изменение обращения');

    await addNewRuleButton.isDisplayed();
    await addNewRuleButton.clickButton();

    await addNewAction.isDisplayed();
    await addNewAction.selectItem('[data-ow-test-action="Создание объекта"]');

    await sourceCreateObject.isDisplayed();
    await sourceCreateObject.selectSingleItem(sourceObjectName);
};

/**
 * Проверяем, что блок "Отправить в ассессмент" при создании правила автоматизации
 * доступен только если метакласс вложен в ticket и есть логика assessmentTicket
 */
describe(`ocrm-1690: Правила автоматизации - вызов правил ассессмента`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('работает корректно', async function() {
        await waitForReactRootLoaded(this.browser);

        await fillRuleData(this, 'Упоминание Покупок (ticket$beruYouScan)');

        const addAssessmentBlockButton = this.browser.$('button=Добавить блок отправки в ассессмент');

        const addAssessmentBlockButtonIsDisplayed = await addAssessmentBlockButton.isDisplayed();

        expect(addAssessmentBlockButtonIsDisplayed).to.equal(true, 'Нет кнопки добавления блока отправки в ассессмент');

        await addAssessmentBlockButton.click();

        const assessmentTicketFormatter = new Dropdown(
            this.browser,
            'body',
            '[data-ow-test-rule-assessment-block="formatter"]'
        );
        const assessmentTicketFormatterIsDisplayed = await assessmentTicketFormatter.isDisplayed();

        expect(assessmentTicketFormatterIsDisplayed).to.equal(true, 'Нет дропдауна для выбора формата отправки');

        await assessmentTicketFormatter.selectItem('[data-ow-test-select-option="Отправить первый комментарий"]');

        await pause(1000);

        const assessmentRule = new Autocomplete(this.browser, 'body', '[data-ow-test-rule-assessment-block="rule"]');

        const assessmentRuleIsDisplayed = await assessmentRule.isDisplayed();

        expect(assessmentRuleIsDisplayed).to.equal(true, 'Нет автокомплита для выбора правила');

        await assessmentRule.selectSingleItem('Тестовое правило для регресса (не трогать)');

        const addAutomationRule = new Button(this.browser, 'body', '[data-ow-test-save-button="automationRule$event"]');

        await addAutomationRule.clickButton();

        await checkSavedConfig(this.browser, EXPECTED_CONFIG);
        await archiveAutomationRule(this.browser);
    });
});

describe(`ocrm-1691: Правила автоматизации - вызов правил ассессмента недоступен, если метакласс не вложен в ticket или нет логики assessmentTicket
`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('работает корректно', async function() {
        await waitForReactRootLoaded(this.browser);

        await fillRuleData(this, 'Обращение первой линии (ticket$firstLine)');
        const addAssessmentBlockButton = this.browser.$('button=Добавить блок отправки в ассессмент');

        const addAssessmentBlockButtonIsDisplayed = await addAssessmentBlockButton.isDisplayed();

        expect(addAssessmentBlockButtonIsDisplayed).to.equal(
            false,
            'Есть кнопка добавления блока отправки в ассессмент'
        );
    });
});
