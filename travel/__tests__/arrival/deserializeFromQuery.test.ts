import {ETimeOfDay} from 'utilities/dateUtils/types';

import arrival from '../../arrival';

const defaultValue = arrival.getDefaultValue();

describe('arrival.deserializeFromQuery', () => {
    it('На входе пустые данные - вернём пустой массив', () => {
        expect(arrival.deserializeFromQuery({arrival: []})).toEqual(
            defaultValue,
        );
    });

    it('Для нейзвестного объекта - вернём дефолтное значение', () => {
        // @ts-ignore
        expect(arrival.deserializeFromQuery({unknown: []})).toEqual(
            defaultValue,
        );
    });

    it('Для нейзвестного значения - вернём дефолтное значение ', () => {
        expect(arrival.deserializeFromQuery({arrival: ['fake']})).toEqual(
            defaultValue,
        );
    });

    it('Вернём еденичное значение', () => {
        expect(arrival.deserializeFromQuery({arrival: 'day'})).toEqual([
            ETimeOfDay.DAY,
        ]);
    });

    it('Вернём массив значений', () => {
        expect(
            arrival.deserializeFromQuery({
                arrival: [ETimeOfDay.DAY, ETimeOfDay.NIGHT],
            }),
        ).toEqual([ETimeOfDay.DAY, ETimeOfDay.NIGHT]);
    });
});
