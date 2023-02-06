import TimeOfDay from '../../../../interfaces/date/TimeOfDay';

import arrival from '../../arrival';

const defaultValue = arrival.getDefaultValue();

describe('arrival.deserializeFromQuery', () => {
    it('На входе пустые данные - вернём пустой массив', () => {
        expect(arrival.deserializeFromQuery({arrival: []})).toEqual(
            defaultValue,
        );
    });

    it('Для нейзвестного объекта - вернём дефолтное значение', () => {
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
            TimeOfDay.day,
        ]);
    });

    it('Вернём массив значений', () => {
        expect(
            arrival.deserializeFromQuery({
                arrival: [TimeOfDay.day, TimeOfDay.night],
            }),
        ).toEqual([TimeOfDay.day, TimeOfDay.night]);
    });
});
