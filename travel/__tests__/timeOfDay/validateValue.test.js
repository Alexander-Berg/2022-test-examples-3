import TimeOfDay from '../../../../interfaces/date/TimeOfDay';

import timeOfDay from '../../timeOfDay';

const defaultValue = timeOfDay.getDefaultValue();

describe('timeOfDay.validateValue', () => {
    it('Если все значения валидны - вернёт исходный список', () => {
        expect(timeOfDay.validateValue([TimeOfDay.evening])).toEqual([
            TimeOfDay.evening,
        ]);
    });

    it('Если в списке есть невалидные значения - они будут отфильтрованы', () => {
        expect(
            timeOfDay.validateValue([TimeOfDay.evening, 'LATE EVENING']),
        ).toEqual([TimeOfDay.evening]);
    });

    it('Если в списке нет валидных значений - вернётся пустой список', () => {
        expect(timeOfDay.validateValue(['DAAY'])).toEqual(defaultValue);
    });
});
