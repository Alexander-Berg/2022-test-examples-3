const { TimetableTrigger, ActionsStep, Scenario } = require('../../../scenarios');
const { Api } = require('../../../helpers/api');
const { UserStorageKey } = require('../../../helpers/user-storage/keys');
const { UserStorageType } = require('../../../helpers/user-storage/types');
const { LightWithScenes } = require('../../../devices');

const FEW_SECONDS_AFTER_MIDNIGHT = 30;

async function makeScenarioWithTrigger(browser, PO, timetableTrigger, time) {
    await browser.yaLoginWritable();
    const api = Api(browser);

    const targetView = PO.IotScenarios.ScenariosList();

    await browser.yaUpdateUserStorage({
        [UserStorageKey.IOT_HOUSEHOLDS_TOOLTIP_CLOSED]: {
            type: UserStorageType.BOOL,
            value: true,
        },
    });


    await browser.yaAddDevices([new LightWithScenes()]);

    const devicesResponse = await api.iot.devicesV3.getDevices();
    const householdDevices = devicesResponse.households[0].all;

    // Создаем сценарий и добавляем его
    const trigger = new TimetableTrigger(timetableTrigger, time);
    const step = new ActionsStep(householdDevices);
    const scenario = new Scenario();

    scenario.addTrigger(trigger);
    scenario.addStep(step);

    await browser.yaAddScenario(scenario);
    await browser.yaOpenPage('', targetView, { tab: 'scenarios' });
}

describe('Расписания', () => {
    afterEach(async function cleaner() {
        const { browser } = this;

        // Очищаем сценарии
        await browser.yaRemoveScenarios();
    });

    it('Отображение в списке сценариев - ежедневно', async function() {
        const { browser, PO } = this;
        const targetView = PO.IotScenarios.ScenariosList();

        await makeScenarioWithTrigger(browser, PO, undefined, FEW_SECONDS_AFTER_MIDNIGHT);
        await new Promise(r => setTimeout(r, 5000));
        await browser.yaAssertView('scenario-list', targetView, { selectorToScroll: PO.App() });
    });

    it('Отображение в списке сценариев - не все дни', async function() {
        const { browser, PO } = this;
        const targetView = PO.IotScenarios.ScenariosList();

        await makeScenarioWithTrigger(browser, PO, [
            'monday',
            'wednesday',
            'thursday',
            'friday',
            'saturday',
            'sunday',
        ], FEW_SECONDS_AFTER_MIDNIGHT);
        await browser.yaAssertView('scenario-list', targetView, { selectorToScroll: PO.App() });
    });

    it('Отображение в списке сценариев - только 1 день', async function() {
        const { browser, PO } = this;
        const targetView = PO.IotScenarios.ScenariosList();

        await makeScenarioWithTrigger(browser, PO, ['friday'], FEW_SECONDS_AFTER_MIDNIGHT);
        await browser.yaAssertView('scenario-list', targetView, { selectorToScroll: PO.App() });
    });

    it('Возможно выключить сценарий', async function() {
        const { browser, PO } = this;
        const targetView = PO.IotScenarios.ScenariosList();

        await makeScenarioWithTrigger(browser, PO, undefined, FEW_SECONDS_AFTER_MIDNIGHT);

        await browser.click(PO.IotScenarios.ScenariosListItem());
        await browser.waitForVisible(PO.ToggleSwitch(), 10_000);
        await browser.click(PO.ToggleSwitch());

        await browser.yaOpenPage('', targetView, { tab: 'scenarios' });

        // Ждем поллинг
        await browser.pause(2000);

        await browser.yaAssertView('scenario-list', targetView, { selectorToScroll: PO.App() });
    });

    it('Отображение внутри сценария - ежедневно', async function() {
        const { browser, PO } = this;

        await makeScenarioWithTrigger(browser, PO, undefined, FEW_SECONDS_AFTER_MIDNIGHT);

        await browser.click(PO.IotScenarios.ScenariosListItem());
        await browser.waitForVisible(PO.ToggleSwitch(), 10_000);

        await browser.yaAssertView('scenario', 'body', { selectorToScroll: PO.App() });
    });

    it('Отображение внутри сценария - несколько дней', async function() {
        const { browser, PO } = this;

        await makeScenarioWithTrigger(browser, PO, ['monday', 'friday'], FEW_SECONDS_AFTER_MIDNIGHT);

        await browser.click(PO.IotScenarios.ScenariosListItem());
        await browser.waitForVisible(PO.ToggleSwitch(), 10_000);

        await browser.yaAssertView('scenario', 'body', { selectorToScroll: PO.App() });
    });

    it('Редактирование расписания', async function() {
        const { browser, PO } = this;

        await makeScenarioWithTrigger(browser, PO, undefined, FEW_SECONDS_AFTER_MIDNIGHT);

        await browser.click(PO.IotScenarios.ScenariosListItem());
        await browser.waitForVisible(PO.ToggleSwitch(), 10_000);
        await browser.yaClickToListItemByText('Время');
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaAssertView('scenario-trigger-edit', 'body', { selectorToScroll: PO.App() });

        await browser.yaClickToBottomSheetItem('Редактировать');
        await browser.yaWaitForVisibleAndClick(PO.DayOfWeekSelectDayButton());

        // Скролим время
        const timeRollHour = PO.TimeRoll.hour();
        const scenarioSaveButton = PO.ScenarioEditPageSaveButton();

        await browser.dragAndDrop(timeRollHour, scenarioSaveButton);
        await browser.click(scenarioSaveButton);

        await browser.waitForVisible(scenarioSaveButton, 10_000);
        await browser.yaAssertView('scenario-trigger-edited', 'body', { selectorToScroll: PO.App() });
    });
});
