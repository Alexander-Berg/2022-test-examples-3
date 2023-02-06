'use strict';

jest.dontMock('../utils');
const parse = require.requireActual('../parse');

describe('parse', () => {
    describe('parseSorting', () => {
        it('parse departure asc', () => {
            expect(parse.parseSorting('departure')).toEqual({
                by: 'departure',
                reverse: false,
            });
        });

        it('parse departure desc', () => {
            expect(parse.parseSorting('-departure')).toEqual({
                by: 'departure',
                reverse: true,
            });
        });

        it('parse arrival asc', () => {
            expect(parse.parseSorting('departure')).toEqual({
                by: 'departure',
                reverse: false,
            });
        });

        it('parse arrival desc', () => {
            expect(parse.parseSorting('-arrival')).toEqual({
                by: 'arrival',
                reverse: true,
            });
        });

        it('parse unknown asc', () => {
            expect(parse.parseSorting('unknown')).toEqual({
                by: 'departure',
                reverse: false,
            });
        });

        it('parse unknown desc', () => {
            expect(parse.parseSorting('unknown')).toEqual({
                by: 'departure',
                reverse: false,
            });
        });

        it('parse empty', () => {
            expect(parse.parseSorting('')).toEqual({
                by: 'departure',
                reverse: false,
            });
        });
    });
});
