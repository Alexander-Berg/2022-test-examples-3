import TimeOfDay from '../../../../interfaces/date/TimeOfDay';

import departure from '../../departure';

describe('departure.serializeToQuery', () => {
    it('Для дефолтного значения вернёт пустой объект', () => {
        expect(departure.serializeToQuery(departure.getDefaultValue())).toEqual(
            {},
        );
    });

    it('Вернёт сериализованное значение', () => {
        expect(
            departure.serializeToQuery([TimeOfDay.morning, TimeOfDay.night]),
        ).toEqual({
            departure: [TimeOfDay.morning, TimeOfDay.night],
        });
    });
});
