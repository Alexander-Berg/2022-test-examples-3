const { testUsers: { adminUITest2: user } } = hermione.ctx;
const { removeSettingByKey } = require('../../helpers/directoryApi');

describe('Блок для опросов', () => {
    afterEach(function() {
        const { cacheMode } = hermione.ctx;

        // В режиме для чтения ничего удалять не надо
        if (cacheMode === 'read') {
            return;
        }

        this.browser.getMeta('survey').then(id => {
            if (!id) {
                return;
            }

            return removeSettingByKey(`custom_survey_status_${id}`, user);
        });
    });

    describe('Положительные', () => {
        it('1. Блок для опросов отображается на странице админки', function() {
            /* alias: pos-1-view */
            return this.browser
                // авторизоваться под пользователем с включенным блоком для опросов
                // и перейти по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...user, retpath: '/portal/admin', bunker: true })

                // отображается блок для опросов
                .waitForVisible('.pinned-notification', 5000)

                // внешний вид блока для опросов  [plain]
                .assertView('plain', '.pinned-notification');
        });

        it('2. Нажатие на кнопку для прохождения опроса', function() {
            /* alias: pos-2-redirect */
            return this.browser
                // авторизоваться под пользователем с включенным блоком для опросов
                // и перейти по ссылке "{{CONNECT_HOST}}/portal/home"
                .login({ ...user, retpath: '/portal/home', bunker: true })

                // отображается блок для опросов
                .waitForVisible('.bunker-notification', 5000)

                // внешний вид блока для опросов  [plain]
                .assertView('plain', '.bunker-notification')

                .setHash('new-setting')

                .execute(() => {
                    let survey = {};

                    try {
                        survey = window.yc.BunkerStore.get('survey');
                    } catch (e) {
                        // noop
                    }

                    return survey;
                })
                .then(({ value }) => {
                    if (!value) {
                        return;
                    }

                    this.browser.setMeta('survey', value.id);
                })

                // нажать на желтую кнопку в блоке
                .click('.bunker-notification__button_role_accept')

                // блок для опросов не отображается
                .waitForVisible('.bunker-notification', 15000, true)

                // перейти по ссылке "{{CONNECT_HOST}}/portal/admin"
                .setHash('new-setting2')
                .url('/portal/admin')

                // блок для опросов не отображается
                .waitForVisible('.pinned-notification', 1000, true);
        });
    });

    describe('Отрицательные', () => {
        it('1. Закрытие блока для опросов', function() {
            /* alias: neg-1-cancel */
            return this.browser
                // авторизоваться под пользователем с включенным блоком для опросов
                // и перейти по ссылке "{{CONNECT_HOST}}/portal/home"
                .login({ ...user, retpath: '/portal/home', bunker: true })

                // отображается блок для опросов
                .waitForVisible('.bunker-notification', 5000)

                // внешний вид блока для опросов [plain]
                .assertView('plain', '.bunker-notification')

                .setHash('new-setting')

                .execute(() => {
                    let survey = {};

                    try {
                        survey = window.yc.BunkerStore.get('survey');
                    } catch (e) {
                        // noop
                    }

                    return survey;
                })
                .then(({ value }) => {
                    if (!value) {
                        return;
                    }

                    this.browser.setMeta('survey', value.id);
                })

                // нажать на крестик в блоке
                .click('.bunker-notification__close')

                // блок для опросов не отображается
                .waitForVisible('.bunker-notification', 1000, true)

                // перейти по ссылке "{{CONNECT_HOST}}/portal/admin"
                .setHash('new-setting2')
                .url('/portal/admin')

                // блок для опросов не отображается
                .waitForVisible('.pinned-notification', 1000, true);
        });
    });
});
