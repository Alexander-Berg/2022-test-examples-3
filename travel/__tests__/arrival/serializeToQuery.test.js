import TimeOfDay from '../../../../interfaces/date/TimeOfDay';

import arrival from '../../arrival';

describe('arrival.serializeToQuery', () => {
    it('Для дефолтного значения вернёт пустой объект', () => {
        expect(arrival.serializeToQuery(arrival.getDefaultValue())).toEqual({});
    });

    it('Вернёт сериализованное значение', () => {
        expect(
            arrival.serializeToQuery([TimeOfDay.morning, TimeOfDay.night]),
        ).toEqual({
            arrival: [TimeOfDay.morning, TimeOfDay.night],
        });
    });
});
