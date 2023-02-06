import TimeOfDay from '../../../../interfaces/date/TimeOfDay';

import timeOfDay from '../../timeOfDay';

describe('timeOfDay.isAvailableWithOptions', () => {
    it('Если время прибытия/отправления сегментов соответствует только одному времени суток - вернёт false', () => {
        expect(timeOfDay.isAvailableWithOptions([TimeOfDay.day])).toBe(false);
    });

    it('Если время прибытия/отправления сегментов соответствует разному времени суток - вернёт true', () => {
        expect(
            timeOfDay.isAvailableWithOptions([TimeOfDay.day, TimeOfDay.night]),
        ).toBe(true);
    });
});
