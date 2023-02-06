const { shiftLocation } = require('./utils');

describe('src/lib/tokenize-skip-node/utils', () => {
    describe('Shift location', () => {
        it('Should shift location after formatter opening', () => {
            const now = {
                line: 1,
                column: 1,
                offset: 0,
            };

            shiftLocation(now, '%%(md)  \n', 0, 9);

            expect(now).toEqual({
                line: 2,
                column: 1,
                offset: 9,
            });
        });

        it('Should shift location after formatter multiline opening', () => {
            const now = {
                line: 1,
                column: 1,
                offset: 0,
            };

            shiftLocation(now, '%%(\nmd\n)  \n', 0, 11);

            expect(now).toEqual({
                line: 4,
                column: 1,
                offset: 11,
            });
        });

        it('Should shift location after inline formatter opening', () => {
            const now = {
                line: 1,
                column: 1,
                offset: 0,
            };

            shiftLocation(now, '%%(md) test %%  \n', 0, 6);

            expect(now).toEqual({
                line: 1,
                column: 7,
                offset: 6,
            });
        });
    });
});
