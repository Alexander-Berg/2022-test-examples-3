import { maskPhone } from '../LcContactsPhone/LcContactsPhone';

describe('maskPhone', () => {
    it('should transform value without spaces', () => {
        expect(maskPhone('+79261234567')).toBe('+7926XXXXXXX');
    });

    it('should transform value with spaces', () => {
        expect(maskPhone('+7 926 123 45 67')).toBe('+7 926 XXX XX XX');
    });

    it('should transform value other symbols', () => {
        expect(maskPhone('+7 (926) 123-45-67')).toBe('+7 (926) XXX-XX-XX');
    });

    it('should transform value 8 in the beginning', () => {
        expect(maskPhone('89261234567')).toBe('8926XXXXXXX');
    });
});
