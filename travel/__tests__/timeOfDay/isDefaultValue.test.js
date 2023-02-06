import TimeOfDay from '../../../../interfaces/date/TimeOfDay';

import timeOfDay from '../../timeOfDay';

describe('timeOfDay.isDefaultValue', () => {
    it('Вернёт false для непустого массива', () => {
        expect(timeOfDay.isDefaultValue([TimeOfDay.night])).toBe(false);
    });

    it('Вернёт true для пустого массива', () => {
        expect(timeOfDay.isDefaultValue([])).toBe(true);
    });
});
