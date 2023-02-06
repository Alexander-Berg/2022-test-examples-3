describe('Главная страница', () => {
    describe('Без авторизации', () => {
        beforeEach(function() {
            return this.browser.yaOpenPage('/ege/');
        });

        describe('Хедер', () => {
            it('Пользователь не залогинен - авторизация', function() {
                const enterLink = 'https://passport.yandex.ru/auth';

                return this.browser
                    .yaCheckLink(PO.UserEnter(), enterLink, { skipQuery: true });
            });

            it('Пользователь не залогинен - регистрация', function() {
                const regLink = 'https://passport.yandex.ru/registration';

                return this.browser
                    .yaCheckLink(PO.UserRegistration(), regLink, { skipQuery: true });
            });

            it('Проверка ссылок в логотипе', function() {
                const yandexLink = 'https://yandex.ru/';
                const tutorLink = '/tutor/';

                return this.browser
                    .yaCheckLink(PO.Header.LogoLinkYandex(), yandexLink)
                    .yaCheckLink(
                        PO.Header.LogoLinkTutor(),
                        tutorLink,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                            skipQuery: true,
                        }
                    );
            });

            it('Проверка переключения экзамена', function() {
                const egeLink = '/tutor/ege/';
                const ogeLink = '/tutor/oge/';

                return this.browser
                    .yaCheckLink(
                        PO.HeadTabs.LinkItemFirst(),
                        egeLink,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        },
                        ['exam_id'],
                    )
                    .yaCheckLink(
                        PO.HeadTabs.LinkItemSecond(),
                        ogeLink,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        },
                        ['exam_id'],
                    );
            });
        });

        describe('Футер', () => {
            it('Пользовательское соглашение', function() {
                const rulesLink = 'https://yandex.ru/legal/tutor_termsofuse/';

                return this.browser
                    .yaCheckLink(PO.Footer.RulesLink(), rulesLink)
                    .yaCheckTargetBlank(PO.Footer.RulesLink());
            });

            it('Помощь', function() {
                const supportLink = 'https://yandex.ru/support/tutor/';

                return this.browser
                    .yaCheckLink(PO.Footer.SupportLink(), supportLink)
                    .yaCheckTargetBlank(PO.Footer.SupportLink());
            });
        });
    });

    describe('С авторизацией', () => {
        beforeEach(function() {
            return this.browser
                .yaLogin()
                .yaOpenPage('/ege/');
        });

        hermione.skip.in([/linux-firefox/], 'протухший tls сертификат');
        it('Пользователь залогинен', function() {
            const userPopup = PO.UserPopup();

            return this.browser
                .yaShouldBeVisible(userPopup, false)
                .click(PO.User())
                .waitForVisible(userPopup)
                .assertView('plain', userPopup);
        });

        hermione.skip.in([/linux-firefox/], 'протухший tls сертификат');
        it('Личный кабинет', function() {
            const statisticLink = '/tutor/user/statistics/';

            return this.browser
                .yaCheckLink(PO.Header.UserStatistics(), statisticLink, {
                    skipProtocol: true,
                    skipHostname: true,
                    skipQuery: true,
                });
        });

        hermione.skip.in([/linux-firefox/], 'протухший tls сертификат');
        it('Проверка внешнего вида шапки', function() {
            return this.browser.assertView('plain', PO.Header());
        });
    });
});
