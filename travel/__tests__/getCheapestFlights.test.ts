import {DEFAULT_CURRENCIES} from 'reducers/common/currencies/reducer';

import {getCheapestFlights} from 'selectors/avia/utils/getCheapestFlights';
import {
    IAviaVariantGroup,
    EAviaVariantGroupType,
} from 'selectors/avia/utils/denormalization/variantGroup';
import {
    BASE_GROUP_VARIANT,
    MIN_PRICE_GROUP_VARIANT,
} from 'selectors/avia/utils/__mocks__/mocks';

import {PriceComparator} from 'utilities/currency/compare';
import {PriceConverter} from 'utilities/currency/priceConverter';

const CHEAPEST_VARIANT = {
    ...MIN_PRICE_GROUP_VARIANT,
    type: EAviaVariantGroupType.cheapest,
} as IAviaVariantGroup;

const priceComparator = new PriceComparator(
    new PriceConverter({currenciesInfo: DEFAULT_CURRENCIES}),
);

describe('getCheapestFlights', () => {
    it('нет данных - вернёт пустой массив', () => {
        expect(getCheapestFlights([], priceComparator)).toEqual([]);
    });

    it('есть один вариант с минимальной ценой - вернёт только один "самый дешевый" вариант', () => {
        expect(
            getCheapestFlights(
                [
                    BASE_GROUP_VARIANT,
                    MIN_PRICE_GROUP_VARIANT,
                    BASE_GROUP_VARIANT,
                ],
                priceComparator,
            ),
        ).toEqual([CHEAPEST_VARIANT]);
    });

    it('есть несколько вариантов с минимальной ценой - вернёт несколько "самых дешёвых" вариантов', () => {
        expect(
            getCheapestFlights(
                [
                    BASE_GROUP_VARIANT,
                    MIN_PRICE_GROUP_VARIANT,
                    MIN_PRICE_GROUP_VARIANT,
                ],
                priceComparator,
            ),
        ).toEqual([CHEAPEST_VARIANT, CHEAPEST_VARIANT]);
    });

    it('у всех вариантов одна цена - вернёт все варианты', () => {
        expect(
            getCheapestFlights(
                [MIN_PRICE_GROUP_VARIANT, MIN_PRICE_GROUP_VARIANT],
                priceComparator,
            ),
        ).toEqual([CHEAPEST_VARIANT, CHEAPEST_VARIANT]);
    });
});
