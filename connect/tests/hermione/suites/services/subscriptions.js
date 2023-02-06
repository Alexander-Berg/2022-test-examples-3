const { URL } = require('url');
const { assert } = require('chai');
const { trackerAdmin: user1, adminUITest2: user2, simpleUser: user3, adminYO3: user4 } = hermione.ctx.testUsers;
const { cacheMode } = hermione.ctx;
const { removeTrackerLicenses } = require('../../helpers/directoryApi');

describe('Подписки на странице сервиса "Трекер"', () => {
    describe('Положительные', () => {
        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('1. Добавление и отзыв подписок', function() {
            let prepareData = Promise.resolve();

            if (cacheMode === 'write') {
                prepareData = removeTrackerLicenses(user1);
            }

            /* alias: pos-1-add-del */

            return prepareData
                .then(() => this.browser
                    // авторизоваться под администратором организации без подписок с правом редактировать подписки (1)
                    // открыть страницу сервиса "Трекер" {{CONNECT_HOST}}/portal/services/tracker
                    .login({ ...user1, retpath: '/portal/services/tracker' })

                    // отображается страница без подписок
                    .waitForVisible('.service-subscribers_empty')
                    .disableAnimations('*')
                    .hideCaret()
                    .stopServiceStatusPolling()

                    // внешний вид вкладки "Подписки" при отсутствии подписок [plain]
                    .assertView('plain', '.tab-link__pane')

                    // нажать на кнопку "Оформить подписку"
                    .click('.service-subscribers_empty .service-subscribers__button_role_add-subscription')

                    // окно с переходом в заполнение платежной информации не отображается
                    .waitForVisible('.modal_visible_yes.no-billing-contract-modal', 1000, true)

                    // появилось модальное окно "Оформление подписок"
                    .waitForVisible('.modal_visible_yes.subscription-modal', 2000)
                    .waitForVisible('.calculated-service-pricing .spin2', 20000, true)

                    // в модальном окне "Оформление подписок" отображается кнопка "Рассчитать для всех сотрудников"
                    .waitForVisible('.modal_visible_yes .subscription-modal__button_role_add-all', 1000)

                    // кнопка "Оформить подписки" не отображается
                    .waitForVisible('.modal_visible_yes .subscription-modal__button_role_apply', 1000, true)

                    // внешний вид модального окна "Оформление подписок" [modal-plain]
                    .assertView('modal-plain', '.modal_visible_yes.subscription-modal .modal__content')

                    // нажать на кнопку "Рассчитать для всех сотрудников"
                    .setHash('addAll')
                    .click('.modal_visible_yes .subscription-modal__button_role_add-all')

                    // в подвале модального окна поменялся тариф
                    // .then(() => checkAmount(this.browser, '0 ₽ за сотрудника в месяц'))
                    .waitForVisible('.calculated-service-pricing .spin2', 20000, true)
                    // eslint-disable-next-line max-len
                    .waitForExactText(
                        '.calculated-service-pricing .pricing-details__slot:first-child .currency-amount',
                        '0 ₽', 1000, true
                    )

                    // внешний вид модального окна "Оформление подписок" [modal-all]
                    .assertView('modal-all', '.modal_visible_yes.subscription-modal .modal__content')

                    // удалить подписку на "Всех сотрудников" кликом по кнопке с мусорным бачком
                    .setHash('rmAll')
                    .click('.subscription-modal__control_role_remove-item')

                    // выбрать трёх пользователей из саджеста
                    .selectFromSuggest('.modal_visible_yes .search-input .textinput__control', 'four four')
                    .setHash('suggest1')
                    .selectFromSuggest('.modal_visible_yes .search-input .textinput__control', 'five five')
                    .setHash('suggest2')
                    .selectFromSuggest('.modal_visible_yes .search-input .textinput__control', 'dva dva')
                    .setHash('suggest3')

                    // отображается кнопка "Оформить подписки"
                    .waitForVisible('.modal_visible_yes .subscription-modal__button_role_apply', 1000)

                    // в подвале модального окна отображается расчёт тарифа - "0 ₽ всего в месяц"
                    .waitForVisible('.calculated-service-pricing .spin2', 20000, true)
                    // eslint-disable-next-line max-len
                    .waitForExactText(
                        '.calculated-service-pricing .pricing-details__slot:first-child .currency-amount',
                        '0 ₽', 1000, true
                    )

                    // внешний вид модального окна "Оформление подписок" [modal-some]
                    .waitForVisible('.subscription-modal__list-item:nth-child(1) .avatar-unit__avatar', 2000)
                    .waitForVisible('.subscription-modal__list-item:nth-child(2) .avatar-unit__avatar', 2000)
                    .waitForVisible('.subscription-modal__list-item:nth-child(3) .avatar-unit__avatar', 2000)
                    .assertView('modal-some', '.modal_visible_yes.subscription-modal .modal__content')

                    // нажать на кнопку "Оформить подписки" в модальном окне
                    .setHash('add')
                    .click('.modal_visible_yes .subscription-modal__button_role_apply')

                    // отображается сообщение "Подписки успешно оформлены" [notification]
                    .closeSuccessNotify('notification')

                    // на вкладке "Подписки" отображается количество подписок "3 сотрудника"
                    .waitForExactText('.service-subscribers__table-header-count', '3 сотрудника', 20000)

                    // на вкладке "Подписки" отображаются добавленные подписки [subscribers]
                    .assertView('subscribers', '.service-subscribers__table')

                    // при наведении на подписку отображается кнопка "Отозвать подписку"
                    // eslint-disable-next-line max-len
                    .click('.service-subscribers__table .table__body .table__row:first-child .table__cell_role_subscriber')

                    // внешний вид вкладки "Подписки" с кнопкой "Отозвать подписку" [subscribers-button]
                    .assertView('subscribers-button', '.service-subscribers')

                    .getText('.service-subscribers__table .table__body .table__row:first-child .avatar-unit__name')
                    .then(name => {
                        this.browser.setMeta('nameForDel', name);
                    })

                    // нажать на кнопку "Отозвать подписку" на первой строке из списка
                    .setHash('del1')
                    // eslint-disable-next-line max-len
                    .click('.service-subscribers__table .table__body .table__row:first-child .service-subscribers__button_role_remove-item')

                    // отображается сообщение "Подписки успешно отозваны" [notification-del]
                    .closeSuccessNotify('notification-del')

                    // подписка удалилась из списка
                    .getText('.service-subscribers__table .table__body .table__row:first-child .avatar-unit__name')
                    .then(text => this.browser.getMeta('nameForDel').then(name => {
                        assert.notEqual(name, text);
                    }))

                    // внешний вид вкладки "Подписки" [subscribers-del]
                    .assertView('subscribers-del', '.service-subscribers')

                    // нажать на галочку около количества подписок
                    .click('.service-subscribers__checkbox_role_select-all')

                    // выделились все подписки
                    // eslint-disable-next-line max-len
                    .waitForExist('.service-subscribers__checkbox_role_select-item:not(.checkbox_checked_yes)', 1000, true)

                    // сверху на странице появилось сообщение о выбранных подписках с кнопкой "Отозвать подписки"
                    .waitForVisible('.selection-bar_visible')

                    // показывается количество выбранных сотрудников
                    .waitForExactText('.selection-bar__status', 'Выбраны 2 подписки', 5000)

                    // внешний вид сообщения о выбранных подписках [subscribers-select]
                    .disableBoxShadow('.selection-bar')
                    .assertView('subscribers-select', '.selection-bar')

                    // отжать галочку у первой подписки
                    // eslint-disable-next-line max-len
                    .click('.service-subscribers__table .table__body .table__row:last-child .service-subscribers__checkbox_role_select-item')

                    // количество выбранных сотрудников на сообщении внутри страницы пересчиталось
                    .waitForExactText('.selection-bar__status', 'Выбрана 1 подписка', 5000)

                    // внешний вид сообщения о выбранных подписках [subscribers-unselect]
                    .assertView('subscribers-unselect', '.selection-bar')

                    // нажать на кнопку "Отозвать подписки" в сообщении о выбранных подписках
                    .setHash('del2')
                    .click('.selection-bar .service-subscribers__button_role_remove-selected')

                    // отображается сообщение "Подписка успешно отозвана" [notification-del2]
                    // нажать на крестик
                    .closeSuccessNotify('notification-del2')

                    // на вкладке подписок осталась одна строка с подпиской [subscribers-last]
                    .waitForExactText('.service-subscribers__table-header-count', '1 сотрудник', 5000)
                    .assertView('subscribers-last', '.service-subscribers')

                    // нажать на кнопку "Отозвать подписку" на единственной строке из списка
                    .setHash('del3')
                    // eslint-disable-next-line max-len
                    .click('.service-subscribers__table .table__body .table__row:first-child .table__cell_role_subscriber')
                    // eslint-disable-next-line max-len
                    .click('.service-subscribers__table .table__body .table__row:first-child .service-subscribers__button_role_remove-item')

                    // отображается сообщение "Подписка успешно отозвана" [notification-del3]
                    // нажать на крестик
                    .closeSuccessNotify('notification-del3')

                    // все подписки отозваны
                    .waitForVisible('.service-subscribers_empty', 5000)

                    // внещний вид страницы без подписок [subscribers-empty]
                    .assertView('subscribers-empty', '.service-subscribers')
                );
        });
    });

    describe('Отрицательные', () => {
        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('1. Закрытие модального окна "Оформление подписок" по клику на крестик', function() {
            /* alias: neg-1-close-modal */
            return this.browser

                // авторизоваться под администратором организации с подписками с правом редактировать подписки (2)
                // открыть страницу сервиса "Трекер" {{CONNECT_HOST}}/portal/services/tracker
                .login({ ...user2, retpath: '/portal/services/tracker' })
                .waitForVisible('.service-subscribers')
                .disableAnimations('*')

                // нажать на кнопку "Оформить подписку"
                .waitForVisible('.service-subscribers__header', 20000)
                .click('.service-subscribers__button_role_add-subscription')

                // появилось модальное окно "Оформление подписок"
                .waitForVisible('.modal_visible_yes.subscription-modal', 2000)

                // кликнуть на крестик
                .click('.modal_visible_yes.subscription-modal .modal__close-button_clickable')

                // модальное окно "Оформление подписок" закрылось
                .waitForVisible('.modal_visible_yes.subscription-modal', 2000, true);
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('2. Удаление пользователя в модальном окне "Оформление подписок"', function() {
            /* alias: neg-2-del-users */
            return this.browser

                // авторизоваться под администратором организации c подписоками с правом редактировать подписки (2)
                // открыть страницу сервиса "Трекер" {{CONNECT_HOST}}/portal/services/tracker
                .login({ ...user2, retpath: '/portal/services/tracker' })
                .waitForVisible('.service-subscribers')
                .disableAnimations('*')

                // нажать на кнопку "Оформить подписку"
                .waitForVisible('.service-subscribers__header', 20000)
                .click('.service-subscribers__button_role_add-subscription')

                // появилось модальное окно "Оформление подписок"
                .waitForVisible('.modal_visible_yes.subscription-modal', 2000)

                // выбрать любого пользователя из саджеста
                .selectFromSuggest('.modal_visible_yes .search-input .textinput__control', 'User Simple')
                .waitForVisible('.calculated-service-pricing .spin2', 20000, true)

                // при наведении на пользователя отображается корзина
                .click('.subscription-modal__list-item .avatar-unit')

                // внешний вид модального окна с отображение корзины [plain]
                .waitForVisible('.subscription-modal__list-item:nth-child(1) .avatar-unit__avatar', 2000)
                .assertView('plain', '.modal_visible_yes.subscription-modal .modal__content')

                // нажать на корзину
                .click('.subscription-modal__control_role_remove-item')

                // пользователь удалился из списка
                .waitForVisible('.subscription-modal__list-item', 1000, true);
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('3. Для пользователя без прав - не отображается кнопка "Оформить подписку"', function() {
            /* alias: neg-3-no-subs */
            return this.browser
                // авторизоваться под пользователем организации с подписками без права редактировать подписки (3)
                // открыть страницу сервиса "Трекер" {{CONNECT_HOST}}/portal/services/tracker
                .login({ ...user3, retpath: '/portal/services/tracker' })
                .waitForVisible('.service-subscribers__header', 20000)
                .disableAnimations('*')

                // кнопка "Оформить подписку" не отображается
                .waitForVisible('.service-subscribers__button_role_add-subscription', 10000, true)

                // внешний вид вкладки "Подписки" без кнопки "Оформить подписку" [plain]
                .assertView('plain', '.service-subscribers');
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('4. Для пользователя без прав - не отображается кнопка "Отозвать подписку"', function() {
            /* alias: neg-4-no-revoke */
            return this.browser
                // авторизоваться под пользователем организации с подписками без права редактировать подписки (3)
                // открыть страницу сервиса "Трекер" {{CONNECT_HOST}}/portal/services/tracker
                .login({ ...user3, retpath: '/portal/services/tracker' })
                .waitForVisible('.service-subscribers__header', 20000)
                .disableAnimations('*')

                // при наведении на подписку отображается кнопка "Отозвать подписку"
                .click('.service-subscribers__table .table__body .table__row:first-child .table__cell_role_subscriber')
                // eslint-disable-next-line max-len
                .waitForVisible('.service-subscribers__table .table__body .table__row:first-child .service-subscribers__button_role_remove-item', 1000, true)

                // внешний вид вкладки "Подписки" без кнопки "Оформить подписку" [plain]
                .assertView('plain', '.service-subscribers');
        });

        hermione.skip.in(/.*/, 'Сервис переехал в Облако DIR-10400');
        it('5. Происходит переход на страницу с формой если нет платежной информации', function() {
            /* alias: neg-5-no-info */
            return this.browser
                // авторизоваться под администратора организации
                // без подписок с правом редактировать подписки, не заполнявшего платежную информацию (4)
                // открыть страницу сервиса "Трекер" {{CONNECT_HOST}}/portal/services/tracker
                .login({ ...user4, retpath: '/portal/services/tracker' })
                .waitForVisible('.service-subscribers_empty', 20000)
                .disableAnimations('*')

                // нажать на кнопку "Оформить подписку"
                .click('.service-subscribers_empty .service-subscribers__button_role_add-subscription')

                // выполнился переход на страницу "Создание платежного аккаунта" /portal/admin/subscription/agreement
                .waitForVisible('.service-subscribers_empty', 20000, true)
                .getUrl()
                .then(url => {
                    const parsedUrl = new URL(url);
                    const retpath = parsedUrl.searchParams.get('retpath');
                    const source = parsedUrl.searchParams.get('source');

                    assert.equal(parsedUrl.pathname, '/portal/balance/contract');
                    assert.equal(source, 'tracker');
                    assert.equal(retpath, '/portal/services/tracker#add-subscription');
                });
        });
    });
});
