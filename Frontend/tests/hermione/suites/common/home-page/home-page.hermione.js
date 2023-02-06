describe('Главная страница', () => {
    describe('Без авторизации', () => {
        beforeEach(function() {
            return this.browser.yaOpenPage('/ege/');
        });

        it('Проверка внешнего вида', function() {
            return this.browser.assertView('plain', 'body');
        });

        it('Проверка ссылок на страницу предмета', function() {
            const russianLinkEge = '/tutor/subject/?subject_id=3';
            const russianLinkOge = '/tutor/subject/?subject_id=17';

            return this.browser
                .yaCheckLink(
                    PO.SubjectItemThird(),
                    russianLinkEge,
                    {
                        skipProtocol: true,
                        skipHostname: true,
                    },
                    ['subject_id']
                )
                .yaOpenPage('/oge/') // переходим на вкладку ОГЭ
                .yaCheckLink(
                    PO.SubjectItemSecond(),
                    russianLinkOge,
                    {
                        skipProtocol: true,
                        skipHostname: true,
                    },
                    ['subject_id']
                );
        });

        describe('Футер', () => {
            it('Проверка ссылок в футере', function() {
                const aboutLink = 'https://yandex.ru/promo/tutor/about';
                const feedbackLink = 'https://yandex.ru/support/tutor/troubleshooting/question.html';

                return this.browser
                    .yaCheckTargetBlank(PO.Footer.AboutLink())
                    .yaCheckLink(PO.Footer.AboutLink(), aboutLink)
                    .yaCheckTargetBlank(PO.Footer.FeedbackLink())
                    .yaCheckLink(PO.Footer.FeedbackLink(), feedbackLink);
            });

            it('Проверка года в копирайте', function() {
                const expectedYear = '2018–' + new Date().getFullYear();

                return this.browser
                    .getText(PO.FooterCopyright())
                    .then(year => assert.equal(year, expectedYear));
            });
        });
    });

    describe('С авторизацией', () => {
        it('Карточка достижений', function() {
            const achievementsLink = '/tutor/user/achievements/';

            return this.browser
                .yaLogin()
                .yaOpenPage('/ege/?passport_uid=hermione_without_achievements')
                .yaWaitForVisible(PO.AchievementCard(), 15000)
                .assertView('plain', PO.AchievementCard())
                .yaCheckLink(
                    PO.AchievementCard.Link(),
                    achievementsLink,
                    {
                        skipProtocol: true,
                        skipHostname: true,
                        skipQuery: true,
                    },
                );
        });
    });

    it('Переход по ссылке с расшаренной ачивкой', function() {
        return this.browser
            .yaOpenPage('/?test-id=216021&share_achievement_id=0b5a83280b1d3a5b0c7ebe748314486b')
            .yaWaitForVisible(PO.ShareLandingPopup())
            .assertView('sharing-modal', PO.ShareLandingPopup())
            .yaWaitForVisible(PO.ShareLandingPopup.Button())
            .click(PO.ShareLandingPopup.Button())
            .yaWaitForHidden(PO.ShareLandingPopup());
    });
});
