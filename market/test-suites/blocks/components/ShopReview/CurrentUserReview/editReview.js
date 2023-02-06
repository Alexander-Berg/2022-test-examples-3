import {makeSuite, makeCase} from 'ginny';
import {routes} from '@self/platform/spec/hermione/configs/routes';

/**
 * @param {PageObject.CurrentUserReview} currentUserReview
 * @class PageObject.Header
 * @class PageObject.Controls
 */
export default makeSuite('Блок «Ваш отзыв». Изменение', {
    feature: 'Рейтинг магазина',
    story: {
        'Кнопка «Изменить».': {
            'При клике происходит переход на страницу редактирования отзыва': makeCase({
                id: 'm-touch-2365',
                issue: 'MOBMARKET-9582',
                async test() {
                    await this.header.openReviewMenu();
                    await this.controls.clickEditButton();

                    const url = await this.browser.getUrl();
                    await this.expect(url).to.be.link({
                        pathname: `/shop--${routes.shop.slug}/${routes.shop.shopId}/reviews/add`,
                    }, {
                        skipProtocol: true,
                        skipHostname: true,
                        skipQuery: true,
                    });
                },
            }),
        },
    },
});
