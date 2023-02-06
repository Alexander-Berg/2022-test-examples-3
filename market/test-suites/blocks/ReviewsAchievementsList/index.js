import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ReviewsAchievementsList.
 * @param {PageObject.ReviewsAchievementsList} reviewsAchievementsList
 * @param {PageObject.BreadcrumbsUnified} breadcrumbsUnified
 */
export default makeSuite('Полный список ачивок пользователя.', {
    params: {
        type: 'Тип контекста: свои или чужие ачивки',
        publicId: 'public_id пользователя',
    },
    story: {
        'Ссылка "Назад"': {
            'должна вести на страницу отзывов': makeCase({
                id: 'm-touch-2046',
                issue: 'MOBMARKET-7943',
                test() {
                    return this.browser.allure.runStep('Проверяем адрес ссылки "Назад"', () => {
                        const backLinkHref = this.breadcrumbsUnified.getCrumbLinkHref(1);

                        const expectedBackLink = this.params.type === 'my' ?
                            this.browser.yaBuildURL('market:my-reviews') :
                            this.browser.yaBuildURL('touch:user-reviews', {publicId: this.params.publicId});

                        return Promise.all([backLinkHref, expectedBackLink])
                            .then(([actualHref, expectedUrl]) => this.expect(actualHref, 'ссылка корректная')
                                .to.be.link({pathname: expectedUrl}, {
                                    skipProtocol: true,
                                    skipHostname: true,
                                })
                            );
                    });
                },
            }),
        },
    },
});
