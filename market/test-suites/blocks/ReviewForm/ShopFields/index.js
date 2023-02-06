import {makeCase, makeSuite} from 'ginny';


/**
 * Тест на формы для отзыва на магазин
 * @property {PageObject.ShopReviewNew} shopReviewNew
 */
export default makeSuite('Форма отзыва на магазин', {
    story: {
        'По умолчанию': {
            'Содержит правильный текст в шапке формы': makeCase({
                id: 'm-touch-2982',
                async test() {
                    const headerText = await this.shopReviewNew.getSubpageHeaderText();
                    await this.expect(headerText)
                        .to.include('Отзыв о магазине', 'Хедер содержит текст "Оценка магазина"');
                    const stepsCount = await this.shopReviewNew.getStepsCount();
                    await this.expect(stepsCount)
                        .to.be.greaterThan(0, 'Количество шагов корректно');
                },
            }),
        },
        'При превышении максимального количества символов в поле Достоинства': {
            'кнопка "Отправить" отключается': makeCase({
                id: 'm-touch-2982',
                async test() {
                    const text = 'zxc';
                    await this.shopReviewNew.waitForFormLoaded();
                    await this.shopReviewNew.setProTextField(text);
                    await this.expect(await this.shopReviewNew.isSubmitButtonDisabled())
                        .to.be.equal(true, 'Кнопка "Отправить" заблокирована');
                    await this.shopReviewNew.clearProTextField();
                    await this.expect(await this.shopReviewNew.isSubmitButtonDisabled())
                        .to.be.equal(true, 'Кнопка "Отправить" разблокирована');
                },
            }),
        },
        'При превышении максимального количества символов в поле Недостатки': {
            'кнопка "Отправить" отключается': makeCase({
                id: 'm-touch-2982',
                async test() {
                    const text = 'zxc';
                    await this.shopReviewNew.setContraTextField(text);
                    await this.expect(await this.shopReviewNew.isSubmitButtonDisabled())
                        .to.be.equal(true, 'Кнопка "Отправить" заблокирована');
                    await this.shopReviewNew.clearContraTextField();
                    await this.expect(await this.shopReviewNew.isSubmitButtonDisabled())
                        .to.be.equal(true, 'Кнопка "Отправить" разблокирована');
                },
            }),
        },
        'При превышении максимального количества символов в поле Комментарий': {
            'кнопка "Отправить" отключается': makeCase({
                id: 'm-touch-2982',
                async test() {
                    const text = 'zxc';
                    await this.shopReviewNew.setCommentTextField(text);
                    await this.expect(await this.shopReviewNew.isSubmitButtonDisabled())
                        .to.be.equal(true, 'Кнопка "Отправить" заблокирована');
                    await this.shopReviewNew.clearCommentTextField();
                    await this.expect(await this.shopReviewNew.isSubmitButtonDisabled())
                        .to.be.equal(true, 'Кнопка "Отправить" разблокирована');
                },
            }),
        },
    },
});
