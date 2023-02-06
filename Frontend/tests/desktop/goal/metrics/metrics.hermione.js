const assert = require('chai').assert;

const PO = require('../../../../page-objects');
const START_URL = '/compilations/company?flags=goal_deps_tabs&goal=58526';

const assertViewOptions = {
    dontMoveCursor: true,
    ignoreElements: ['.GoalMetrics-Img', '.GoalMetrics-Embed'],
};

describe('Метрики', function() {
    beforeEach(async function() {
        const browser = this.browser;

        await browser.setViewportSize({ width: 2500, height: 2000 });

        await browser.loginToGoals();

        await browser.preparePage('goal-metrics', START_URL);
        await browser.waitForVisible(PO.goalTabControl());
        await browser.click(PO.goalTabControl.criteriasMetrics());
    });

    it('внешний вид разных типов метрик', async function() {
        const browser = this.browser;

        await browser.waitForVisible(PO.metrics.itemTypeText());

        await browser.yaScrollIntoView(PO.commentsHeader());

        await browser.assertView('itemTypeText', PO.metrics.itemTypeText(), assertViewOptions);
        await browser.moveToObject(PO.metrics.itemTypeText.text());
        await browser.assertView('itemTypeText-hovered', PO.metrics.itemTypeText(), assertViewOptions);
        await browser.click(PO.metrics.itemTypeText.editButton());
        await browser.waitForVisible(PO.metrics.itemTypeText.editForm());
        await browser.assertView('itemTypeText-editForm', PO.metrics.itemTypeText.editForm(), assertViewOptions);
        await browser.click(PO.metrics.itemTypeText.editForm.url());
        await browser.yaKeyPress('qwe');
        await browser.click(PO.metrics.itemTypeText.editForm.saveButton());
        await browser.assertView('editForm-invalid', PO.metrics.itemTypeText.editForm(), assertViewOptions);

        await browser.assertView('itemTypeHref', PO.metrics.itemTypeHref(), assertViewOptions);
        await browser.moveToObject(PO.metrics.itemTypeHref.text());
        await browser.assertView('itemTypeHref-hovered', PO.metrics.itemTypeHref(), assertViewOptions);
        await browser.click(PO.metrics.itemTypeHref.editButton());
        await browser.waitForVisible(PO.metrics.itemTypeHref.editForm());
        await browser.assertView('itemTypeHref-editForm', PO.metrics.itemTypeHref.editForm(), assertViewOptions);

        await browser.assertView('itemTypeEmbed', PO.metrics.itemTypeEmbed(), assertViewOptions);
        await browser.moveToObject(PO.metrics.itemTypeEmbed.text());
        await browser.assertView('itemTypeEmbed-hovered', PO.metrics.itemTypeEmbed(), assertViewOptions);
        await browser.click(PO.metrics.itemTypeEmbed.editButton());
        await browser.waitForVisible(PO.metrics.itemTypeEmbed.editForm());
        await browser.assertView('itemTypeEmbed-editForm', PO.metrics.itemTypeEmbed.editForm(), assertViewOptions);

        await browser.assertView('itemTypeImg', PO.metrics.itemTypeImg(), assertViewOptions);
        await browser.moveToObject(PO.metrics.itemTypeImg.text());
        await browser.assertView('itemTypeImg-hovered', PO.metrics.itemTypeImg(), assertViewOptions);
        await browser.click(PO.metrics.itemTypeImg.editButton());
        await browser.waitForVisible(PO.metrics.itemTypeImg.editForm());
        await browser.assertView('itemTypeImg-editForm', PO.metrics.itemTypeImg.editForm(), assertViewOptions);

        await browser.click(PO.metrics.addButton());
        await browser.assertView('addForm-disabled-submit', PO.metrics.addForm(), assertViewOptions);
    });

    it('редактирование метрики', async function() {
        const browser = this.browser;

        await browser.waitForVisible(PO.metrics.itemTypeHref());

        const oldText = await browser.$(PO.metrics.itemTypeHref.text()).getText();
        const oldUrl = await browser.$(PO.metrics.itemTypeHref.link()).getAttribute('href');
        const textToAdd = '123';
        const urlToAdd = 'qq';

        await browser.moveToObject(PO.metrics.itemTypeHref());
        await browser.waitForVisible(PO.metrics.itemTypeHref.editButton());

        await browser.click(PO.metrics.itemTypeHref.editButton());
        await browser.waitForVisible(PO.metrics.itemTypeHref.editForm());
        await browser.click(PO.metrics.itemTypeHref.editForm.title());
        await browser.yaKeyPress(textToAdd);
        await browser.click(PO.metrics.itemTypeHref.editForm.url());
        await browser.yaKeyPress(urlToAdd);
        await browser.click(PO.metrics.itemTypeHref.editForm.saveButton());

        await browser.waitForVisible(PO.metrics.itemTypeHref.text());
        const text = await browser.$(PO.metrics.itemTypeHref.text()).getText();
        const url = await browser.$(PO.metrics.itemTypeHref.link()).getAttribute('href');

        assert.equal(text, oldText + textToAdd, `К тексту должно добавиться ${textToAdd}`);
        assert.equal(url, oldUrl + urlToAdd, `К url должно добавиться ${urlToAdd}`);
    });

    it('создание и удаление метрики без url', async function() {
        const browser = this.browser;

        await browser.waitForVisible(PO.metrics.itemTypeText());

        const numberOfMetrics = (await browser.$$(PO.metrics.item())).length;
        const newMetricName = 'newMetricName';

        await browser.click(PO.metrics.addButton());
        await browser.waitForVisible(PO.metrics.addForm());
        await browser.click(PO.metrics.addForm.title());
        await browser.yaKeyPress(newMetricName);
        await browser.click(PO.metrics.addForm.saveButton());

        await browser.yaWaitUntilElCountChanged(PO.metrics.item(), numberOfMetrics + 1);

        const text = await browser.$(PO.metrics.lastItem.text()).getText();

        await browser.yaShouldExist(PO.metrics.lastItem.link(), 'Ссылки на элементе без url быть не должно', false);
        assert.equal(text, newMetricName, `Текст должен быть равен ${newMetricName}`);

        await browser.moveToObject(PO.metrics.lastItem());
        await browser.waitForVisible(PO.metrics.lastItem.deleteButton());
        await browser.click(PO.metrics.lastItem.deleteButton());
        await browser.waitForVisible(PO.metrics.lastItem.deleteConfirmationPopup());
        await browser.click(PO.metrics.lastItem.deleteConfirmationPopup.confirmButton());
        await browser.yaWaitForHidden(PO.metrics.lastItem.editForm());

        const numberOfMetricsAfterDeletion = await browser.yaCountElements(PO.metrics.item());

        assert.equal(numberOfMetricsAfterDeletion, numberOfMetrics, 'Метрика должна удалиться');
    });

    it('создание и удаление метрики со ссылкой', async function() {
        const browser = this.browser;

        await browser.waitForVisible(PO.metrics.itemTypeText());

        const numberOfMetrics = (await browser.$$(PO.metrics.item())).length;
        const metricName = 'newHrefMetricName';
        const metricUrl = 'https://ya.ru/';

        await browser.click(PO.metrics.addButton());
        await browser.waitForVisible(PO.metrics.addForm());
        await browser.click(PO.metrics.addForm.title());
        await browser.yaKeyPress(metricName);
        await browser.click(PO.metrics.addForm.url());
        await browser.yaKeyPress(metricUrl);
        await browser.click(PO.metrics.addForm.saveButton());

        await browser.yaWaitUntilElCountChanged(PO.metrics.item(), numberOfMetrics + 1);

        const text = await browser.$(PO.metrics.lastItem.text()).getText();
        const url = await browser.$(PO.metrics.lastItem.link()).getAttribute('href');

        assert.equal(text, metricName, `Текст должен быть равен ${metricName}`);
        assert.equal(url, metricUrl, `URL должен быть равен ${metricUrl}`);

        await browser.moveToObject(PO.metrics.lastItem());
        await browser.waitForVisible(PO.metrics.lastItem.deleteButton());
        await browser.click(PO.metrics.lastItem.deleteButton());
        await browser.waitForVisible(PO.metrics.lastItem.deleteConfirmationPopup());
        await browser.click(PO.metrics.lastItem.deleteConfirmationPopup.confirmButton());
        await browser.yaWaitForHidden(PO.metrics.lastItem.editForm());

        const numberOfMetricsAfterDeletion = await browser.yaCountElements(PO.metrics.item());

        assert.equal(numberOfMetricsAfterDeletion, numberOfMetrics, 'Метрика должна удалиться');
    });

    it('создание и удаление метрики с iframe', async function() {
        const browser = this.browser;

        await browser.waitForVisible(PO.metrics.itemTypeText());

        const numberOfMetrics = (await browser.$$(PO.metrics.item())).length;
        const metricName = 'newHrefMetricName';
        const metricUrl = 'https://charts.yandex-team.ru/preview/editor/80xuoxrh0emu3';

        await browser.click(PO.metrics.addButton());
        await browser.waitForVisible(PO.metrics.addForm());
        await browser.click(PO.metrics.addForm.title());
        await browser.yaKeyPress(metricName);
        await browser.click(PO.metrics.addForm.url());
        await browser.yaKeyPress(metricUrl);
        await browser.click(PO.metrics.addForm.saveButton());

        await browser.yaWaitUntilElCountChanged(PO.metrics.item(), numberOfMetrics + 1);

        const text = await browser.$(PO.metrics.lastItem.text()).getText();
        const url = await browser.$(PO.metrics.lastItem.link()).getAttribute('href');

        assert.equal(text, metricName, `Текст должен быть равен ${metricName}`);
        assert.equal(url, metricUrl, `URL должен быть равен ${metricUrl}`);
        await browser.yaShouldExist(PO.metrics.lastItem.iframe(), 'Должен быть iframe');

        await browser.moveToObject(PO.metrics.lastItem(), 10, 10);
        await browser.waitForVisible(PO.metrics.lastItem.deleteButton());
        await browser.click(PO.metrics.lastItem.deleteButton());
        await browser.waitForVisible(PO.metrics.lastItem.deleteConfirmationPopup());
        await browser.click(PO.metrics.lastItem.deleteConfirmationPopup.confirmButton());

        await browser.yaWaitUntilElCountChanged(PO.metrics.item(), numberOfMetrics, 'Метрика должна удалиться');
    });

    it('создание и удаление метрики с картинкой', async function() {
        const browser = this.browser;

        await browser.waitForVisible(PO.metrics.itemTypeText());

        const numberOfMetrics = (await browser.$$(PO.metrics.item())).length;
        const metricName = 'newHrefMetricName';
        const metricUrl = 'https://jing.yandex-team.ru/files/a-lexx/2021-03-11_03-10-09.png';

        await browser.click(PO.metrics.addButton());
        await browser.waitForVisible(PO.metrics.addForm());
        await browser.click(PO.metrics.addForm.title());
        await browser.yaKeyPress(metricName);
        await browser.click(PO.metrics.addForm.url());
        await browser.yaKeyPress(metricUrl);
        await browser.click(PO.metrics.addForm.saveButton());

        await browser.yaWaitUntilElCountChanged(PO.metrics.item(), numberOfMetrics + 1);

        const text = await browser.$(PO.metrics.lastItem.text()).getText();
        const url = await browser.$(PO.metrics.lastItem.link()).getAttribute('href');

        assert.equal(text, metricName, `Текст должен быть равен ${metricName}`);
        assert.equal(url, metricUrl, `URL должен быть равен ${metricUrl}`);
        await browser.yaShouldExist(PO.metrics.lastItem.img(), 'Должна быть картинка');

        await browser.moveToObject(PO.metrics.lastItem(), 10, 10);
        await browser.waitForVisible(PO.metrics.lastItem.deleteButton());
        await browser.click(PO.metrics.lastItem.deleteButton());
        await browser.waitForVisible(PO.metrics.lastItem.deleteConfirmationPopup());
        await browser.click(PO.metrics.lastItem.deleteConfirmationPopup.confirmButton());

        await browser.yaWaitUntilElCountChanged(PO.metrics.item(), numberOfMetrics, 'Метрика должна удалиться');
    });
});
