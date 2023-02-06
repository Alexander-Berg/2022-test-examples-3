import {makeSuite, makeCase} from 'ginny';
import {getFormatedPrice} from '@self/root/src/entities/price/getters';
import {getCurrency} from '@self/root/src/utils/price';

/**
 * Тесты на блок DefaultOffer фармы
 */
export default makeSuite('Данные ДО', {
    story: {
        'Отображается цена "от"': makeCase({
            async test() {
                const {offer} = this.params;
                const resPrice = `от ${getFormatedPrice(offer.prices)} ${getCurrency(offer.prices.currency)}`;
                await this.defaultOffer.waitForVisible();
                const fullPrice = await this.price.getPriceText();
                return this.expect(fullPrice)
                    .to.be.equal(resPrice, 'Цена "от" отображается на карточке');
            },
        }),
        'Отсутствует информация о поставщике': makeCase({
            async test() {
                await this.defaultOffer.waitForVisible();
                return this.shopInfo.isVisible()
                    .should.eventually.be.equal(
                        false,
                        'Отсутствует информация о поставщике'
                    );
            },
        }),
    },
});
