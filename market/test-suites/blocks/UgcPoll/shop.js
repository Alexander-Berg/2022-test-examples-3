import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на попап-агитацию оставления отзыва на магазин
 * @property {PageObjects.ReviewPollScreen} reviewPollScreen
 * @property {PageObjects.ReviewPollScreenManager} reviewPollScreenManager
 * @property {PageObjects.ReviewPollShopGrade} reviewPollShopGrade
 * @property {PageObjects.RatingStars} shopGradeRatingStars
 * @property {PageObjects.ExpertiseMotivation} expertiseMotivation
 */
export default makeSuite('Опрос на магазин.', {
    story: {
        'Экран "Общая оценка".': {
            'По умолчанию': {
                'должно отображаться правильное название магазина.': makeCase({
                    id: 'marketfront-3445',
                    async test() {
                        await this.reviewPollScreen.waitForOpened();

                        return this.expect(await this.reviewPollShopGrade.getShopName())
                            .to.be.equal('Как вам магазин myShopName?', 'Название магазина должно быть правильным');
                    },
                }),
            },
            'При клике на кнопке закрытия': {
                'опрос должен закрыться и выставиться кука "ugcp".': makeCase({
                    id: 'marketfront-3745',
                    async test() {
                        await this.reviewPollScreen.waitForOpened();
                        await this.reviewPollScreenManager.clickClose();
                        await this.reviewPollScreen.waitForInvisible();

                        const cookie = await this.browser.getCookie('ugcp');
                        return this.expect(cookie.value)
                            .to.be.equal('1', 'Значение должно быть 1');
                    },
                }),
            },
            'При клике на звезду': {
                'должна выставиться кука "ugcp".': makeCase({
                    id: 'marketfront-3746',
                    async test() {
                        await this.reviewPollScreen.waitForOpened();
                        await this.shopGradeRatingStars.setRating(4);

                        const cookie = await this.browser.getCookie('ugcp');
                        return this.expect(cookie.value)
                            .to.be.equal('1', 'Значение должно быть 1');
                    },
                }),
            },
            'Бейдж с экспертизой': {
                'должен отображаться.': makeCase({
                    issue: 'MARKETFRONT-33503',
                    id: 'marketfront-4563',
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
