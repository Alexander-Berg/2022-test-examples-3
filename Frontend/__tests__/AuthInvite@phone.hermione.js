specs({
    feature: 'Auth Invite',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=authinvite/registration.json')
            .yaWaitForVisible(PO.blocks.authInvite())
            .assertView('registration', PO.blocks.authInvite())
            .url('/turbo?stub=authinvite/login.json')
            .yaWaitForVisible(PO.blocks.authInvite())
            .assertView('login', PO.blocks.authInvite());
    });

    hermione.only.notIn('safari13');
    it('Проверка ссылки регистрации', function() {
        return this.browser
            .url('/turbo?stub=authinvite/registration.json')
            .yaWaitForVisible(PO.blocks.authInvite())
            .yaCheckLink({
                selector: PO.blocks.authInviteAction(),
                message: `Неправильная ссылка (${PO.blocks.authInviteAction()})`,
                target: '_blank',
                url: {
                    href: 'https://passport.yandex.ru/registration',
                },
            });
    });

    hermione.only.notIn('safari13');
    it('Проверка ссылки авторизации', function() {
        return this.browser
            .url('/turbo?stub=authinvite/login.json')
            .yaWaitForVisible(PO.blocks.authInvite())
            .yaCheckLink({
                selector: PO.blocks.authInviteAction(),
                message: `Неправильная ссылка (${PO.blocks.authInviteAction()})`,
                target: '_blank',
                url: {
                    href: 'https://passport.yandex.ru/auth',
                },
            });
    });
});
