import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на попап-агитацию оставления отзыва на товар
 * @property {PageObjects.ReviewPollPopup} reviewPollPopup
 * @property {PageObjects.AgitationPollCard} agitationPollCard
 * @property {PageObjects.ExpertiseMotivation} expertiseMotivation
 * @property {PageObjects.Button} addTextButton
 * @property {PageObjects.ProductMainFields} productMainFields
 */
export default makeSuite('Агитация оставить отзыв на товар.', {
    story: {
        'По умолчанию': {
            'должно отображаться правильное название товара.': makeCase({
                id: 'm-touch-3546',
                async test() {
                    await this.reviewPollPopup.waitForOpened();

                    return this.expect(await this.agitationPollCard.getTitleText())
                        .to.be.equal('Как вам товар myProductName?', 'Название товара должно быть правильным');
                },
            }),
        },
        'При клике на "Написать отзыв"': {
            'должна выставиться кука "ugcp".': makeCase({
                id: 'm-touch-3547',
                async test() {
                    await this.reviewPollPopup.waitForOpened();
                    await this.addTextButton.click();

                    const cookie = await this.browser.getCookie('ugcp');
                    return this.expect(cookie.value)
                        .to.be.equal('1', 'Значение должно быть 1');
                },
            }),
            'должна открыться полная форма отзыва': makeCase({
                id: 'm-touch-3548',
                async test() {
                    await this.reviewPollPopup.waitForOpened();
                    await this.addTextButton.click();

                    return this.expect(this.productMainFields.isTextFieldsVisible())
                        .to.be.equal(true, 'Видны текстовые поля');
                },
            }),
        },
        'Бейдж с баллами экспертизы': {
            'должен отображаться.': makeCase({
                id: 'm-touch-3545',
                async test() {
                    await this.reviewPollPopup.waitForOpened();
                    await this.expect(this.expertiseMotivation.isVisible())
                        .to.be.equal(true, 'Бейдж должен быть виден');
                },
            }),
        },
    },
});
