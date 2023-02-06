import { formatPhone } from '.';

describe('Phone', () => {
    describe('formatPhone', () => {
        it('должен форматировать телефон с кодом +7', () => {
            expect(formatPhone('+79121231231')).toBe('+7 912 123-12-31');
        });

        it('должен форматировать телефон с кодом 8', () => {
            expect(formatPhone('89121231231')).toBe('8 912 123-12-31');
        });

        it('должен форматировать телефон с произвольным кодом', () => {
            expect(formatPhone('+60123123123123')).toBe('+6 012 312-31-23 123');
        });

        it('должен форматировать неполный телефон', () => {
            expect(formatPhone('+')).toBe('+');
            expect(formatPhone('+79')).toBe('+7 9');
            expect(formatPhone('+79121')).toBe('+7 912 1');
            expect(formatPhone('+791212312')).toBe('+7 912 123-12');
        });

        it('должен убирать лишние символы из телефона', () => {
            expect(formatPhone('+79121231231a')).toBe('+7 912 123-12-31');
            expect(formatPhone('+7912a')).toBe('+7 912');
            expect(formatPhone('+7912.')).toBe('+7 912');
            expect(formatPhone('+7912 ')).toBe('+7 912');
            expect(formatPhone('+7912123-')).toBe('+7 912 123');
        });

        it('должен ограничивать число цифр для телефонов с кодом +7 и 8', () => {
            expect(formatPhone('+791212312319')).toBe('+7 912 123-12-31');
            expect(formatPhone('891212312319')).toBe('8 912 123-12-31');
        });

        it('не должен ограничивать число цифр для телефона с произвольным кодом', () => {
            expect(formatPhone('+60123123123123123123')).toBe('+6 012 312-31-23 123 123123');
        });
    });
});
