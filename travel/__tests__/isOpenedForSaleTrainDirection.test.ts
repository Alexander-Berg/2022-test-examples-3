import {RU, KZ} from '../../countries';

import ISearchContext from '../../../interfaces/state/search/ISearchContext';

import isOpenedForSaleTrainDirection from '../isOpenedForSaleTrainDirection';

const FR = 'fr';
const getContext = (from, to): ISearchContext =>
    ({
        from: {
            country: {
                code: from,
            },
        },
        to: {
            country: {
                code: to,
            },
        },
    } as ISearchContext);

describe('isOpenedForSaleTrainDirection', () => {
    describe('Внутренние рейсы', () => {
        it('Для маршрута внутри России - вернём true', () =>
            expect(isOpenedForSaleTrainDirection(getContext(RU, RU))).toBe(
                true,
            ));

        it('Для внутренних маршрутов других разрешённых стран - вернём true', () =>
            expect(isOpenedForSaleTrainDirection(getContext(KZ, KZ))).toBe(
                true,
            ));

        it('Для внутренних маршрутов не разрешённых стран - вернём false', () =>
            expect(isOpenedForSaleTrainDirection(getContext(FR, FR))).toBe(
                false,
            ));
    });

    describe('Международные рейсы', () => {
        it('Если маршрут между разными странами - вернём true', () =>
            expect(isOpenedForSaleTrainDirection(getContext(RU, FR))).toBe(
                true,
            ));
    });
});
