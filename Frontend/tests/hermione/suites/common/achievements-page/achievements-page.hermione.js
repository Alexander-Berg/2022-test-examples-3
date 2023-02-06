describe('Страница достижений', () => {
    const getAchievementsUrl = passportUid =>
        `/user/achievements/${passportUid ? '?passport_uid=' + passportUid : ''}`;

    describe('Внешний вид блока достижений', () => {
        it('Без авторизации', function() {
            const passportLink = 'https://passport.yandex.ru/auth';

            return this.browser
                .yaOpenPage(getAchievementsUrl())
                .yaCheckLink(
                    PO.NeedLogin.Link(),
                    passportLink,
                    { skipQuery: true },
                )
                .assertView('plain', PO.NeedLogin());
        });

        describe('С авторизацией', () => {
            const AchievementList = PO.AchievementList();

            it('Без достижений', function() {
                return this.browser
                    .yaLogin()
                    .yaOpenPage(getAchievementsUrl('hermione_without_achievements'))
                    .assertView('plain', AchievementList)
                    .click(PO.AchievementList.ItemLast())
                    .yaShouldBeVisible(PO.AchievementPopupLast());
            });

            it('С одним открытым достиженением', function() {
                return this.browser
                    .yaLogin()
                    .yaOpenPage(getAchievementsUrl('hermione_with_achievement'))
                    .assertView('plain', AchievementList);
            });

            hermione.skip.in(['linux-chrome-iphone', 'linux-chrome-ipad'], 'https://st.yandex-team.ru/YOUNGLINGS-2080');
            it('Все достижения открыты', function() {
                return this.browser
                    .yaLogin()
                    .yaOpenPage(getAchievementsUrl('hermione_all_achievements'))
                    .assertView('plain', AchievementList);
            });

            it('Шэринг', function() {
                return this.browser
                    .yaLogin()
                    .yaOpenPage(getAchievementsUrl('hermione_with_achievement'))
                    .assertView('plain', AchievementList)
                    .yaShouldBeVisible(PO.AchievementShareButton())
                    .click(PO.AchievementShareButton())
                    .yaShouldBeVisible(PO.AchievementSharePopup())
                    .assertView('sharing', AchievementList);
            });
        });
    });

    it('Внешний вид навигации', function() {
        const statisticUrl = '/tutor/user/statistics/';

        return this.browser
            .yaOpenPage(getAchievementsUrl())
            .assertView('plain', PO.CabinetRightCard())
            .yaCheckLink(
                PO.CabinetRightCard.StatisticLink(),
                statisticUrl,
                {
                    skipProtocol: true,
                    skipHostname: true,
                    skipQuery: true,
                },
            );
    });

    it('Балломер', function() {
        return this.browser
            .yaOpenPage(getAchievementsUrl())
            .yaShouldBeVisible(PO.Scoremeter());
    });
});
