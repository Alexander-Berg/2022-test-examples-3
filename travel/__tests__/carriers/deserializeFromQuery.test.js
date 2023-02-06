'use strict';

jest.dontMock('../../baseFilterManager');
const carriers = require.requireActual('../../carriers').default;

describe('carriers', () => {
    describe('deserializeFromQuery', () => {
        it('query contains no carriers', () => {
            const value = carriers.deserializeFromQuery({foo: 'bar'});

            expect(value).toEqual([]);
        });

        it('query contains single carrier', () => {
            const value = carriers.deserializeFromQuery({carrier: '123'});

            expect(value).toEqual(['123']);
        });

        it('value has two carriers', () => {
            const value = carriers.deserializeFromQuery({
                carrier: ['123', '456'],
            });

            expect(value).toEqual(['123', '456']);
        });
    });
});
