import getSeatsTitle from '../getSeatsTitle';

import threadKeyset from '../../../i18n/thread';

describe('getSeatsTitle', () => {
    it('Для количества мест === 1 вернет "место"', () => {
        expect(getSeatsTitle(1)).toBe(threadKeyset.get('one-seat'));
    });
    it('Для количества мест === 201 вернет "место"', () => {
        expect(getSeatsTitle(201)).toBe(threadKeyset.get('one-seat'));
    });
    it('Для количества мест === 0 вернет "мест"', () => {
        expect(getSeatsTitle(0)).toBe(threadKeyset.get('zero-or-many-seats'));
    });
    it('Для количества мест === 5010 вернет "мест"', () => {
        expect(getSeatsTitle(5010)).toBe(
            threadKeyset.get('zero-or-many-seats'),
        );
    });
    it('Для количества мест === 5 вернет "мест"', () => {
        expect(getSeatsTitle(5)).toBe(threadKeyset.get('zero-or-many-seats'));
    });
    it('Для количества мест === 205 вернет "мест"', () => {
        expect(getSeatsTitle(205)).toBe(threadKeyset.get('zero-or-many-seats'));
    });
    it('Для количества мест === 2 вернет "места"', () => {
        expect(getSeatsTitle(2)).toBe(threadKeyset.get('few-seats'));
    });
    it('Для количества мест === 92 вернет "места"', () => {
        expect(getSeatsTitle(92)).toBe(threadKeyset.get('few-seats'));
    });
    it('Для количества мест === 2.5 вернет пустую строку', () => {
        expect(getSeatsTitle(2.5)).toBe('');
    });
    it('Для количества мест === undefined вернет "мест"', () => {
        expect(getSeatsTitle(undefined)).toBe(
            threadKeyset.get('zero-or-many-seats'),
        );
    });
    it('Для количества мест === 11 вернет "мест"', () => {
        expect(getSeatsTitle(11)).toBe(threadKeyset.get('zero-or-many-seats'));
    });
});
