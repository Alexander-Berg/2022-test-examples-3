'use strict';

jest.dontMock('../../baseFilterManager');
const carriers = require.requireActual('../../carriers').default;

const value = ['1', '2', '3'];

describe('carriers', () => {
    describe('apply', () => {
        it('segment without carrier', () => {
            const segment = {title: 'Moscow - Omsk'};

            const applyResult = carriers.apply(value, segment);

            expect(applyResult).toBe(false);
        });

        it('empty value', () => {
            const segment = {title: 'Moscow - Omsk', company: {id: 1}};

            const applyResult = carriers.apply([], segment);

            expect(applyResult).toBe(false);
        });

        it('value does no include segment carrier', () => {
            const segment = {title: 'Moscow - Omsk', company: {id: 4}};

            const applyResult = carriers.apply(value, segment);

            expect(applyResult).toBe(false);
        });

        it('value includes segment carrier', () => {
            const segment = {title: 'Moscow - Omsk', company: {id: 2}};

            const applyResult = carriers.apply(value, segment);

            expect(applyResult).toBe(true);
        });
    });
});
