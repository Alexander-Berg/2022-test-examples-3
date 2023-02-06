jest.disableAutomock();

import {ALL_TYPE, BUS_TYPE, TRAIN_TYPE} from '../../transportType';
import {TLD_RU, TLD_UA} from '../../tlds';
import shouldRequestYBus from '../shouldRequestYBus';

const todayContext = {
    transportType: ALL_TYPE,
    when: {
        special: 'today',
    },
};
const allDaysContext = {
    transportType: ALL_TYPE,
    when: {
        special: 'all-days',
    },
};

describe('shouldRequestYBus', () => {
    it('Не запрашиваем автобусные цены для поиска на все дни', () => {
        expect(shouldRequestYBus(allDaysContext, TLD_RU)).toBe(false);
    });

    it('Не запрашиваем автобусные цены для UA версии', () => {
        expect(shouldRequestYBus(todayContext, TLD_UA)).toBe(false);
    });

    it('Не запрашиваем автобусные цены если контекст поиска (тип транспорта) не соответствует', () => {
        expect(
            shouldRequestYBus(
                {
                    ...todayContext,
                    transportType: TRAIN_TYPE,
                },
                TLD_RU,
            ),
        ).toBe(false);
    });

    [ALL_TYPE, BUS_TYPE].forEach(transportType =>
        it(`Запрашиваем цены для поисков на дату для поисков с типом транспорта ${transportType}`, () => {
            expect(
                shouldRequestYBus(
                    {
                        ...todayContext,
                        transportType,
                    },
                    TLD_RU,
                ),
            ).toBe(true);
        }),
    );
});
