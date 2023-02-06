import DateSpecialValue from '../../../interfaces/date/DateSpecialValue';

import isTodaySearch from '../isTodaySearch';

describe('isTodaySearch', () => {
    it('если не определен контекст - возвращаем false', () => {
        expect(isTodaySearch()).toBe(false);
        expect(isTodaySearch({when: {}})).toBe(false);
    });

    it('если контекст отличный от поиска на сегодня - возвращаем false', () => {
        expect(
            isTodaySearch({
                when: {
                    special: 'tomorrow',
                },
            }),
        ).toBe(false);
    });

    it('если контекст соответствует поиску на сегодня - возвращаем true', () => {
        expect(
            isTodaySearch({
                when: {
                    special: DateSpecialValue.today,
                },
            }),
        ).toBe(true);
    });
});
