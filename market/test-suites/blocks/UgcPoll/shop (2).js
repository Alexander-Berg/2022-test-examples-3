import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на попап-агитацию оставления отзыва на магазин
 * @property {PageObjects.ReviewPollPopup} reviewPollPopup
 * @property {PageObjects.AgitationPollCard} agitationPollCard
 * @property {PageObjects.RatingStars} shopGradeRatingStars
 * @property {PageObjects.ExpertiseMotivation} expertiseMotivation
 * @property {PageObjects.ShopMainFields} shopMainFields
 */
export default makeSuite('Опрос на магазин.', {
    story: {
        'Экран "Общая оценка".': {
            'По умолчанию': {
                'должно отображаться правильное название магазина.': makeCase({
                    id: 'm-touch-3555',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();

                        return this.expect(await this.agitationPollCard.getTitleText())
                            .to.be.equal('Как вам магазин myShopName?', 'Название магазина должно быть правильным');
                    },
                }),
            },
            'При клике на кнопке закрытия': {
                'опрос должен закрыться и выставиться кука "ugcp".': makeCase({
                    id: 'm-touch-3558',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();
                        await this.reviewPollPopup.clickClose();
                        await this.reviewPollPopup.waitForInvisible();

                        const cookie = await this.browser.getCookie('ugcp');
                        return this.expect(cookie.value)
                            .to.be.equal('1', 'Значение должно быть 1');
                    },
                }),
            },
            'При клике на звезду': {
                'должна выставиться кука "ugcp".': makeCase({
                    id: 'm-touch-3556',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();
                        await this.shopGradeRatingStars.setRating(4);

                        const cookie = await this.browser.getCookie('ugcp');
                        return this.expect(cookie.value)
                            .to.be.equal('1', 'Значение должно быть 1');
                    },
                }),
                'должна открыться полная форма отзыва': makeCase({
                    id: 'm-touch-3557',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();
                        await this.shopGradeRatingStars.setRating(4);

                        return this.expect(this.shopMainFields.isVisible())
                            .to.be.equal(true, 'Видны текстовые поля');
                    },
                }),
            },
            'Бейдж с баллами экспертизы': {
                'должен отображаться.': makeCase({
                    id: 'm-touch-3553',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();
                        await this.expect(this.expertiseMotivation.isVisible())
                            .to.be.equal(true, 'Бейдж должен быть виден');
                    },
                }),
            },
        },
    },
});
