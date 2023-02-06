import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';
import CommonSuite from './common';

/**
 * @param {PageObject.components.ReviewPollCard} reviewPollCard
 * @param {PageObject.components.RatingStars} ratingStars
 */
export default makeSuite('Aгитация на товар.', {
    params: {
        ratingHeading: 'Ожидаемое значение заголовка агитации',
        reviewUrl: 'Страница оставления отзыва',
        entityUrl: 'Страница карточки объекта',
    },
    story: mergeSuites(
        prepareSuite(CommonSuite),
        {
            'Карточка агитации оставления оценки. Клик по': {
                'названию товара ведет в карточку товара': makeCase({
                    id: 'marketfront-3971',
                    async test() {
                        await this.reviewPollCard.entityName.click();

                        return this.browser.getUrl()
                            .should.eventually.be.link({
                                pathname: this.params.entityUrl,
                            }, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
                'изображению товара ведет в карточку товара': makeCase({
                    id: 'marketfront-3972',
                    async test() {
                        await this.reviewPollCard.entityImage.click();

                        return this.browser.getUrl()
                            .should.eventually.be.link({
                                pathname: this.params.entityUrl,
                            }, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        }
    ),
});
