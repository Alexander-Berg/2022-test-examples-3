import DateSpecialValue from '../../../interfaces/date/DateSpecialValue';

import isAllDaysSearch from '../isAllDaysSearch';

describe('isAllDaysSearch', () => {
    it('если не определен контекст - возвращаем false', () => {
        expect(isAllDaysSearch()).toBe(false);
        expect(isAllDaysSearch({when: {}})).toBe(false);
    });

    it('если контекст отличный от поиска на все дни - возвращаем false', () => {
        expect(
            isAllDaysSearch({
                when: {
                    special: DateSpecialValue.tomorrow,
                },
            }),
        ).toBe(false);
    });

    it('если контекст соответствует поиску на все дни - возвращаем true', () => {
        expect(
            isAllDaysSearch({
                when: {
                    special: DateSpecialValue.allDays,
                },
            }),
        ).toBe(true);
    });
});
