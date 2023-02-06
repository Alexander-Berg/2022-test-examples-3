'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

hermione.only.notIn('searchapp', 'фича не актуальна в searchapp');
specs({
    feature: 'Подвал',
    type: 'Авторизация'
}, function() {
    it('аутентификация из подвала', function() {
        const login = 'stas.mihailov666';

        return this.browser
            .yaOpenSerp({
                text: 'test login stas',
                yandex_login: login,
                user_connection: 'slow_connection=1'
            })
            .yaWaitForVisible(PO.footer2(), 'Подвал не появился')
            .getText(PO.footer2.enterLink()).then(text =>
                assert.equal(text, login, 'В подвале отсутствует логин пользователя после входа')
            )
            .yaCheckLink(PO.footer2.enterLink(), { target: '' }).then(url => this.browser
                .yaCheckURL(url, 'https://passport.yandex.ru/passport', 'Сломана ссылка на паспорт', {
                    skipQuery: true
                }))
            .yaMockExternalUrl(PO.footer2.enterLink())
            .yaCheckBaobabCounter(PO.footer2.enterLink(), { path: '/$page/$footer/link[@type="username"]' })
            .yaWaitUntilPageReloaded(() => this.browser.refresh())
            .getText(PO.footer2.enterLink()).then(text =>
                assert.equal(text, login, 'В подвале отсутствует логин пользователя после обновления страницы')
            )
            // отличаем дамп после разлогинивания
            .yaWaitUntilPageReloaded(() => this.browser.yaOpenSerp({
                text: 'test logout',
                user_connection: 'slow_connection=1'
            }))
            .getText(PO.footer2.enterLink()).then(text =>
                assert.notEqual(text, login, 'После выхода из аккаунта в подвале остался логин')
            );
    });

    it('проверка кодировки логина пользователя', function() {
        const login = 'robbitter-5151382063@закодированный.домен';

        return this.browser
            .yaOpenSerp({
                text: 'test login robbitter',
                yandex_login: login,
                user_connection: 'slow_connection=1'
            })
            .yaWaitForVisible(PO.footer2(), 'Подвал не появился')
            .getText(PO.footer2.enterLink()).then(text =>
                assert.equal(text, login, 'Отображение кириллического логина пользователя в подвале сломалось')
            );
    });
});
