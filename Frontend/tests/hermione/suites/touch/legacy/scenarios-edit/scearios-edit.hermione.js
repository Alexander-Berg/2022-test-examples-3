const { Socket, SocketWithProperties, Light } = require('../../../../devices');
const { Api } = require('../../../../helpers/api');
const { TimetableTrigger, ActionsStep, Scenario } = require('../../../../scenarios');
const { YandexMini } = require('../../../../speakers');

describe('Страница редактирования сценария', () => {
    it('Пустая страница отображается корректно', async function() {
        // 29c578c
        const { browser, PO } = this;

        await browser.yaLoginReadonly();
        await browser.yaOpenPage('iot/scenario/add/summary', PO.ScenarioEditPage());
        await browser.yaAssertView('plain', 'body');
    });

    it('Открывается шторка с выбором иконки сценария', async function() {
        // 8a08539
        const { browser, PO } = this;

        await browser.yaLoginReadonly();
        await browser.yaOpenPage('iot/scenario/add/name', PO.ScenarioEditPage());
        await browser.click(PO.ScenarioIcon());
        await browser.pause(1_000);
        await browser.yaAssertViewBottomSheet('plain', 'body');
    });

    // Данный сценарий на yndx.quasar.test.1
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Открывается страница с триггерами и экшенами', async function() {
        const { browser, PO } = this;

        await browser.yaLogin();

        await browser.yaOpenPage(
            'iot/scenario/1eff3d10-1f2d-425e-b8f2-26e18f54277f/summary',
            PO.ScenarioEditPage()
        );

        await browser.yaPrepareQuasarAppWrapperForScreenshot();
        await browser.yaAssertView('plain', 'body');
    });

    it('Колонка, которая услышит выполнит команду: экшен отображается корректно', async function() {
        const { browser, PO } = this;

        await browser.yaLoginWritable();
        await browser.yaAddSpeakers([new YandexMini()]);

        await browser.yaOpenPage('iot/scenario/add', PO.ScenarioSummaryAddActionButton());
        await browser.click(PO.ScenarioSummaryAddActionButton());
        await browser.waitForVisible(PO.ScenarioActionSelectorCurrentSpeaker(), 10_000);
        await browser.click(PO.ScenarioActionSelectorCurrentSpeaker());
        await browser.waitForVisible(PO.ScenarioSpeakerActionListItemQuasarServerActionText(), 10_000);
        await browser.click(PO.ScenarioSpeakerActionListItemQuasarServerActionText());
        await browser.waitForVisible(PO.ScenarioSpeakerCapabilityEditTextActionInput(), 10_000);
        await browser.yaSetValue(PO.ScenarioSpeakerCapabilityEditTextActionInput(), 'какая-то команда');
        await browser.click(PO.ScenarioSpeakerCapabilityEditTextActionCompleteButton());
        await browser.waitForVisible(PO.ScenarioSummaryActionList(), 10_000);
        await browser.yaAssertView('plain', PO.ScenarioSummaryActionList());
    });

    it('Колонка, которая услышит произнесет фразу: экшен отображается корректно', async function() {
        const { browser, PO } = this;

        await browser.yaLoginWritable();
        await browser.yaAddSpeakers([new YandexMini()]);

        await browser.yaOpenPage('iot/scenario/add', PO.ScenarioSummaryAddActionButton());
        await browser.click(PO.ScenarioSummaryAddActionButton());
        await browser.waitForVisible(PO.ScenarioActionSelectorCurrentSpeaker(), 10_000);
        await browser.click(PO.ScenarioActionSelectorCurrentSpeaker());
        await browser.waitForVisible(PO.ScenarioSpeakerActionListItemQuasarActionTts(), 10_000);
        await browser.click(PO.ScenarioSpeakerActionListItemQuasarActionTts());
        await browser.waitForVisible(PO.ScenarioSpeakerCapabilityEditTtsInput(), 10_000);
        await browser.yaSetValue(PO.ScenarioSpeakerCapabilityEditTtsInput(), 'какая-то фраза');
        await browser.click(PO.ScenarioSpeakerCapabilityEditTtsCompleteButton());
        await browser.waitForVisible(PO.ScenarioSummaryActionList(), 10_000);
        await browser.yaAssertView('plain', PO.ScenarioSummaryActionList());
    });

    it('Выбранная колонка выполнит команду: экшен отображается корректно', async function() {
        //ebe7f84
        const { browser, PO } = this;

        await browser.yaLoginWritable();
        await browser.yaAddSpeakers([new YandexMini()]);

        await browser.yaOpenPage('iot/scenario/add', PO.ScenarioSummaryAddActionButton());
        await browser.click(PO.ScenarioSummaryAddActionButton());
        await browser.waitForVisible(PO.ScenarioActionSelectorDeviceItemSpeaker(), 10_000);
        await browser.click(PO.ScenarioActionSelectorDeviceItemSpeaker());
        await browser.waitForVisible(PO.ScenarioSpeakerActionListItemQuasarServerActionText(), 10_000);
        await browser.click(PO.ScenarioSpeakerActionListItemQuasarServerActionText());
        await browser.waitForVisible(PO.ScenarioSpeakerCapabilityEditTextActionInput(), 10_000);
        await browser.yaSetValue(PO.ScenarioSpeakerCapabilityEditTextActionInput(), 'какая-то команда');
        await browser.click(PO.ScenarioSpeakerCapabilityEditTextActionCompleteButton());
        await browser.waitForVisible(PO.ScenarioSummaryActionList(), 10_000);
        await browser.yaAssertView('plain', PO.ScenarioSummaryActionList());
    });

    it('Выбранная колонка произнесет фразу: экшен отображается корректно', async function() {
        // ebe7f84
        const { browser, PO } = this;

        await browser.yaLoginWritable();
        await browser.yaAddSpeakers([new YandexMini()]);

        await browser.yaOpenPage('iot/scenario/add', PO.ScenarioSummaryAddActionButton());
        await browser.click(PO.ScenarioSummaryAddActionButton());
        await browser.waitForVisible(PO.ScenarioActionSelectorDeviceItemSpeaker(), 10_000);
        await browser.click(PO.ScenarioActionSelectorDeviceItemSpeaker());
        await browser.waitForVisible(PO.ScenarioSpeakerActionListItemQuasarActionTts(), 10_000);
        await browser.click(PO.ScenarioSpeakerActionListItemQuasarActionTts());
        await browser.waitForVisible(PO.ScenarioSpeakerCapabilityEditTtsInput(), 10_000);
        await browser.yaSetValue(PO.ScenarioSpeakerCapabilityEditTtsInput(), 'какая-то фраза');
        await browser.click(PO.ScenarioSpeakerCapabilityEditTtsCompleteButton());
        await browser.waitForVisible(PO.ScenarioSummaryActionList(), 10_000);
        await browser.yaAssertView('plain', PO.ScenarioSummaryActionList());
    });

    it('Ограничение на количество действий в сценарии', async function() {
        // ebe7f84
        const { browser, PO } = this;
        const deviceName = 'Розетка Hermione';

        // Добавить триггер фразу
        async function addTriggerVoice(name) {
            await browser.yaWaitForVisibleAndClick(PO.ScenarioSummaryAddTriggerButton());
            await browser.yaClickToBottomSheetItem('Фраза');
            await browser.waitForVisible(PO.TextInputReal(), 10_000);
            await browser.yaSetValue(PO.TextInputReal(), name);
            await browser.click(PO.ScenarioEditPageSaveButton());
        }

        // Добавить триггер по времени
        async function addTriggerTime() {
            await browser.yaClickToBottomSheetItem('Время');
            await browser.yaClickToListItemByText('Точное время');
            await browser.yaWaitForVisibleAndClick(PO.DayOfWeekSelectDayButton());
            await browser.click(PO.ScenarioEditPageSaveButton());
        }

        async function addTriggerDevice() {
            await browser.yaWaitForVisibleAndClick(PO.ScenarioSummaryAddTriggerButton(), undefined, true);
            await browser.yaClickToListItemByText('Устройство');
            await browser.yaClickToListItemByText('текущее напряжение');
            await browser.yaSetValue(PO.TextInputReal(), '220');
            await browser.yaClickToSelectorByIndex('.button.button_theme_primary>.active-area', 0);
        }

        // Авторизуемся под пользователем из группы 'with-devices'
        await browser.yaLoginWritable();
        // Добавляем устройства пользователю
        await browser.yaAddDevices([new SocketWithProperties(deviceName)]);

        // 1
        // Открыть страницу Сценарии, начать создавать новый
        await browser.yaOpenPage('iot/scenario/add');

        // Тапнуть на "Добавить условие" -> Фраза -> добавить и сохранить любую фразу
        // Повторить добавление фразы ещё три раза (суммарно -- 4 фразы)
        await addTriggerVoice('какая-то команда');
        await addTriggerVoice('какая-то команда два');
        await addTriggerVoice('какая-то команда три');
        await addTriggerVoice('какая-то команда четыре');

        // Скриншот 4 фраз
        await browser.waitForVisible(PO.ScenarioSummaryTriggerList(), 10_000);

        await browser.yaAssertView('4-voice', PO.ScenarioSummaryTriggerList());

        // 2
        // Тапнуть на "Добавить условие"
        await browser.yaWaitForVisibleAndClick(PO.ScenarioSummaryAddTriggerButton(), undefined, true);
        // Добавление фразы отображается задизейбленным
        await browser.yaAssertViewBottomSheet('no-voice');

        // 3
        // Нажать на "Время срабатывания" и сохранить любой таймер
        await addTriggerTime();

        // 4
        // Добавить условия по датчику
        await browser.yaScrollToEndOnSelector('.app__wrapper');
        await addTriggerDevice();

        // Кнопка "Добавить условие" пропала
        await browser.waitForVisible(PO.ScenarioSummaryTriggerList(), 10_000);
        await browser.waitForExist(PO.ScenarioSummaryAddTriggerButton(), 1000, true);

        // 5
        // Нажать на любую фразу и удалить её
        await browser.yaScrollToEndOnSelector('.app__wrapper');
        await browser.yaClickToListItemByText('«какая-то команда четыре»');
        await browser.yaClickToBottomSheetItem('Удалить');
        // Кнопка "Добавить условие" появилась
        await browser.waitForExist(PO.ScenarioSummaryAddTriggerButton());

        // 6
        // Тапнуть на "Добавить условие"
        await browser.yaWaitForVisibleAndClick(PO.ScenarioSummaryAddTriggerButton(), undefined, true);
        // Добавление таймера отображается задизейбленным
        await browser.yaAssertViewBottomSheet('no-time');
    });

    it('Длинное название устройства', async function() { //4ce19f2
        const { browser, PO } = this;
        const api = Api(browser);

        // Авторизуемся под пользователем из группы 'with-devices'
        await browser.yaLoginWritable();
        // Добавляем устройства пользователю
        await browser.yaAddDevices([
            new Socket(Array(25).fill('Ж').join('')),
        ]);

        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const device = household.unconfigured_devices[0];

        // Создаем комнату и добавляем туда первое из устройств
        await browser.yaAddRoom('Комнатка', household.id, [device.id]);

        // Открываем создание сценария:
        // - Создание сценария (экран выбора устройства)
        await browser.yaOpenPage('iot/scenario/add', '.iot-scenario-edit-summary__section');
        await browser.click(PO.ScenarioSummaryAddActionButton());
        await browser.waitForVisible(PO.ListItem(), 10_000);
        await browser.yaAssertView('list', 'body');

        // - Создание сценария (экран подтверждения устройства)
        await browser.yaClickToListItemByText(device.name);
        await browser.yaAssertView('device', 'body');

        // - Создание сценария (главный экран с триггерами и экшенами)
        await browser.yaClickToSelectorByText(PO.ScenarioEditPage.primaryButton(), 'Далее');
        await browser.waitForVisible(PO.ScenarioSummaryActionList(), 10_000);
        await browser.yaAssertView('summary', 'body');
    });

    it('Тоггл "повторять ежедневно" верно аффектит дни недели', async function() { // a8ead1a
        const { browser, PO } = this;

        const censorSkillsOptions = { ignoreElements: ['.time-roll'] };

        const targetToggle = PO.ScenarioEditPage.content.listItem.toggle;
        const targetDay = PO.DayOfWeekSelectDayButton.nthChild(7);
        const targetView = PO.ScenarioEditPage.content();

        await browser.yaLoginReadonly();
        await browser.yaOpenPage('iot/scenario/add/trigger/add/scenario.trigger.timetable/specific_time', targetView);

        // Нажать на тоггл - включить все дни недели
        await browser.click(targetToggle());
        await browser.yaAssertView('toggle-all-on', targetView, censorSkillsOptions);

        // Нажать на день недели (воскресенье) - отключить один день
        await browser.click(targetDay());
        await browser.yaAssertView('click-on-a-day', targetView, censorSkillsOptions);

        // Нажать на тоггл - снова включить все дни недели
        await browser.click(targetToggle());
        await browser.yaAssertView('toggle-all-on-again', targetView, censorSkillsOptions);

        // Нажать на тоггл - выключить все дни недели
        await browser.click(targetToggle());
        await browser.yaAssertView('toggle-all-off', targetView, censorSkillsOptions);
    });

    describe('Отображение дней недели у триггера', () => {
        afterEach(async function cleaner() {
            const { browser } = this;

            // Очищаем сценарии
            await browser.yaRemoveScenarios();
        });

        it('Выбраны все дни кроме вторника - отображает эти дни двумя буквами', async function() { // 6db9ede
            const { browser, PO } = this;
            const api = Api(browser);

            await browser.yaLoginWritable();

            // Добавляем устройства пользователю
            await browser.yaAddDevices([
                new Light(),
            ]);

            // Получаем информацию о только что созданном девайсе
            const devices = await api.iot.devicesV2.getDevices();
            const household = devices.households[0];
            const unconfiguredDevices = household.unconfigured_devices;

            // Создаем триггер - в ПН, СР, ЧТ, ПТ, СБ, ВС в указанное время
            const somedays = [
                'monday',
                'wednesday',
                'thursday',
                'friday',
                'saturday',
                'sunday',
            ];

            const somedaysTrigger = new TimetableTrigger(somedays);

            // Создаем действие, сценарий
            const step = new ActionsStep(unconfiguredDevices);
            const scenario = new Scenario();

            scenario.addTrigger(somedaysTrigger);
            scenario.addStep(step);

            // Добавляем сценарий, получаем ответом id созданного сценария
            const { scenario_id } = await browser.yaAddScenario(scenario);

            // Открываем страницу сценария, делаем скрин
            const targetView = PO.ScenarioSummaryTriggerList();

            await browser.yaOpenPage(`iot/scenario/${scenario_id}`, targetView);
            await browser.yaAssertView('plain', targetView);
        });

        it('Выбраны все дни - отображает "ежедневно"', async function() { // 33f20a8
            const { browser, PO } = this;
            const api = Api(browser);

            await browser.yaLoginWritable();

            // Добавляем устройства пользователю
            await browser.yaAddDevices([
                new Light(),
            ]);

            // Получаем информацию о только что созданном девайсе
            const devices = await api.iot.devicesV2.getDevices();
            const household = devices.households[0];
            const unconfiguredDevices = household.unconfigured_devices;

            // Создаем триггер - ежедневно, в указанное время
            const everydayTrigger = new TimetableTrigger();

            // Создаем действие, сценарий
            const step = new ActionsStep(unconfiguredDevices);
            const scenario = new Scenario();

            scenario.addTrigger(everydayTrigger);
            scenario.addStep(step);

            // Добавляем сценарий, получаем ответом id созданного сценария
            const { scenario_id } = await browser.yaAddScenario(scenario);

            // Открываем страницу сценария, делаем скрин
            const targetView = PO.ScenarioSummaryTriggerList();

            await browser.yaOpenPage(`iot/scenario/${scenario_id}`, targetView);
            await browser.yaAssertView('plain', targetView);
        });
    });

    describe('После добавления действия в сценарий страница отскролливается вниз', () => {
        function hideFooter(browser) {
            return browser.execute(function() {
                const footer = document.querySelector('.page-layout__footer');
                footer.style.display = 'none';
            });
        }

        const deviceNamesArray = [
            'один',
            'два',
            'три',
            'четыре',
            'пять',
            'шесть',
            'семь ',
        ];

        describe('Во время редактирования сценария', () => {
            afterEach(async function cleaner() {
                const { browser } = this;

                // Очищаем сценарии
                await browser.yaRemoveScenarios();
            });

            it('Без редактирования девайса', async function() { // be73196
                const { browser, PO } = this;
                const api = Api(browser);

                // Вход, открытие главной страницы
                await browser.yaLoginWritable();

                // Добавляем устройства пользователю
                await browser.yaAddDevices(
                    deviceNamesArray.map(name => {
                        return new Light(name);
                    }),
                );

                // Получаем информацию о только что добавленных девайсах
                const devices = await api.iot.devicesV2.getDevices();
                const household = devices.households[0];
                const allDevices = household.unconfigured_devices;
                const allDevicesExceptLast = allDevices.slice(0, -1);
                const lastDevice = allDevices[allDevices.length - 1];

                // Создаем комнату и добавляем туда устройства
                await browser.yaAddRoom('Комнатка', household.id, allDevices.map(device => {
                    return device.id;
                }));

                // Создаем сценарий
                const trigger = new TimetableTrigger();
                const step = new ActionsStep(allDevicesExceptLast);
                const scenario = new Scenario();
                scenario.addStep(step);
                scenario.addTrigger(trigger);
                const { scenario_id } = await browser.yaAddScenario(scenario);

                // Переменные - элементы страницы
                const addActionButton = PO.ScenarioSummaryAddActionButton();
                const listItem = PO.ListItem();
                const saveButton = PO.ScenarioEditPageSaveButton();
                const scenarioEditPage = PO.ScenarioEditPage.content();

                // Переходим на страницу сценария и выполняем тесткейс
                await browser.yaOpenPage(`iot/scenario/${scenario_id}/summary`, scenarioEditPage);
                // Прячем футер
                await hideFooter(browser);
                // Кликаем на "добавить устройство"
                await browser.click(addActionButton);
                await browser.waitForVisible(listItem, 10_000);
                // Выбираем последнее из устройств
                await browser.yaClickToListItemByText(lastDevice.name);
                // Добавляем устройство в сценарий и делаем скрин
                await browser.yaWaitForVisibleAndClick(saveButton);
                await browser.waitForVisible(scenarioEditPage, 10_000);

                await browser.yaAssertView('plain', 'body');
            });

            it('С редактированием девайса в конце', async function() { // 8bd657e
                const { browser, PO } = this;
                const api = Api(browser);

                // Вход, открытие главной страницы
                await browser.yaLoginWritable();

                // Добавляем устройства пользователю
                await browser.yaAddDevices(
                    deviceNamesArray.map(name => {
                        return new Light(name);
                    }),
                );

                // Получаем информацию о только что добавленных девайсах
                const devices = await api.iot.devicesV2.getDevices();
                const household = devices.households[0];
                const allDevices = household.unconfigured_devices;
                const allDevicesExceptLast = allDevices.slice(0, -1);
                const lastDevice = allDevices[allDevices.length - 1];

                // Создаем комнату и добавляем туда устройства
                await browser.yaAddRoom('Комнатка', household.id, allDevices.map(device => {
                    return device.id;
                }));

                // Создаем сценарий
                const trigger = new TimetableTrigger();
                const step = new ActionsStep(allDevicesExceptLast);
                const scenario = new Scenario();
                scenario.addStep(step);
                scenario.addTrigger(trigger);
                const { scenario_id } = await browser.yaAddScenario(scenario);

                // Переменные - элементы страницы
                const addActionButton = PO.ScenarioSummaryAddActionButton();
                const listItem = PO.ListItem();
                const saveButton = PO.ScenarioEditPageSaveButton();
                const scenarioEditPage = PO.ScenarioEditPage.content();

                // Переходим на страницу сценария и выполняем тесткейс
                await browser.yaOpenPage(`iot/scenario/${scenario_id}/summary`, scenarioEditPage);
                // Прячем футер
                await hideFooter(browser);
                // Кликаем на "добавить устройство"
                await browser.click(addActionButton);
                await browser.waitForVisible(listItem, 10_000);
                // Выбираем последнее из устройств
                await browser.yaClickToListItemByText(lastDevice.name);
                // Добавляем устройство в сценарий
                await browser.yaWaitForVisibleAndClick(saveButton);
                await browser.waitForVisible(scenarioEditPage, 10_000);

                // Редактируем девайс и делаем скрин
                await browser.yaClickToListItemByText(lastDevice.name);
                await browser.yaWaitForVisibleAndClick(saveButton);
                await browser.waitForVisible(scenarioEditPage, 10_000);

                await browser.yaAssertView('plain', 'body');
            });
        });

        describe('Во время создания нового сценария', () => {
            afterEach(async function cleaner() {
                const { browser } = this;

                // Очищаем сценарии
                await browser.yaRemoveScenarios();
            });

            it('Без редактирования девайса', async function() { // 9f10010
                const { browser, PO } = this;
                const api = Api(browser);

                // Вход, открытие главной страницы
                await browser.yaLoginWritable();

                // Добавляем устройства пользователю
                await browser.yaAddDevices(
                    deviceNamesArray.map(name => {
                        return new Light(name);
                    }),
                );

                // Получаем информацию о только что добавленных девайсах
                const devices = await api.iot.devicesV2.getDevices();
                const household = devices.households[0];
                const allDevices = household.unconfigured_devices;
                const allDevicesExceptLast = allDevices.slice(0, -1);
                const lastDevice = allDevices[allDevices.length - 1];

                // Создаем комнату и добавляем туда устройства
                await browser.yaAddRoom('Комнатка', household.id, allDevices.map(device => {
                    return device.id;
                }));

                // Переменные - элементы страницы
                const addActionButton = PO.ScenarioSummaryAddActionButton();
                const listItem = PO.ListItem();
                const saveButton = PO.ScenarioEditPageSaveButton();
                const scenarioEditPage = PO.ScenarioEditPage.content();

                // Переходим на страницу создания сценария
                await browser.yaOpenPage('iot/scenario/add/summary', scenarioEditPage);

                // Добавляем устройство через listItem
                await browser.click(addActionButton);
                await browser.waitForVisible(listItem, 10_000);
                // Выбираем последнее из устройств
                await browser.yaClickToListItemByText(lastDevice.name);
                await browser.yaWaitForVisibleAndClick(saveButton);
                await browser.waitForVisible(scenarioEditPage, 10_000);

                // Добавляем устройство через addActionButton для всех оставшихся девайсов
                for (const { name } of allDevicesExceptLast) {
                    // Прячем футер
                    await hideFooter(browser);
                    // Кликаем на "добавить устройство"
                    await browser.click(addActionButton);
                    await browser.waitForVisible(listItem, 10_000);
                    // Кликаем на устройство с нужным именем
                    await browser.yaClickToListItemByText(name);
                    // Сохраняем
                    await browser.yaWaitForVisibleAndClick(saveButton);
                    await browser.waitForVisible(scenarioEditPage, 10_000);
                }

                await browser.yaAssertView('plain', 'body');
            });

            it('С редактированием девайса в конце', async function() { // 8ee8337
                const { browser, PO } = this;
                const api = Api(browser);

                // Вход, открытие главной страницы
                await browser.yaLoginWritable();

                // Добавляем устройства пользователю
                await browser.yaAddDevices(
                    deviceNamesArray.map(name => {
                        return new Light(name);
                    }),
                );

                // Получаем информацию о только что добавленных девайсах
                const devices = await api.iot.devicesV2.getDevices();
                const household = devices.households[0];
                const allDevices = household.unconfigured_devices;
                const allDevicesExceptLast = allDevices.slice(0, -1);
                const lastDevice = allDevices[allDevices.length - 1];

                // Создаем комнату и добавляем туда устройства
                await browser.yaAddRoom('Комнатка', household.id, allDevices.map(device => {
                    return device.id;
                }));

                // Переменные - элементы страницы
                const addActionButton = PO.ScenarioSummaryAddActionButton();
                const listItem = PO.ListItem();
                const saveButton = PO.ScenarioEditPageSaveButton();
                const scenarioEditPage = PO.ScenarioEditPage.content();

                // Переходим на страницу создания сценария
                await browser.yaOpenPage('iot/scenario/add/summary', scenarioEditPage);

                // Добавляем устройство через listItem
                await browser.click(addActionButton);
                await browser.waitForVisible(listItem, 10_000);
                // Выбираем последнее из устройств
                await browser.yaClickToListItemByText(lastDevice.name);
                await browser.yaWaitForVisibleAndClick(saveButton);
                await browser.waitForVisible(scenarioEditPage, 10_000);

                // Добавляем устройство через addActionButton для всех оставшихся девайсов
                for (const { name } of allDevicesExceptLast) {
                    // Прячем футер
                    await hideFooter(browser);
                    // Кликаем на "добавить устройство"
                    await browser.click(addActionButton);
                    await browser.waitForVisible(listItem, 10_000);
                    // Кликаем на устройство с нужным именем
                    await browser.yaClickToListItemByText(name);
                    // Сохраняем
                    await browser.yaWaitForVisibleAndClick(saveButton);
                    await browser.waitForVisible(scenarioEditPage, 10_000);
                }

                // Редактируем девайс и делаем скрин
                await browser.yaClickToListItemByText(lastDevice.name);
                await browser.yaWaitForVisibleAndClick(saveButton);
                await browser.waitForVisible(scenarioEditPage, 10_000);

                await browser.yaAssertView('plain', 'body');
            });
        });
    });

    describe('Редактирование триггера сценария', () => {
        afterEach(async function cleaner() {
            const { browser } = this;

            // Очищаем сценарии
            await browser.yaRemoveScenarios();
        });

        it('При изменении времени срабатывания - отображаемое время срабатывания меняется', async function() {
            const { browser, PO } = this;
            const api = Api(browser);

            // Открываем главную страницу
            await browser.yaLoginWritable();

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

            // Открываем страницу со сценарием
            const targetPage = PO.ScenarioEditPage.content();
            const targetView = PO.ScenarioSummaryTriggerList();

            await browser.yaOpenPage(`iot/scenario/${scenario_id}`, targetPage);
            await browser.yaAssertView('initialTrigger', targetView);

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

            // Повторно делаем скрин
            await browser.waitForVisible(targetView, 10_000);
            await browser.yaAssertView('updatedTrigger', targetView);
        });

        it('При нажатии на триггер выезжает модалка редактировать / удалить', async function() {
            const { browser, PO } = this;
            const api = Api(browser);

            // Открываем главную страницу
            await browser.yaLoginWritable();

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

            // Открываем страницу со сценарием
            const targetPage = PO.ScenarioEditPage.content();

            await browser.yaOpenPage(`iot/scenario/${scenario_id}`, targetPage);

            // Нажимаем на триггер
            await browser.yaClickToListItemByText('Время');

            // Делаем скрин
            await browser.yaAssertViewBottomSheet('modal');
        });
    });

    describe('Кнопка сохранения сценария', () => {
        afterEach(async function cleaner() {
            const { browser } = this;

            // Очищаем сценарии
            await browser.yaRemoveScenarios();
        });

        it('Кнопка задизейблина при отсутсвии триггеров', async function() {
            const { browser, PO } = this;
            const api = Api(browser);

            // Открываем главную страницу
            await browser.yaLoginWritable();
            await browser.yaOpenPage('', PO.IotScenarios.ScenariosList(), { tab: 'scenarios' });

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

            // Открываем страницу со сценарием
            const targetPage = PO.ScenarioEditPage.content();
            const targetButton = PO.ScenarioEditPageSaveButton();

            await browser.yaOpenPage(`iot/scenario/${scenario_id}`, targetPage);

            // Удаляем триггер
            await browser.yaClickToListItemByText('Время');
            await browser.yaClickToBottomSheetItem('Удалить');

            // Кликаем на кнопук сохранить
            await browser.click(targetButton);

            // Проверяем не вернулись ли мы на страницу списка сценариев
            await browser.pause(1_000);
            await browser.yaWaitForVisibleAndAssertErrorMessage(
                PO.IotScenarios.ScenariosList(),
                10_000,
                'Кнопка сохранения не задизейблилась и вернула на страницу списка сценариев',
                true
            );
            await browser.yaAssertView('inactiveButton', targetButton);

            // Кликаем на триггер по времени, затем на редактировать в модалке
            await browser.yaClickToListItemByText('Добавить условие');
            await browser.yaClickToBottomSheetItem('Время');
            await browser.yaClickToListItemByText('Точное время');

            // Выставляем дату
            await browser.yaWaitForVisibleAndClick(PO.DayOfWeekSelectDayButton());
            await browser.click(targetButton);

            // Делаем скрин активной кнопки
            await browser.waitForVisible(targetPage, 10_000);
            await browser.yaAssertView('activeButton', targetButton);

            // Кликаем сохранить сценарий
            await browser.click(targetButton);

            // Отслеживаем, что вернулись к странице всех сценариев
            await browser.yaWaitForVisibleAndAssertErrorMessage(
                PO.IotScenarios.ScenariosList(),
                10_000,
                'Кнопка сохранения не переадресовала на страницу списка сценариев',
            );
        });
    });
});
