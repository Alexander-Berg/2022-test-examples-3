const isUssrThread = require.requireActual('../isUssrThread').default;

jest.unmock('../../countries');

describe('isUssrThread', () => {
    it('should return false if the thread is equal to null', () => {
        expect(isUssrThread(null)).toBe(false);
    });

    it('should return false to thread without stations info', () => {
        expect(isUssrThread({})).toBe(false);
    });

    it('should return false to international thread', () => {
        expect(
            isUssrThread({
                firstCountryCode: 'RU',
                lastCountryCode: 'FR',
            }),
        ).toBe(false);
    });

    it('should return true to ussr countries thread', () => {
        expect(
            isUssrThread({
                firstCountryCode: 'RU',
                lastCountryCode: 'BY',
            }),
        ).toBe(true);
    });
});
