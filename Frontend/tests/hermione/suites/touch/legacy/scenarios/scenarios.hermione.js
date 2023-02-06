const { Light, Socket } = require('../../../../devices');
const { Api } = require('../../../../helpers/api');
const { VoiceTrigger, TimetableTrigger, Scenario, ActionsStep } = require('../../../../scenarios');
const { UserStorageKey } = require('../../../../helpers/user-storage/keys');
const { UserStorageType } = require('../../../../helpers/user-storage/types');
const { YandexMini, YandexMini2 } = require('../../../../speakers');

describe('Сценарии', () => {
    afterEach(async function cleaner() {
        const { browser } = this;

        // Очищаем сценарии
        await browser.yaRemoveScenarios();
    });

    it('Нет запусков в сценариях', async function() {
        const { browser } = this;

        // Открываем главную страницу
        await browser.yaLoginWritable();
        await browser.yaOpenPage('iot');

        // Добавляем устройства пользователю
        await browser.yaAddDevices([
            new Light(),
        ]);

        // Открываем страницу историю запусков сценариев и делаем скрин
        await browser.yaOpenPage('iot/scenarios/history');
        await browser.yaAssertView('plain', 'html');
    });

    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Длинное название сценария', async function() { // f5504e0
        const { browser, PO } = this;
        const api = Api(browser);

        const targetView = PO.IotScenarios.ScenariosList();

        // Открываем главную страницу
        await browser.yaLoginWritable();
        await browser.yaUpdateUserStorage({
            [UserStorageKey.IOT_HOUSEHOLDS_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        // Добавляем устройства пользователю
        await browser.yaAddDevices([
            new Light(),
        ]);

        // Получаем информацию о только что созданном девайсе
        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const unconfiguredDevices = household.unconfigured_devices;

        // Создаем сценарий и добавляем его
        const trigger = new VoiceTrigger();
        const step = new ActionsStep(unconfiguredDevices);
        const scenario = new Scenario(Array(100).fill('Щ').join(''));

        scenario.addTrigger(trigger);
        scenario.addStep(step);

        await browser.yaAddScenario(scenario);

        /// Открываем страницу сценариев и делаем скрин
        await browser.yaOpenPage('', targetView, { tab: 'scenarios' });
        await browser.yaAssertView('plain', targetView);
    });

    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Сценарий "ежедневно" отображает текст "ежедневно"', async function() { // 58633f5
        const { browser, PO } = this;
        const api = Api(browser);

        const targetView = PO.IotScenarios.ScenariosList();

        // Открываем главную страницу
        await browser.yaLoginWritable();
        await browser.yaUpdateUserStorage({
            [UserStorageKey.IOT_HOUSEHOLDS_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        // Добавляем устройства пользователю
        await browser.yaAddDevices([
            new Light(),
        ]);

        // Получаем информацию о только что созданном девайсе
        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const unconfiguredDevices = household.unconfigured_devices;

        // Создаем триггер "ежедневно"
        const trigger = new TimetableTrigger();

        // Создаем действия, прикрепляем триггер
        const step = new ActionsStep(unconfiguredDevices);
        const scenario = new Scenario();

        scenario.addStep(step);
        scenario.addTrigger(trigger);

        await browser.yaAddScenario(scenario);

        /// Открываем страницу сценариев и делаем скрин
        await browser.yaOpenPage('', targetView, { tab: 'scenarios' });
        await browser.yaAssertView('plain', targetView);
    });

    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Сценарий "определенные дни" отображает эти дни двумя буквами', async function() { // f1899e0
        const { browser, PO } = this;
        const api = Api(browser);

        const targetView = PO.IotScenarios.ScenariosList();

        // Открываем главную страницу
        await browser.yaLoginWritable();
        await browser.yaUpdateUserStorage({
            [UserStorageKey.IOT_HOUSEHOLDS_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        // Добавляем устройства пользователю
        await browser.yaAddDevices([
            new Light(),
        ]);

        // Получаем информацию о только что созданном девайсе
        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const unconfiguredDevices = household.unconfigured_devices;

        // Создаем триггер "в Пн, Ср, Чт, Пт, Сб, Вс"
        const somedays = [
            'monday',
            'wednesday',
            'thursday',
            'friday',
            'saturday',
            'sunday',
        ];
        const trigger = new TimetableTrigger(somedays);

        // Создаем действия, прикрепляем триггер
        const step = new ActionsStep(unconfiguredDevices);
        const scenario = new Scenario();

        scenario.addStep(step);
        scenario.addTrigger(trigger);

        await browser.yaAddScenario(scenario);

        /// Открываем страницу сценариев и делаем скрин
        await browser.yaOpenPage('', targetView, { tab: 'scenarios' });
        await browser.yaAssertView('plain', targetView);
    });

    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Сценарий "в среду" отображает день перед временем', async function() { // d4484ce
        const { browser, PO } = this;
        const api = Api(browser);

        const targetView = PO.IotScenarios.ScenariosList();

        // Открываем главную страницу
        await browser.yaLoginWritable();
        await browser.yaUpdateUserStorage({
            [UserStorageKey.IOT_HOUSEHOLDS_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        // Добавляем устройства пользователю
        await browser.yaAddDevices([
            new Light(),
        ]);

        // Получаем информацию о только что созданном девайсе
        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const unconfiguredDevices = household.unconfigured_devices;

        // Создаем триггер "только в среду"
        const onlyWednesday = [
            'wednesday',
        ];
        const trigger = new TimetableTrigger(onlyWednesday);

        // Создаем действия, прикрепляем триггер
        const step = new ActionsStep(unconfiguredDevices);
        const scenario = new Scenario();

        scenario.addStep(step);
        scenario.addTrigger(trigger);

        await browser.yaAddScenario(scenario);

        /// Открываем страницу сценариев и делаем скрин
        await browser.yaOpenPage('', targetView, { tab: 'scenarios' });
        await browser.yaAssertView('plain', targetView);
    });

    describe('Верхняя карусель "скоро сработают"', () => {
        let scenarioId;

        // Логин, добавление девайса, сценария
        beforeEach(async function prepare() {
            const { browser, PO } = this;
            const api = Api(browser);

            // Открываем главную страницу
            await browser.yaLoginWritable();
            await browser.yaUpdateUserStorage({
                [UserStorageKey.IOT_HOUSEHOLDS_TOOLTIP_CLOSED]: {
                    type: UserStorageType.BOOL,
                    value: true,
                },
            });

            // Добавляем устройство пользователю
            await browser.yaAddDevices([
                new Light(),
            ]);

            // Получаем информацию о только что созданном девайсе
            const devices = await api.iot.devicesV2.getDevices();
            const household = devices.households[0];
            const device = household.unconfigured_devices[0];

            // Создаем действие, триггер, сценарий
            const step = new ActionsStep([device]);
            const trigger = new TimetableTrigger();
            const scenario = new Scenario();
            scenario.addStep(step);
            scenario.addTrigger(trigger);

            // Добавляем сценарий пользователю
            const { scenario_id } = await browser.yaAddScenario(scenario);
            scenarioId = scenario_id;
        });

        // Очистка аккаунта от девайсов и сценариев
        afterEach(async function cleaner() {
            const { browser } = this;

            await browser.yaRemoveScenarios();
        });

        // todo: расскипать в https://st.yandex-team.ru/QUASARUI-2396
        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('При изменении времени срабатывания - время меняется в карусели, в списке всех сценариев', async function() {
            const { browser, PO } = this;

            // Делаем изначальный скрин
            const scenarioEditPage = PO.ScenarioEditPage.content();
            const targetView = PO.IotScenarios();

            await browser.yaOpenPage('', targetView, { tab: 'scenarios' });
            await browser.yaAssertView('initialScenario', targetView);

            // Открываем страницу со сценарием
            await browser.yaOpenPage(`iot/scenario/${scenarioId}`, scenarioEditPage);

            // Кликаем на триггер по времени, затем на редактировать в модалке
            await browser.yaClickToListItemByText('Время');
            await browser.yaClickToBottomSheetItem('Редактировать');

            // Меняем дату
            await browser.yaWaitForVisibleAndClick(PO.DayOfWeekSelectDayButton());

            // Скролим время
            const timeRollHour = PO.TimeRoll.hour();
            const scenarioSaveButton = PO.ScenarioEditPageSaveButton();

            await browser.dragAndDrop(timeRollHour, scenarioSaveButton);
            await browser.click(scenarioSaveButton);

            // Сохраняем сценарий
            await browser.yaWaitForVisibleAndClick(scenarioSaveButton);

            // Повторно делаем скрин;
            await browser.waitForVisible(targetView, 10_000);
            await browser.yaAssertView('updatedScenario', targetView);
        });

        // todo: расскипать в https://st.yandex-team.ru/QUASARUI-2396
        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('При изменении триггера "по времени" на "фраза" сценарий пропадает из карусели', async function() {
            const { browser, PO } = this;

            // Делаем изначальный скрин
            const scenarioEditPage = PO.ScenarioEditPage.content();
            const targetView = PO.IotScenarios();

            await browser.yaOpenPage('', targetView, { tab: 'scenarios' });
            await browser.yaAssertView('initialScenario', targetView);

            // Открываем страницу со сценарием
            await browser.yaOpenPage(`iot/scenario/${scenarioId}`, scenarioEditPage);

            // Кликаем на триггер по времени и удаляем
            await browser.yaClickToListItemByText('Время');
            await browser.yaClickToBottomSheetItem('Удалить');

            // Дабавляем триггер по фразе
            const scenarioSaveButton = PO.ScenarioEditPageSaveButton();
            const textInput = PO.TextInputReal();

            await browser.yaClickToListItemByText('Добавить условие');
            await browser.yaClickToBottomSheetItem('Фраза');
            await browser.waitForVisible(textInput);
            await browser.yaSetValue(PO.TextInputReal(), 'Теперь это триггер по фразе');
            await browser.click(scenarioSaveButton);

            // Сохраняем сценарий
            await browser.yaWaitForVisibleAndClick(scenarioSaveButton);

            // Повторно делаем скрин
            await browser.waitForVisible(targetView, 10_000);
            await browser.yaAssertView('updatedScenario', targetView);
        });
    });

    describe('Список колонок', async function() {
        it('Без колонок', async function() {
            const { browser } = this;
            const api = Api(browser);

            await browser.yaLoginWritable();
            await browser.yaAddDevices([
                new Socket('Розетка'),
            ]);

            const devices = await api.iot.devicesV2.getDevices();
            const household = devices.households[0];
            const device = household.unconfigured_devices[0];

            await browser.yaAddRoom('Комнатка', household.id, [device.id]);

            await browser.yaOpenPage('iot/scenario/add/action');
            await browser.yaWaitForVisibleAndAssertErrorMessage(
                '.list-item',
                10_000,
                'Ни один вариант выбора не прогрузился за 10 секунд'
            );
            await browser.yaWaitForVisibleAndAssertErrorMessage(
                '.iot-device-icon_type_smart-speaker',
                2_000,
                'Вариант "Любая умная колонка" не должен был отобразиться',
                true
            );
            await browser.yaAssertView('plain', 'body');
        });

        it('Без комнат', async function() {
            const { browser } = this;

            await browser.yaLoginWritable();
            await browser.yaAddSpeakers([new YandexMini(), new YandexMini2()]);

            await browser.yaOpenPage('iot/scenario/add/action');
            await browser.yaWaitForVisibleAndAssertErrorMessage(
                '.iot-device-icon_type_smart-speaker',
                10_000,
                'Вариант "Любая умная колонка" не отобразился за 10 секунд'
            );
            await browser.yaAssertView('plain', 'body');
        });

        it('Один в комнате', async function() {
            const { browser } = this;
            const api = Api(browser);

            await browser.yaLoginWritable();
            await browser.yaAddSpeakers([new YandexMini(), new YandexMini2()]);

            const devices = await api.iot.devicesV3.getDevices();
            const household = devices.households[0];
            const mini = household.all[0];

            await browser.yaAddRoom('Комнатка', household.id, [mini.id]);

            await browser.yaOpenPage('iot/scenario/add/action');
            await browser.yaWaitForVisibleAndAssertErrorMessage(
                '.iot-device-icon_type_smart-speaker',
                10_000,
                'Вариант "Любая умная колонка" не отобразился за 10 секунд'
            );
            await browser.yaAssertView('plain', 'body');
        });

        it('Все по комнатам', async function() {
            const { browser } = this;
            const api = Api(browser);

            await browser.yaLoginWritable();
            await browser.yaAddSpeakers([new YandexMini(), new YandexMini2()]);

            const devices = await api.iot.devicesV3.getDevices();
            const household = devices.households[0];
            const mini = household.all[0];
            const mini2 = household.all[1];

            await browser.yaAddRoom('Комнатка', household.id, [mini.id]);
            await browser.yaAddRoom('Дворик', household.id, [mini2.id]);

            await browser.yaOpenPage('iot/scenario/add/action');
            await browser.yaWaitForVisibleAndAssertErrorMessage(
                '.iot-device-icon_type_smart-speaker',
                10_000,
                'Вариант "Любая умная колонка" не отобразился за 10 секунд'
            );
            await browser.yaAssertView('plain', 'body');
        });
    });
});
