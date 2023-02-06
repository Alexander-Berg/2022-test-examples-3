const { URL } = require('url');
const { assert } = require('chai');

const {
    testUsers: {
        serviceAdmin: admin1,
        serviceAdminWithSubs: admin2,
        simpleUser: user,
    },
    cacheMode,
} = hermione.ctx;

const serviceLink = {
    webmaster: 'Перейти в Вебмастер',
    direct: 'Перейти в сервис',
    yamb: 'Перейти в сервис',
    disk: 'Перейти в Диск',
    yandexsprav: 'Перейти в Справочник',
    metrika: 'Перейти в сервис',
    mail: 'Перейти в сервис',
    staff: 'Перейти в сервис',
    calendar: 'Перейти в сервис',
    forms: 'Перейти в сервис',
    wiki: 'Перейти в Вики',
};

const mainAction = {
    mail: 'Написать письмо',
    staff: 'Редактировать профиль',
    calendar: 'Создать событие',
    forms: 'Создать форму',
    wiki: 'Создать страницу',
};

function disabling(browser, count) {
    return browser
        // на странице сервиса в верхнем правом углу нажать на экшен "Отключить"
        .setHash('disable-1')
        .click('.service-header__actions .service-toggle_type_disable')

        // появился попап "Уверены, что хотите отключить сервис?" [confirm-disabling]
        .waitForVisible('.modal_visible_yes')
        .assertView(`confirm-disabling-${count}`, '.modal_visible_yes .modal__content')

        // нажать кнопку "Отключить"
        .click('.modal_visible_yes .modal__button_type_confirm')

        // попап закрылся
        .waitForVisible('.modal_visible_yes', 1000, true)

        // через время статус изменился на "сервис отключен" с красным кружком
        .waitForExactText('.service-header__status .service-status_disabled', 'сервис отключен',
            cacheMode === 'read' ? 20000 : 200000)

        // в верхнем правом углу отображается кнопка "Подключить"
        .waitForExactText('.service-header__actions .service-toggle_type_enable', 'Подключить');
}

function trackerDisabling(browser, count) {
    return browser
        // на странице сервиса в верхнем правом углу нажать на экшен "Отключить"
        .click('.service-header__actions .service-toggle_type_disable')

        // появился попап "Хотите отключить Трекер?" [confirm2]
        .waitForVisible('.modal_visible_yes')

        // нажать кнопку "Отключить"
        .setHash(`disable-${count}`)
        .click('.modal_visible_yes .modal__button_type_confirm')

        // попап закрылся
        .waitForVisible('.modal_visible_yes', 1000, true)

        // через время статус изменился на "сервис отключен" с красным кружком
        .waitForExactText('.service-header__status .service-status', 'сервис отключен',
            cacheMode === 'read' ? 20000 : 200000)

        // в верхнем правом углу отображается кнопка "Подключить"
        .waitForVisible('.service-header__actions .service-toggle_type_enable')
        .waitForExactText('.service-header__actions .service-toggle_type_enable', 'Подключить')

        // вкладки "Подписки", "Запросы", "Настройки сервиса" не отображаются
        .waitForVisible('.tab-link__group', 1000, true);
}

function enablingFromServicePage(browser) {
    return browser
        // нажать кнопку "Подключить"
        .setHash('enable-1')
        .click('.service-header__actions .service-toggle_type_enable')

        // через время статус изменился на "сервис подключен" с зеленым кружком
        .waitForExactText('.service-header__status .service-status_enabled', 'сервис подключен',
            cacheMode === 'read' ? 20000 : 200000);
}

function enablingFromDashboard(browser, serviceSlug) {
    return browser
        // перейти на страницу дашборда {{CONNECT_HOST}}/portal/home
        .url('/portal/home')

        // на плитке отображается экшен "+ Подключить сервис"
        .waitForExactText(`.dashboard-card[data-slug="${serviceSlug}"] .dashboard-card__action`, 'Подключить сервис')

        // внешний вид экшена [action-enable]
        .assertView('action-enable', `.dashboard-card[data-slug="${serviceSlug}"] .dashboard-card__footer`)

        // нажать на экшен "+ Подключить сервис"
        .setHash('enable-2')
        .click(`.dashboard-card[data-slug="${serviceSlug}"] .dashboard-card__action.link`);
}

function enablingAndDisabling(browser, serviceSlug) {
    return browser
        // авторизоваться под админом организации
        // перейти на страницу сервиса {{CONNECT_HOST}}/portal/services/<serviceSlug>
        .login({ ...admin1, retpath: `/portal/services/${serviceSlug}` })
        .waitForVisible('.service-page')
        .disableAnimations('*')

        // открыта страница сервиса с экшенами [enabled]
        .assertView('enabled', '.service-page .service-header')
        .then(() => disabling(browser, 1))

        // открыта страница сервиса без экшенов [disabled]
        .assertView('disabled', '.service-page .service-header')
        .then(() => enablingFromServicePage(browser))

        // отображаются экшены "<mainAction>", "<serviceLink>", "Отключить"
        .waitForExactText('.service-header__actions .link', mainAction[serviceSlug])
        .waitForExactText('.service-header__actions .link', serviceLink[serviceSlug])
        .waitForExactText('.service-header__actions .link', 'Отключить')
        .then(() => disabling(browser, 2))
        .then(() => enablingFromDashboard(browser, serviceSlug))

        // статус изменился на экшен "+ Редактировать профиль"
        .waitForExactText(`.dashboard-card[data-slug="${serviceSlug}"] .dashboard-card__action`,
            mainAction[serviceSlug], 20000)

        // внешний вид экшена [action]
        .assertView('action', `.dashboard-card[data-slug="${serviceSlug}"] .dashboard-card__footer`);
}

function alwaysEnabled(browser, serviceSlug) {
    return browser
        // авторизоваться под админом организации
        // перейти на страницу сервиса {{CONNECT_HOST}}/portal/services/<serviceSlug>
        .login({ ...admin1, retpath: `/portal/services/${serviceSlug}` })
        .waitForVisible('.service-page')
        .disableAnimations('*')

        // открыта страница сервиса с экшенами [enabled]
        .assertView('enabled', '.service-page .service-header')
        .waitForExactText('.service-header__actions .link', serviceLink[serviceSlug])

        .waitForVisible('.service-header__actions .service-toggle_type_disable', 1000, true);
}

function cannotManageService(browser, serviceSlug) {
    return browser
        // авторизоваться под сотрудником организации
        // перейти на страницу сервиса {{CONNECT_HOST}}/portal/services/<serviceSlug>
        .login({ ...user, retpath: `/portal/services/${serviceSlug}` })
        .waitForVisible('.service-page')
        .disableAnimations('*')

        // открыта страница сервиса с экшенами [enabled]
        .assertView('enabled', '.service-page .service-header')

        // нет экшена "Отключить"
        .waitForVisible('.service-header__actions .service-toggle_type_disable', 1000, true);
}

describe('Включение и отключение сервисов', () => {
    describe('Админ', () => {
        it('1. Неотключаемый сервис Почта', function() {
            /* alias: admin-1-mail */
            return this.browser
                // авторизоваться под сотрудником организации
                // перейти на страницу сервиса {{CONNECT_HOST}}/portal/services/<serviceSlug>
                // Ожидаем, что произойдёт редирект в админку PDB
                .login({ ...admin1, retpath: '/portal/services/mail', waitElement: 'body' })
                .getUrl()
                .then(url => {
                    const { searchParams } = new URL(url);
                    const retpath = decodeURIComponent(searchParams.get('retpath'));
                    const retpathUrl = new URL(retpath);
                    const { hostname } = retpathUrl;

                    assert.equal(hostname, 'admin.yandex.ru');
                });
        });

        it('2. Отключение и включение сервиса Люди', function() {
            /* alias: admin-2-staff */
            return this.browser
                .then(() => enablingAndDisabling(this.browser, 'staff'));
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('3. Отключение и включение сервиса Вики', function() {
            /* alias: admin-3-wiki */
            return this.browser
                .then(() => enablingAndDisabling(this.browser, 'wiki'));
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('4. Отключение и включение сервиса Формы', function() {
            /* alias: admin-4-forms */
            return this.browser
                .then(() => enablingAndDisabling(this.browser, 'forms'));
        });

        it('5. Неотключаемый сервис Календарь', function() {
            /* alias: admin-5-calendar */
            return this.browser
                .then(() => alwaysEnabled(this.browser, 'calendar'));
        });

        it('6. Неотключаемый сервис Вебмастер', function() {
            /* alias: admin-6-webmaster */
            return this.browser
                .then(() => alwaysEnabled(this.browser, 'webmaster'));
        });

        it('8. Неотключаемый сервис Чаты', function() {
            /* alias: admin-8-yamb */
            return this.browser
                .then(() => alwaysEnabled(this.browser, 'yamb'));
        });

        it('9. Неотключаемый сервис Диск', function() {
            /* alias: admin-9-disk */
            return this.browser
                .then(() => alwaysEnabled(this.browser, 'disk'));
        });

        it('10. Неотключаемый сервис Справочник', function() {
            /* alias: admin-10-yandexsprav */
            return this.browser
                .then(() => alwaysEnabled(this.browser, 'yandexsprav'));
        });

        it('11. Неотключаемый сервис Метрика', function() {
            /* alias: admin-11-metrika */
            return this.browser
                .then(() => alwaysEnabled(this.browser, 'metrika'));
        });

        it('12. Проверка работы попапа отключения сервиса Люди', function() {
            /* alias: admin-12-confirm */
            return this.browser
                // перейти на страницу сервиса {{CONNECT_HOST}}/portal/services/staff
                .login({ ...admin1, retpath: '/portal/services/staff' })
                .waitForVisible('.service-page')
                .disableAnimations('*')

                // нажать на экшен "Отключить"
                .click('.service-header__actions .service-toggle_type_disable')

                // появился попап "Уверены, что хотите отключить сервис?"
                .waitForVisible('.modal_visible_yes')

                // нажать на кнопку "Отменить"
                .click('.modal_visible_yes .modal__button.modal__button_type_cancel')

                // попап закрылся
                .waitForVisible('.modal_visible_yes', 1000, true)

                // отображаются экшены "+ Редактировать профиль", "Перейти в Люди", "Отключить"
                .waitForExactText('.service-header__actions .link', 'Редактировать профиль')
                .waitForExactText('.service-header__actions .link', 'Перейти в сервис')
                .waitForExactText('.service-header__actions .link', 'Отключить')

                // нажать на экшен "Отключить"
                .click('.service-header__actions .service-toggle_type_disable')

                // появился попап "Уверены, что хотите отключить сервис?"
                .waitForVisible('.modal_visible_yes')

                // нажать на элемент попапа "крестик"
                .click('.modal_visible_yes .icon_type_cross')

                // попап закрылся
                .waitForVisible('.modal_visible_yes', 1000, true)

                // отображаются экшены "+ Редактировать профиль", "Перейти в Люди", "Отключить"
                .waitForExactText('.service-header__actions .link', 'Редактировать профиль')
                .waitForExactText('.service-header__actions .link', 'Перейти в сервис')
                .waitForExactText('.service-header__actions .link', 'Отключить');
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('13. Отключение и включение сервиса Трекер', function() {
            /* alias: admin-13-tracker */
            return this.browser
                // авторизоваться под админом организации
                // перейти на страницу сервиса {{CONNECT_HOST}}/portal/services/tracker
                .login({ ...admin1, retpath: '/portal/services/tracker' })
                .waitForVisible('.subscription-service-page')
                .disableAnimations('*')

                // открыта страница Трекера с экшенами и три вкладки [enabled]
                .assertView('enabled', '.subscription-service-page .service-page__content')

                // статус сервиса "сервис подключен" с зеленым кружком
                .waitForExactText('.service-status_enabled', 'сервис подключен', 20000)

                .then(() => trackerDisabling(this.browser, 1))

                // открыта страница Трекера с кнопкой "Подключить" [disabled]
                .assertView('disabled', '.service-page .service-header')

                // нажать на кнопку "Подключить"
                .click('.service-header__actions .service-toggle_type_enable')

                // появился попап "Подключение Трекера" [confirm]
                .waitForVisible('.modal_visible_yes')
                .assertView('confirm', '.modal_visible_yes .modal__content')

                // нажать на "крестик"
                .click('.modal_visible_yes .modal__close-button')

                // попап закрылся
                .waitForVisible('.modal_visible_yes', 1000, true)

                // нажать на кнопку "Подключить"
                .click('.service-header__actions .service-toggle_type_enable')

                // появился попап "Подключение Трекера"
                .waitForVisible('.modal_visible_yes')

                // нажать на кнопку "Включить"
                .setHash('enable-1')
                .click('.modal_visible_yes .modal__button_type_confirm')

                // попап "Подключение Трекера" закрылся
                .waitForVisible('.modal_visible_yes', 1000, true)

                // через время статус изменился на "сервис подключен" с зеленым кружком
                .waitForExactText('.service-status_enabled', 'сервис подключен',
                    cacheMode === 'read' ? 20000 : 200000)

                // появилась плашка "Только чтение"
                .waitForExactText('.service-header .service-badge', 'Только чтение')

                // отображаются экшены "+ Создать задачу", "Перейти в Трекер", "Отключить"
                .waitForExactText('.service-header__actions .control', 'Создать задачу')
                .waitForExactText('.service-header__actions .control', 'Перейти в Трекер')
                .waitForExactText('.service-header__actions .control', 'Отключить')

                // отображаются три вкладки "Подписки", "Запросы", "Настройки сервиса"
                .waitForVisible('.tab-link__group')

                .then(() => trackerDisabling(this.browser, 2))

                // не отображается плашка "Только чтение"
                .waitForVisible('.service-header .service-badge', 1000, true)

                // перейти на страницу дашборда {{CONNECT_HOST}}/portal/home
                .url('/portal/home')

                // на плитке отображается экшен "+ Подключить сервис"
                .waitForExactText('.dashboard-card[data-slug="tracker"] .dashboard-card__action',
                    'Подключить сервис')

                // внешний вид экшена [action-enable]
                .assertView('action-enable', '.dashboard-card[data-slug="tracker"] .dashboard-card__footer')

                // нажать на экшен "+ Подключить сервис"
                .click('.dashboard-card[data-slug="tracker"] .dashboard-card__action.link')

                // появился попап "Подключение Трекера"
                .waitForVisible('.modal_visible_yes .subscription-service-toggle__content')

                // нажать на кнопку "Подключить"
                .setHash('enable-2')
                .click('.modal_visible_yes .modal__button_type_confirm')

                // попап закрылся
                .waitForVisible('.modal_visible_yes .subscription-service-toggle__content', 1000, true)

                // через время появился экшен "+ Создать задачу"
                .waitForExactText('.dashboard-card[data-slug="tracker"] .dashboard-card__action',
                    'Создать задачу', 20000)

                // внешний вид экшена [action-create]
                .assertView('action-create', '.dashboard-card[data-slug="tracker"] .dashboard-card__footer')

                // появилась плашка "Только чтение"
                .waitForExactText('.dashboard-card[data-slug="tracker"] .dashboard-card__badge', 'Только чтение');
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('14. Отключение и включение сервиса Трекер когда есть подписки', function() {
            /* alias: admin-14-subs */
            return this.browser
                // авторизоваться под админом в организацией с подписками
                // перейти на страницу сервиса {{CONNECT_HOST}}/portal/services/tracker
                .login({ ...admin2, retpath: '/portal/services/tracker' })

                // открыта страница Трекера с экшенами и три вкладки
                .waitForVisible('.subscription-service-page')
                .disableAnimations('*')

                .then(() => trackerDisabling(this.browser, 1))

                // перейти на страницу дашборда {{CONNECT_HOST}}/portal/home
                .url('/portal/home')
                .waitForVisible('.dashboard__tile')
                .disableAnimations('*')

                // на плитке отображается экшен "+ Подключить сервис"
                .waitForExactText('.dashboard-card[data-slug="tracker"] .dashboard-card__action',
                    'Подключить сервис')

                // нажать на экшен "+ Подключить сервис"
                .click('.dashboard-card[data-slug="tracker"] .dashboard-card__action.link')

                // появился попап "Подключение Трекера" с тремя кнопками
                // "Включить с подписками", "Включить без подписок" и "Отменить"
                .waitForVisible('.modal_visible_yes.subscription-service-toggle')
                .assertView('confirm', '.modal_visible_yes .modal__content')

                // нажать на кнопку "отмена"
                .click('.modal_visible_yes .subscription-service-toggle__button_type_cancel')

                // попап закрылся
                .waitForVisible('.modal_visible_yes.subscription-service-toggle', 1000, true)

                // на плитке отображается экшен "+ Подключить сервис"
                .waitForExactText('.dashboard-card[data-slug="tracker"] .dashboard-card__action',
                    'Подключить сервис')

                // нажать на экшен "+ Подключить сервис"
                .click('.dashboard-card[data-slug="tracker"] .dashboard-card__action.link')

                // появился попап "Подключение Трекера"
                .waitForVisible('.modal_visible_yes.subscription-service-toggle')

                // нажать на кнопку  "Включить с подписками"
                .setHash('enable')
                .click('.modal_visible_yes .subscription-service-toggle__button_type_enable_with_subs')

                // попап закрылся
                .waitForVisible('.modal_visible_yes.subscription-service-toggle', 1000, true)

                // через время появился экшен "+ Создать задачу"
                .waitForExactText('.dashboard-card[data-slug="tracker"] .dashboard-card__action',
                    'Создать задачу', 20000)

                // перейти на страницу сервиса {{CONNECT_HOST}}/portal/services/tracker
                .url('/portal/services/tracker')

                // открыта страница Трекера с экшенами и тремя вкладками
                .waitForVisible('.subscription-service-page')
                .disableAnimations('*')
                .waitForExactText('.service-header__actions .control', 'Создать задачу')
                .waitForExactText('.service-header__actions .control', 'Перейти в Трекер')
                .waitForExactText('.service-header__actions .control', 'Отключить')
                .waitForVisible('.tab-link__group')

                // на странице Трекера отображаются ранее выданные подписки [subs]
                .waitForVisible('.service-subscribers__table', 5000)
                .assertView('subs', '.subscription-service-page .service-page__content');
        });
    });

    describe('Пользователь', () => {
        it('1. Нельзя отключить сервис Метрика', function() {
            /* alias: user-1-metrika */
            return this.browser
                .then(() => cannotManageService(this.browser, 'metrika'));
        });

        hermione.skip.in(/.*/, 'Всегда редиректим в Почту DIR-10291');
        it('2. Нельзя отключить сервис Почта', function() {
            /* alias: user-2-mail */
            return this.browser
                .then(() => cannotManageService(this.browser, 'mail'));
        });

        it('4. Нельзя отключить сервис Чаты', function() {
            /* alias: user-4-yamb */
            return this.browser
                .then(() => cannotManageService(this.browser, 'yamb'));
        });

        it('5. Нельзя отключить сервис Диск', function() {
            /* alias: user-5-disk */
            return this.browser
                .then(() => cannotManageService(this.browser, 'disk'));
        });

        it('6. Нельзя отключить сервис Люди', function() {
            /* alias: user-6-staff */
            return this.browser
                .then(() => cannotManageService(this.browser, 'staff'));
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('7. Нельзя отключить сервис Трекер', function() {
            /* alias: user-7-tracker */
            return this.browser
                .then(() => cannotManageService(this.browser, 'tracker'));
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('8. Нельзя отключить сервис Вики', function() {
            /* alias: user-8-wiki */
            return this.browser
                .then(() => cannotManageService(this.browser, 'wiki'));
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('9. Нельзя отключить сервис Формы', function() {
            /* alias: user-9-forms */
            return this.browser
                .then(() => cannotManageService(this.browser, 'forms'));
        });

        it('10. Нельзя отключить сервис Календарь', function() {
            /* alias: user-10-calendar */
            return this.browser
                .then(() => cannotManageService(this.browser, 'calendar'));
        });

        it('11. Нельзя отключить сервис Справочник', function() {
            /* alias: user-11-yandexsprav */
            return this.browser
                .then(() => cannotManageService(this.browser, 'yandexsprav'));
        });
    });
});
