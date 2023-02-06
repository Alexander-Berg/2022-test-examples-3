// @flow
// flowlint-next-line untyped-import: off
import {waitFor} from '@testing-library/dom';

import {getCurrency, strFormatPrice} from '@self/root/src/utils/price';
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';
import type {RawDiscountCountBound} from '@self/root/src/resources/report/normalization/promo/spreadDiscountCount';

export const createSpreadDiscountCountPriceSuite = ({
    getContainer,
    selectors,
    promoBound,
}: {
    // flowlint-next-line unclear-type:off
    getContainer: () => any,
    selectors: {
        cartButton: string,
        cartCount: string,
        discountBadge?: string,
        increaseCountButton: string,
        price: string,
    },
    promoBound: RawDiscountCountBound,
}) => {
    test('При увеличении количества товара промо за количество товара начинает применяться', async () => {
        const container = getContainer();

        container.querySelector(selectors.cartButton).click();

        await waitFor(() => {
            expect(container.querySelector(selectors.increaseCountButton)).toBeTruthy();
        });
        container.querySelector(selectors.increaseCountButton).click();

        await waitFor(() => {
            expect(container.querySelector(selectors.cartCount).textContent).toBe(
                `${promoBound.count} товара в корзине`
            );
        });

        const {currency, value} = promoBound.promoPriceWithTotalDiscount;
        const expectedCurrentPrice = `${strFormatPrice(value)}${NBSP}${getCurrency(currency)}`;

        await waitFor(() => {
            expect(container.querySelector(selectors.price).textContent)
                .toBe(expectedCurrentPrice);

            if (selectors.discountBadge) {
                expect(container.querySelector(selectors.discountBadge).textContent)
                    // $FlowIgnore
                    .toBe(`\u2012${promoBound.promoPriceWithTotalDiscount.discount.percent}%`);
            }
        });
    });
};
