import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';
import {ORDER_TEXTS} from '@self/root/src/constants/order';

const {CANCELLATION_ERROR_TEXTS} = ORDER_TEXTS;

/**
 * Тесты на корректное отображение ошибок валидации при попытке отмены заказа
 */
module.exports = makeSuite('Возникает ошибка валидации при попытке отмены заказа', {
    feature: 'Отмена заказа',
    story: mergeSuites(
        {
            'если не выбрана причина отмены': makeCase({
                id: 'bluemarket-2304',
                issue: 'BLUEMARKET-5383',
                environment: 'kadavr',
                async test() {
                    await this.orderCancelButton.click();
                    await this.orderCancellation.waitForVisible()
                        .should.eventually.to.be.equal(true, 'Попап отмены должен быть виден');

                    await this.cancellationComment.setText('Просто комментарий');
                    await this.cancellationButton.click();

                    await this.cancellationErrorPopup.waitForNotificationVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Попап с текстом ошибки должен быть виден'
                        );

                    return this.cancellationErrorPopup.getText()
                        .should.eventually.to.be.string(
                            CANCELLATION_ERROR_TEXTS.SUBSTATUS_IS_EMPTY,
                            'Должен быть корректный текст в ошибке'
                        );
                },
            }),
            'если введен комментарий длинее 500 символов': makeCase({
                id: 'bluemarket-2304',
                issue: 'BLUEMARKET-5383',
                environment: 'kadavr',
                async test() {
                    await this.orderCancelButton.click();
                    await this.orderCancellation.waitForVisible()
                        .should.eventually.to.be.equal(true, 'Попап отмены должен быть виден');
                    await this.cancelSubstatusRadioControl.clickLabel();
                    await this.cancellationComment.setText(Array(502)
                        .join('A'));
                    await this.cancellationButton.click();

                    await this.cancellationErrorPopup.waitForNotificationVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Попап с текстом ошибки должен быть виден'
                        );

                    return this.cancellationErrorPopup.getText()
                        .should.eventually.to.be.string(
                            CANCELLATION_ERROR_TEXTS.NOTES_IS_TOO_LONG,
                            'Должен быть корректный текст в ошибке'
                        );
                },
            }),
        }
    ),
});
