import { trim } from '../trim';

describe('Util trim', () => {
    describe('#trim', () => {
        it('returns trimmed string', () => {
            const value = `
                abc bca
            `;

            expect(trim(value)).toBe('abc bca');
        });
    });
});
