import {makeCase, makeSuite} from 'ginny';

import getPrice from '@self/project/src/entities/delivery/helpers/getPrice';
import {NBSP} from '@self/project/src/constants/string';

export default makeSuite('Акция "Прогрессивная скидка за количество".', {
    environment: 'kadavr',
    params: {
        promoBound: 'Шаг промки за количество',
    },
    story: {
        'При увеличении количества товара в корзине до целевого числа': {
            'промо за количество товара начинает применяться': makeCase({
                async test() {
                    const {promoBound} = this.params;

                    await this.counterCartButton.waitForVisible();
                    await this.counterCartButton.increase.click();
                    await this.counterCartButton.waitUntilCounterChanged(1, promoBound.count);

                    if (this.price.discountBadge) {
                        await this.price.getDiscountBadgeText()
                            .should.eventually
                            .be.equal(`\u2012${promoBound.promoPriceWithTotalDiscount.discount.percent}%`);
                    }

                    const {currency, discount: {oldMin}, value} = promoBound.promoPriceWithTotalDiscount;

                    const expectedDiscountPrice = getPrice(oldMin, currency).replace(NBSP, ' ');
                    await this.price.getDiscountPriceText()
                        .should.eventually.be.equal(expectedDiscountPrice);

                    const expectedCurrentPrice = getPrice(value, currency).replace(NBSP, ' ');
                    await this.price.getPriceText()
                        .should.eventually.be.equal(expectedCurrentPrice);
                },
            }),
        },
    },
});
