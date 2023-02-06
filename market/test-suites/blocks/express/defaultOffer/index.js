import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок DefaultOffer
 * @param {PageObject.DefaultOffer} defaultOffer
 */
export default makeSuite('Данные ДО', {
    story: {
        'Отображаются ожидаемым образом': makeCase({
            async test() {
                const {
                    expectedWithPrice,
                    expectedExpressBadgeHeading,
                    expectedExpressBadgeHeadingColor,
                    expectedExpressBadgeMessageColor,
                    expectedPaymentTypes,
                    expectedButtonCaption,
                    expectedButtonColor,
                    expectedShopName,
                    expectedWithComplainButtonOnHover,
                } = this.params;

                await this.defaultOffer.price.isExisting().should.eventually.be.equal(
                    expectedWithPrice, 'наличие цены соответствует ожидаемому'
                );

                await this.defaultOffer.getExpressBadgeHeading().should.eventually.be.equal(
                    expectedExpressBadgeHeading, 'заголовок бейджа соответствует ожидаемому'
                );

                // FIXME: начать мокать время, чтоб тест не мигал в разное время суток
                // await this.defaultOffer.getExpressBadgeMessage().should.eventually.be.equal(
                //     expectedExpressBadgeText, 'текст бейджа соответствует ожидаемому'
                // );

                await this.defaultOffer.getExpressBadgeHeadingColor().should.eventually.be.equal(
                    expectedExpressBadgeHeadingColor, 'цвет заголовка бэйджа соответствует ожидаемому'
                );

                await this.defaultOffer.getExpressBadgeMessageColor().should.eventually.be.equal(
                    expectedExpressBadgeMessageColor, 'цвет текста бэйджа соответствует ожидаемому'
                );

                await this.defaultOffer.paymentTypes.getText().should.eventually.be.equal(
                    expectedPaymentTypes, 'тип оплаты соответствует ожидаемому'
                );

                await this.defaultOffer.cartButton.getText().should.eventually.be.equal(
                    expectedButtonCaption, 'надпись на кнопке соответствует ожидаемому'
                );
                await this.defaultOffer.getCartButtonBackground().should.eventually.to.be.equal(
                    expectedButtonColor, 'цвет кнопки соответствует ожидаемому'
                );

                await this.defaultOffer.getShopName().should.eventually.be.equal(
                    expectedShopName, 'название магазина соответствует ожидаемому'
                );

                if (expectedWithComplainButtonOnHover) {
                    await this.defaultOffer.getComplainButtonOpacity().should.eventually.to.be.equal(
                        0, 'видимость кнопки жалобы соответствует ожидаемому'
                    );
                    await this.defaultOffer.hoverOverDefaultOffer();
                    await this.defaultOffer.getComplainButtonOpacity().should.eventually.to.be.equal(
                        Number(expectedWithComplainButtonOnHover), 'видимость кнопки жалобы соответствует ожидаемому'
                    );
                }
            },
        }),

    },
});
