const { YandexStation, YandexMini, YandexModule2 } = require('../../../speakers');
const { LightWithScenes } = require('../../../devices');
const { UserStorageKey, UserStorageDynamicKeys } = require('../../../helpers/user-storage/keys');
const { UserStorageType } = require('../../../helpers/user-storage/types');
const { StoriesViewsKey } = require('../../../helpers/stories');

describe('Сторисы / с колонкой и УД', () => {
    beforeEach(async function() {
        const { browser, PO } = this;

        await browser.yaLoginWritable();
        await browser.yaAddSpeakers([new YandexMini(), new YandexModule2()]);
        await browser.yaAddDevices([new LightWithScenes()]);

        await browser.yaUpdateUserStorage({
            // Сбросить просмотренность сторей
            [UserStorageDynamicKeys.STORY_VIEW(StoriesViewsKey.IOT_NEWS_FEED, 'app')]: {
                type: UserStorageType.BOOL,
                value: false,
            },
            [UserStorageDynamicKeys.STORY_VIEW(StoriesViewsKey.IOT_NEWS_FEED, 'scenarios-steps')]: {
                type: UserStorageType.BOOL,
                value: false,
            },
            [UserStorageDynamicKeys.STORY_VIEW(StoriesViewsKey.IOT_NEWS_FEED, 'whisper')]: {
                type: UserStorageType.BOOL,
                value: false,
            },
            [UserStorageDynamicKeys.STORY_VIEW(StoriesViewsKey.IOT_NEWS_FEED, 'tandem')]: {
                type: UserStorageType.BOOL,
                value: false,
            },

            // Скрыть тултипы
            [UserStorageKey.IOT_HOUSEHOLDS_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        await browser.yaOpenPage('', PO.IotHome());
    });

    it('Лента сторей', async function() {
        const { browser, PO } = this;

        // do: Открыть список устройств
        // screenshot: Отображаются сторисы в правильном порядке [newsfeed]
        await browser.yaAssertView('newsfeed', PO.IotHome.News());
    });

    it('Кнопка Все новости', async function() {
        const { browser, PO } = this;

        // do: Проскроллить ленту сторей
        // assert: В конце ленты кнопка Все новости
        const allNewsButtonText = await browser.getText(PO.IotNewsFeed.emptyItem());
        assert(allNewsButtonText === 'Все новости', 'Ожидалась кнопка «Все новости»');

        // do: Тапнуть на кнопку
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();

        // screenshot: Отображаются все сторисы в правильном порядке [all-news]
        // скриншот немного косячный, но порядок сторей виден хорошо
        await browser.yaAssertView(
            'all-news',
            PO.BottomSheetContentLayout(),
            {
                selectorToScroll: PO.BottomSheetContent(),
            }
        );
    });

    it('Сторя отображается просмотренной', async function() {
        const { browser, PO } = this;

        // Искусственная активность пользователя, чтобы работал history.replace() при открытии сторей
        await browser.click('.home__tab');

        // do: Открыть любую сторю из ленты
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.tile(), 'Что нового в сценариях');
        await browser.yaWaitForStoryOpenAnimation();

        // do: Тапнуть на крестик в правом углу
        await browser.click(PO.StoryModal.story.storyItem.closeButton());

        await browser.yaWaitForStoryCloseAnimation();

        // assert: Страница закрылась, просмотренные сторисы отображаются без фиолетовой обводки
        await browser.yaAssertView('newsfeed-viewed', PO.IotHome.News());
    });

    it('Что нового', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на Что нового
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.tile(), 'Что нового', true);
        await browser.yaWaitForStoryOpenAnimation();

        // screenshot: Отображается сторя Добро пожаловать [whatsnew-1]
        await browser.yaAssertView('whatsnew-1', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя До сценария один клик [whatsnew-2]
        await browser.yaAssertView('whatsnew-2', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Любимые устройства [whatsnew-3]
        await browser.yaAssertView('whatsnew-3', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Разные фоны [whatsnew-4]
        await browser.yaAssertView('whatsnew-4', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя А ещё темная тема с кнопкой [whatsnew-5]
        await browser.yaAssertView('whatsnew-5', 'body');

        // assert: Тап на `Выбрать тему и фон` ведет в настройки оформления
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.includes('/iot/settings/appearance?'), 'Ожидалась ссылка в Настройки');

        await browser.click(PO.StoryModal.story.storyItem.content.button());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaAssertView('appearance-settings', PO.BottomSheetContent());
    });

    it('Что нового в сценариях', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на Что нового в сценариях и пролистать до последней стори
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.tile(), 'Что нового в сценариях');
        await browser.yaWaitForStoryOpenAnimation();

        // screenshot: Отображается сторя Одно устройство // нексколько команд [newscenario-1]
        await browser.yaAssertView('newscenario-1', 'body');

        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Пауза между действиями [newscenario-2]
        await browser.yaAssertView('newscenario-2', 'body');

        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Больше навыков у колонок с кнопкой [newscenario-3]
        await browser.yaAssertView('newscenario-3', 'body');

        // assert: Тап на `Создать сценарий` ведет на экран создания нового сценария
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.includes('/iot/scenario/add'), 'Ожидалась ссылка на создание сценария');

        await browser.click(PO.StoryModal.story.storyItem.content.button());
        await browser.yaAssertView('creating-scenario', 'body');
    });

    it('Когда важно не шуметь', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Когда важно не шуметь
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Когда важно не шуметь');

        // screenshot: Отображается сторя без кнопки [whisper]
        await browser.yaAssertView('whisper', 'body');
    });

    it('Соберите тандем - с одним модулем', async function() {
        const { browser, PO } = this;

        // Добавлена еще одна колонка, чтобы сработал переход на полный визард создания тандема
        await browser.yaAddSpeakers([new YandexStation()]);
        await browser.yaOpenPage('', PO.IotHome());

        // do: Добавить на аккаунт модуль (должен быть только 1 модуль)
        // (сделано в beforeEach)

        // do: Тапнуть на сторю Соберите тандем
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Соберите тандем');

        // screenshot: Отображается сторя c кнопкой [tandem-1]
        await browser.yaAssertView('tandem-1', 'body');

        // assert: Тап на кнопку ведёт в настройки тандема с модулем
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.includes('/tandem-setup/'), 'Ожидалась ссылка на создание тандема');

        await browser.click(PO.StoryModal.story.storyItem.content.button());
        await browser.yaAssertView('tandem-setup-1', 'body');
    });

    it('Соберите тандем - больше одного модуля', async function() {
        const { browser, PO } = this;

        // do: Добавить на аккаунт больше 1 модуля
        await browser.yaAddSpeakers([new YandexModule2()]);
        await browser.yaOpenPage('', PO.IotHome());

        // do: Тапнуть на сторю Соберите тандем
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Соберите тандем');

        // screenshot: Отображается сторя без кнопки [tandem-2]
        await browser.yaAssertView('tandem-2', 'body');
    });

    it('Эквалайзер', async function() {
        const { browser, PO } = this;

        await browser.yaAddSpeakers([new YandexStation()]);
        await browser.yaOpenPage('', PO.IotHome());

        // do: Тапнуть на сторю Настройте эквалайзер
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), ['Звук по вашим правилам', 'Настройте эквалайзер']);

        // screenshot: Отображается сторя [equalizer]
        await browser.yaAssertView('equalizer', 'body');
    });

    it('Пульт от музыки', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Пульт от музыки
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), ['Управляйте музыкой на колонке', 'Пульт от музыки']);

        // screenshot: Отображается сторя [muspult-1]
        await browser.yaAssertView('muspult-1', 'body');

        // do: Перейти на следующую сторю
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя с кнопкой [muspult-2]
        await browser.yaAssertView('muspult-2', 'body');

        // assert: При установленном приложении Я.Музыки открывается плейлист с видеошотами (https://music.app.link/0GDkApJamjb) и сразу запускается первый трек; При не установленной Я.Музыке ссылка ведет на AppStore/Google Play; Оба варианта ок
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link === 'https://music.app.link/0GDkApJamjb', 'Ожидалась ссылка на приложение Я.Музыки');
    });

    it('Дома', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Все по домам
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Все по домам');

        // screenshot: Отображается сторя [households]
        await browser.yaAssertView('households', 'body');
    });

    it('Уведомления', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Расскажу о новом
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Расскажу о новом');

        // screenshot: Отображается сторя с кнопкой [notifications-1]
        await browser.yaAssertView('notifications-1', 'body');

        // assert: Тап на Настроить уведомления ведёт на страницу настроек уведомлений

        // do: Вернуться назад, тапнуть на картинку
        await browser.yaShowNextStory();

        // screenshot: Отображается следующая сторя с кнопкой [notifications-2]
        await browser.yaAssertView('notifications-2', 'body');

        // assert: Тап на Настроить уведомления ведёт на страницу настроек уведомлений
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.includes('/account/alice-subscriptions'), 'Ожидалась ссылка на страницу настроек уведомлений');

        await browser.click(PO.StoryModal.story.storyItem.content.button());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaAssertView('settings', PO.BottomSheetContent());
    });

    it('Лампочка', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Волшебная лампа
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Волшебная лампа');

        // screenshot: Отображается сторя [lamp]
        await browser.yaAssertView('lamp', 'body');
    });

    // Закоменченно, т.к. не работает проброс эксп флагов
    // it('Группы колонок', async function() {
    //     const { browser, PO } = this;
    //
    //     await browser.yaAddSpeakers([new YandexStation()]);
    //
    //     await browser.yaEnableExpFlags(['multiroom_groups']);
    //     await browser.yaOpenPage('', PO.IotHome());
    //
    //     // do: Тапнуть на сторю Группы колонок
    //     await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
    //     await browser.waitForVisible(PO.StoriesGrid());
    //     await browser.yaWaitBottomSheetAnimation();
    //     await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Группы колонок');
    //
    //     // screenshot: Отображается сторя с кнопкой [multiroom]
    //     await browser.yaAssertView('multiroom', 'body');
    //
    //     // assert: Тап на кнопку ведёт на страницу создания группы
    //     const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
    //     assert(link.includes('/iot/settings/groups/add'), 'Ожидалась ссылка на страницу создания группы');
    //
    //     await browser.click(PO.StoryModal.story.storyItem.content.button());
    //     await browser.yaWaitBottomSheetAnimation();
    //     await browser.yaAssertView('settings', PO.BottomSheetContent());
    // });

    it('Расписания', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Расписания
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Расписания');

        // screenshot: Отображается сторя с кнопкой [timetable-1]
        await browser.yaAssertView('timetable-1', 'body');

        // assert: Тап на Настроить расписание ведёт на страницу создания сценария
        const link1 = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link1.includes('/iot/scenario/add'), 'Ожидалась ссылка на страницу создания сценария');

        // do: Вернуться назад, тапнуть на картинку
        await browser.yaShowNextStory();

        // screenshot: Отображается следующая сторя с кнопкой [timetable-2]
        await browser.yaAssertView('timetable-2', 'body');

        // assert: Тап на Настроить расписание ведёт на страницу создания сценария
        const link2 = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link2.includes('/iot/scenario/add'), 'Ожидалась ссылка на страницу создания сценария');

        await browser.click(PO.StoryModal.story.storyItem.content.button());
        await browser.yaAssertView('scenario-timetable-create', 'body');
    });

    it('Как там дома', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Как там дома
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Как там дома');

        // screenshot: Отображается сторя [voice-status]
        await browser.yaAssertView('voice-status', 'body');
    });

    it('Команды с таймером', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Команды с таймером
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Команды с таймером');

        // screenshot: Отображается сторя [timer-1]
        await browser.yaAssertView('timer-1', 'body');

        // do: Тапнуть на картинку
        await browser.yaShowNextStory();

        // screenshot: Отображается следующая сторя [timer-2]
        await browser.yaAssertView('timer-2', 'body');
    });

    it('Сценарии', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Одна фраза для всего
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Одна фраза для всего');

        // screenshot: Отображается сторя с кнопкой [scenario]
        await browser.yaAssertView('scenario', 'body');

        // assert: Тап на Создать сценарий ведёт на страницу создания сценария
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.includes('/iot/scenario/add'), 'Ожидалась ссылка на страницу создания сценария');

        await browser.click(PO.StoryModal.story.storyItem.content.button());
        await browser.yaAssertView('scenario-create', 'body');
    });

    it('Музыка', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Получайте от Музыки максимум удовольствия
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), ['Получайте от Музыки максимум', 'Максимум музыки']);

        // screenshot: Отображается сторя Признайтесь что вы любите [music-1]
        await browser.yaAssertView('music-1', 'body');

        await browser.yaShowNextStory();

        // do: Тапнуть на картинку чтобы пролисталась сторя
        // screenshot: Отображается сторя Концерт любимой музыки [music-2]
        await browser.yaAssertView('music-2', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Ничего если забыли название [music-3]
        await browser.yaAssertView('music-3', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Музыка под настроение [music-4]
        await browser.yaAssertView('music-4', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Слушайте что нравится [music-5]
        await browser.yaAssertView('music-5', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Весь вечер на повторе [music-6]
        await browser.yaAssertView('music-6', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Сладких снов [music-7]
        await browser.yaAssertView('music-7', 'body');
    });

    it('Будильники', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Вставайте с той ноги
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), ['Просыпайтесь и засыпайте', 'Вставайте с той ноги']);

        // screenshot: Отображается сторя Просыпайтесь вовремя [alarm-1]
        await browser.yaAssertView('alarm-1', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Начините день с музыки [alarm-2]
        await browser.yaAssertView('alarm-2', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Расслабьтесь перед сном [alarm-3]
        await browser.yaAssertView('alarm-3', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Колонка тоже может уснуть [alarm-4]
        await browser.yaAssertView('alarm-4', 'body');
    });

    it('Таймеры и дела', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Таймеры, списки дел и покупок
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), ['Таймеры, списки дел', 'Как всё успеть']);

        // screenshot: Отображается сторя Как не сжечь пирог [timer-1]
        await browser.yaAssertView('timer-1', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Как ничего не забыть [timer-2]
        await browser.yaAssertView('timer-2', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Как планировать покупки [timer-3]
        await browser.yaAssertView('timer-3', 'body');
    });

    it('Новости и шоу', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Новости и шоу Алисы
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), ['новости и шоу Алисы', 'слушайте интересные вам новости и шоу Алисы']);

        // screenshot: Отображается сторя про Утреннее шоу [show-1]
        await browser.yaAssertView('show-1', 'body');

        // do: Тапнуть на картинку чтобы пролисталась сторя
        await browser.yaShowNextStory();

        // screenshot: Отображается сторя Темы и новости с кнопкой [show-2]
        await browser.yaAssertView('show-2', 'body');

        // assert: Тап на Настроить шоу Алисы ведёт на страницу настроек шоу
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.includes('/account/show'), 'Ожидалась ссылка на страницу настроек шоу');

        await browser.click(PO.StoryModal.story.storyItem.content.button());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaAssertView('settings', PO.BottomSheetContent());
    });

    it('Навыки', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Тысячи навыков
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), ['Тысячи навыков', 'Игры, тренажёры, рецепты']);

        // screenshot: Отображается сторя про навыки с кнопкой [skills]
        await browser.yaAssertView('skills', 'body');

        // assert: Тап на Перейти в каталог навыков ведёт на сайт Яндекс Диалоги во внешнем браузере со списком навыков Алисы
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.includes('https://dialogs.yandex.ru/store/'), 'Ожидалась ссылка на сайт Яндекс Диалоги');
    });

    it('Биометрия', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Только ваша
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), ['Только ваша', 'Узнает вас по голосу']);

        // screenshot: Отображается сторя про запоминание голоса [biometry]
        await browser.yaAssertView('biometry', 'body');
    });

    it('Детский режим', async function() {
        const { browser, PO } = this;

        // do: Тапнуть на сторю Если у вас есть дети
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), ['Если у вас дети', 'Если есть ребенок']);

        // screenshot: Отображается сторя про настройки режима поиска с кнопкой [child-mode]
        await browser.yaAssertView('child-mode', 'body');

        // assert: Тап на Перейти в настройки ведёт на настройки режимов поиска
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.includes('/account/content-access'), 'Ожидалась ссылка на настройки режимов поиска');

        await browser.click(PO.StoryModal.story.storyItem.content.button());
        await browser.yaWaitBottomSheetAnimation();
        await browser.yaAssertView('settings', PO.BottomSheetContent());
    });
});
