import TimeOfDay from '../../../../interfaces/date/TimeOfDay';

import departure from '../../departure';

const defaultValue = departure.getDefaultValue();

describe('departure.deserializeFromQuery', () => {
    it('На входе пустые данные - вернём пустой массив', () => {
        expect(departure.deserializeFromQuery({departure: []})).toEqual(
            defaultValue,
        );
    });

    it('Для нейзвестного объекта - вернём дефолтное значение', () => {
        expect(departure.deserializeFromQuery({unknown: []})).toEqual(
            defaultValue,
        );
    });

    it('Для нейзвестного значения - вернём дефолтное значение ', () => {
        expect(departure.deserializeFromQuery({departure: ['fake']})).toEqual(
            defaultValue,
        );
    });

    it('Вернём еденичное значение', () => {
        expect(departure.deserializeFromQuery({departure: 'day'})).toEqual([
            TimeOfDay.day,
        ]);
    });

    it('Вернём массив значений', () => {
        expect(
            departure.deserializeFromQuery({
                departure: [TimeOfDay.day, TimeOfDay.night],
            }),
        ).toEqual([TimeOfDay.day, TimeOfDay.night]);
    });
});
