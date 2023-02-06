import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на попап-агитацию оставления отзыва на товар
 * @property {PageObjects.ReviewPollPopup} reviewPollPopup
 * @property {PageObjects.AgitationPollCard} agitationPollCard
 * @property {PageObjects.RatingStars} productGradeRatingStars
 * @property {PageObjects.ExpertiseMotivation} expertiseMotivation
 * @property {PageObjects.Notification} notification
 * @property {PageObjects.ProductMainFields} productMainFields
 */
export default makeSuite('Опрос на товар.', {
    story: {
        'Экран "Общая оценка".': {
            'По умолчанию': {
                'должно отображаться правильное название товара.': makeCase({
                    id: 'm-touch-3561',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();

                        return this.expect(await this.agitationPollCard.getTitleText())
                            .to.be.equal('Как вам товар myProductName?', 'Название товара должно быть правильным');
                    },
                }),
            },
            'При клике на кнопке закрытия': {
                'опрос должен закрыться и выставиться кука "ugcp".': makeCase({
                    id: 'm-touch-3565',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();
                        await this.reviewPollPopup.clickClose();
                        await this.reviewPollPopup.waitForInvisible();

                        const cookie = await this.browser.getCookie('ugcp');
                        return this.expect(cookie.value)
                            .to.be.equal('1', 'Значение должно быть 1');
                    },
                }),
                'должна появиться нотификация со ссылкой на ЛК.': makeCase({
                    id: 'm-touch-3564',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();
                        await this.reviewPollPopup.clickClose();
                        await this.reviewPollPopup.waitForInvisible();

                        await this.notification.waitForNotificationVisible();
                        await this.notification.getText()
                            .should.eventually.be.equal('Оценивайте когда удобно в разделе\nМои публикации', 'Текст нотификации верный');

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
                    id: 'm-touch-3562',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();
                        await this.productGradeRatingStars.setRating(4);

                        const cookie = await this.browser.getCookie('ugcp');
                        return this.expect(cookie.value)
                            .to.be.equal('1', 'Значение должно быть 1');
                    },
                }),
                'должна открыться полная форма отзыва': makeCase({
                    id: 'm-touch-3563',
                    async test() {
                        await this.reviewPollPopup.waitForOpened();
                        await this.productGradeRatingStars.setRating(4);

                        return this.expect(this.productMainFields.isTextFieldsVisible())
                            .to.be.equal(true, 'Видны текстовые поля');
                    },
                }),
            },
            'Бейдж с баллами экспертизы': {
                'должен отображаться.': makeCase({
                    id: 'm-touch-3559',
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
