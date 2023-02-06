import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на хлебные крошки
 * @property {PageObject.UserAchievementsPage} userAchievementsPage
 * @property {PageObject.UserAchievementsModal} userAchievementsModal
 */
export default makeSuite('Блок хлебных крошек', {
    environment: 'testing',
    story: {
        'хлебная крошка': {
            'по умолчанию': {
                'ведет на страницу всех отзывов.': makeCase({
                    id: 'marketfront-2497',
                    issue: 'MARKETVERSTKA-28949',
                    test() {
                        return Promise
                            .all([
                                this.userAchievementsPage.breadcrumbsHref,
                                this.browser.yaBuildURL('market:my-reviews'),
                            ])
                            .then(([urlForRelocate, expectedUrl]) => this.expect(urlForRelocate, 'ссылка корректная')
                                .to.be.link({
                                    pathname: expectedUrl,
                                }, {
                                    skipProtocol: true,
                                    skipHostname: true,
                                }));
                    },
                }),
            },
        },
    },
});
