const { isEscaped } = require('./spec');

describe('isEscaped()', () => {
    it('Should check if char escaped by \\', () => {
        expect(isEscaped('\\1', 1))
            .toEqual(true);

        expect(isEscaped('\\1', 0))
            .toEqual(false);

        expect(isEscaped('\\1', 2))
            .toEqual(false);

        expect(isEscaped('\\\\1', 2))
            .toEqual(false);

        expect(isEscaped('1\\\\1', 3))
            .toEqual(false);

        expect(isEscaped('\\\\1', 1))
            .toEqual(true);

        expect(isEscaped('1\\\\1', 2))
            .toEqual(true);

        expect(isEscaped('\\\\\\1', 3))
            .toEqual(true);

        expect(isEscaped('1\\\\\\1', 4))
            .toEqual(true);
    });
});
