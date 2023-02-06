import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Отображение комплекта', {
    feature: 'Акция товар + подарок',
    id: 'bluemarket-3123',
    story: {
        'По умолчанию': {
            'между сниппетом основного товара и подарком нет разделения': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Проверяем, что нет разделителя под основным товаром',
                        () => this.primaryCartItem.getCssValue('border-bottom-width')
                            .should.eventually.be.equal('0px', 'Не должно быть разделителя под основным товаром')
                    );

                    await this.browser.allure.runStep(
                        'Проверяем, что нет разделителя над подарком',
                        () => this.giftCartItem.getCssValue('border-top-width')
                            .should.eventually.be.equal('0px', 'Не должно быть разделителя над подарком')
                    );
                },
            }),
            'у подарка нет кнопки для изменения количества': makeCase({
                async test() {
                    await this.primaryCartOffer.isVisible()
                        .should.eventually.be.equal(true, 'Должен показаться основной товар');
                    await this.giftCartOffer.isVisible()
                        .should.eventually.be.equal(true, 'Должен показаться подарок');

                    await this.primaryAmountSelect.isVisible()
                        .should.eventually.be.equal(true, 'У основного товара должны быть кнопки изменения количества');
                    await this.giftAmountSelect.isVisible()
                        .should.eventually.be.equal(false, 'У подарка не должно быть кнопок изменения количества');
                },
            }),
        },
    },
});
