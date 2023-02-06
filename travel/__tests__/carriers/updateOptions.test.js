'use strict';

jest.dontMock('../../baseFilterManager');
const carriers = require.requireActual('../../carriers').default;

describe('carriers', () => {
    describe('updateOptions', () => {
        it('segment without carrier', () => {
            const segment = {
                title: 'Moscow - Omsk',
                transport: {code: 'plane'},
            };
            const options = 'options';

            const newOptions = carriers.updateOptions(options, segment);

            expect(newOptions).toBe(options);
        });

        it('train segment', () => {
            const segment = {
                title: 'Moscow - Omsk',
                transport: {code: 'train'},
            };
            const options = 'options';

            const newOptions = carriers.updateOptions(options, segment);

            expect(newOptions).toBe(options);
        });

        it('options already include segment carrier', () => {
            const segment = {
                title: 'Moscow - Omsk',
                transport: {code: 'plane'},
                company: {id: 2},
            };
            const options = [{id: '1'}, {id: '2'}];

            const newOptions = carriers.updateOptions(options, segment);

            expect(newOptions).toBe(options);
        });

        it('options do not include segment carrier', () => {
            const segment = {
                title: 'Moscow - Omsk',
                transport: {code: 'plane'},
                company: {id: 3, title: 'S7'},
            };
            const options = [
                {id: '1', title: 'Aeroflot'},
                {id: '2', title: 'OrenAir'},
            ];

            const newOptions = carriers.updateOptions(options, segment);

            expect(newOptions).toEqual([
                {id: '1', title: 'Aeroflot'},
                {id: '2', title: 'OrenAir'},
                {id: '3', title: 'S7'},
            ]);
        });
    });
});
