import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на попап-агитацию оставления отзыва на товар
 * @property {PageObjects.ReviewPollScreen} reviewPollScreen
 * @property {PageObjects.ReviewPollScreenManager} reviewPollScreenManager
 * @property {PageObjects.ReviewPollProductGrade} reviewPollProductGrade
 * @property {PageObjects.RatingStars} productGradeRatingStars
 * @property {PageObjects.ProductReviewForm} reviewForm
 * @property {PageObjects.ExpertiseMotivation} expertiseMotivation
 * @property {PageObjects.Notification} notification
 */
export default makeSuite('Опрос на товар.', {
    story: {
        'Экран "Общая оценка".': {
            'По умолчанию': {
                'должно отображаться правильное название товара.': makeCase({
                    id: 'marketfront-3454',
                    async test() {
                        await this.reviewPollScreen.waitForOpened();

                        return this.expect(await this.reviewPollProductGrade.getProductName())
                            .to.be.equal('Как вам товар myProductName?', 'Название товара должно быть правильным');
                    },
                }),
            },
            'При клике на кнопке закрытия': {
                'опрос должен закрыться и выставиться кука "ugcp".': makeCase({
                    id: 'marketfront-3743',
                    async test() {
                        await this.reviewPollScreen.waitForOpened();
                        await this.reviewPollScreenManager.clickClose();
                        await this.reviewPollScreen.waitForInvisible();

                        const cookie = await this.browser.getCookie('ugcp');
                        return this.expect(cookie.value)
                            .to.be.equal('1', 'Значение должно быть 1');
                    },
                }),
                'должна появиться нотификация со ссылкой на ЛК.': makeCase({
                    id: 'marketfront-4573',
                    async test() {
                        await this.reviewPollScreen.waitForOpened();
                        await this.reviewPollScreenManager.clickClose();
                        await this.reviewPollScreen.waitForInvisible();

                        await this.notification.waitForNotificationVisible();
                        await this.notification.getText()
                            .should.eventually.be.equal('Оценивайте когда удобно в разделеМои публикации', 'Текст нотификации верный');

                        const linkHref = await this.notification.getLinkHref();
                        return this.expect(linkHref, 'Ссылка ведет на "Мои задания" в ЛК')
                            .to.be.link({
                                pathname: '/my/tasks',
                            }, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
            'При клике на звезду': {
                'должна выставиться кука "ugcp".': makeCase({
                    id: 'marketfront-3747',
                    async test() {
                        await this.reviewPollScreen.waitForOpened();
                        await this.productGradeRatingStars.setRating(4);

                        const cookie = await this.browser.getCookie('ugcp');
                        return this.expect(cookie.value)
                            .to.be.equal('1', 'Значение должно быть 1');
                    },
                }),
                'должна открыться полная форма отзыва.': makeCase({
                    id: 'marketfront-4572',
                    issue: 'MARKETFRONT-33503',
                    async test() {
                        await this.reviewPollScreen.waitForOpened();
                        await this.productGradeRatingStars.setRating(4);
                        return this.reviewForm.waitForVisible();
                    },
                }),
            },

            'Бейдж с экспертизой': {
                'должен отображаться.': makeCase({
                    id: 'marketfront-4568',
                    issue: 'MARKETFRONT-33503',
                    async test() {
                        await this.reviewPollScreen.waitForOpened();
                        await this.expect(this.expertiseMotivation.isVisible())
                            .to.be.equal(true, 'Бейдж должен быть виден');
                    },
                }),
            },
        },
    },
});
