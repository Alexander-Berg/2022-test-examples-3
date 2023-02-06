describe('Главная страница', () => {
    describe('Без авторизации', () => {
        beforeEach(function() {
            return this.browser.yaOpenPage('/ege/');
        });

        describe('Хедер', () => {
            it('Пользователь не залогинен - авторизация', function() {
                const enterLink = 'https://passport.yandex.ru/auth';

                return this.browser.yaCheckLink(
                    PO.UserEnter(),
                    enterLink,
                    { skipQuery: true }
                );
            });

            it('Проверка ссылок в логотипе', function() {
                const yandexLink = 'https://yandex.ru/';
                const tutorLink = '/tutor/';

                return this.browser
                    .yaCheckLink(PO.HeaderTouch.LogoLinkYandex(), yandexLink)
                    .yaCheckLink(
                        PO.HeaderTouch.LogoLinkTutor(),
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
                        PO.HeadTabsTouch.LinkItemFirst(),
                        egeLink,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        },
                        ['exam_id']
                    )
                    .yaCheckLink(
                        PO.HeadTabsTouch.LinkItemSecond(),
                        ogeLink,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        },
                        ['exam_id']
                    );
            });
        });

        describe('Футер', () => {
            it('Личный кабинет', function() {
                const statisticLink = '/tutor/user/statistics/';

                return this.browser
                    .yaCheckLink(PO.Footer.StatisticsButton(), statisticLink, {
                        skipProtocol: true,
                        skipHostname: true,
                        skipQuery: true,
                    });
            });
        });
    });

    describe('С авторизацией', () => {
        beforeEach(function() {
            return this.browser
                .yaLogin()
                .yaOpenPage('/ege/');
        });

        it('Хедер', function() {
            const userPopup = PO.UserPopup();

            return this.browser.yaShouldBeVisible(userPopup, false)
                .click(PO.User())
                .waitForVisible(userPopup)
                .pause(1000)
                .assertView('plain', userPopup, { allowViewportOverflow: true });
        });
    });
});
