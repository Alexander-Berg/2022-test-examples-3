import {
    parseDepartmentSuggestResults,
    parseDepartmentDetailsResults,

    parseStaffSuggestResults,
    parseStaffDetailsResults,

    parseTagsSuggestResults,
    parseTagsDetailsResults,
} from './utils';

describe('Filters utils', () => {
    describe('parseDepartmentSuggestResults', () => {
        const result1 = [
            'oh',
            'no',
            { _id: 1, name: 'the partment', _type: 'goat' },
        ];
        const result2 = [
            undefined,
            undefined,
            { _id: 2, name: 'qwe', _type: 'wolf' },
        ];

        it('Should parse data from 3rd element', () => {
            const actual = parseDepartmentSuggestResults([result1]);

            expect(actual[0].id).toBe(1);
            expect(actual[0].name).toMatchObject({
                ru: 'the partment',
                en: 'the partment',
            });
            expect(actual[0]._type).toBe('goat');
        });

        it('Should keep results order', () => {
            const actual = parseDepartmentSuggestResults([result2, result1]);

            expect(actual[0].id).toBe(2);
            expect(actual[1].id).toBe(1);
        });
    });

    describe('parseDepartmentDetailsResults', () => {
        const result1 = {
            id: 42,
            name: 'Какой-то там департамент',
            name_en: 'Department of might, ought and would have been',
        };
        const result2 = {
            id: 11,
            name: 'А Имя',
            name_en: 'A Name',
        };

        it('Should take data from results', () => {
            const actual = parseDepartmentDetailsResults([result1]);

            expect(actual[0].id).toBe(42);
            expect(actual[0].name).toMatchObject({
                ru: 'Какой-то там департамент',
                en: 'Department of might, ought and would have been',
            });
        });

        it('Should have type department', () => {
            const actual = parseDepartmentDetailsResults([result1]);

            expect(actual[0]._type).toBe('department');
        });

        it('Should pick first department in each result', () => {
            const actual = parseDepartmentDetailsResults([result2]);

            expect(actual[0].id).toBe(11);
        });

        it('Should keep order of results', () => {
            const actual = parseDepartmentDetailsResults([result2, result1]);

            expect(actual[0].id).toBe(11);
            expect(actual[1].id).toBe(42);
        });

        it('Should ignore incorrect/empty results', () => {
            const actual = parseDepartmentDetailsResults([
                undefined,
                result1,
                null,
                result2,
            ]);

            expect(actual.length).toBe(2);
            expect(actual[0].id).toBe(42);
            expect(actual[1].id).toBe(11);
        });
    });

    describe('parseStaffSuggestResults', () => {
        const result1 = [
            undefined,
            undefined,
            { person: { login: 'jeff' }, _text: 'Jeff', _type: 'wibbly-wobbly' },
        ];
        const result2 = [
            undefined,
            undefined,
            { person: { login: 'cap' }, _text: 'Malcolm Reynolds', _type: 'timey-wimey' },
        ];

        it('Should parse data from 3rd element', () => {
            const actual = parseStaffSuggestResults([result1]);

            expect(actual[0].id).toBe('jeff');
            expect(actual[0].name).toMatchObject({
                ru: 'Jeff',
                en: 'Jeff',
            });
            expect(actual[0]._type).toBe('wibbly-wobbly');
        });

        it('Should keep results order', () => {
            const actual = parseStaffSuggestResults([result2, result1]);

            expect(actual[0].id).toBe('cap');
            expect(actual[1].id).toBe('jeff');
        });
    });

    describe('parseStaffDetailsResults', () => {
        const result1 = {
            results: [{
                login: 'merlin',
                name: {
                    en: 'Merlin',
                    ru: 'Мерлин',
                },
            }],
        };
        const result2 = {
            results: [{
                login: 'aaa',
                name: {
                    en: 'AAA',
                    ru: 'ААА',
                },
            }, {
                login: 'kkk',
                name: {
                    en: 'KKK',
                    ru: 'ККК',
                },
            }],
        };

        it('Should take data from results', () => {
            const actual = parseStaffDetailsResults([result1]);

            expect(actual[0].id).toBe('merlin');
            expect(actual[0].name).toEqual({
                en: 'Merlin',
                ru: 'Мерлин',
            });
        });

        it('Should have type staff', () => {
            const actual = parseStaffDetailsResults([result1]);

            expect(actual[0]._type).toBe('staff');
        });

        it('Should pick first result in each request', () => {
            const actual = parseStaffDetailsResults([result2]);

            expect(actual[0].id).toBe('aaa');
        });

        it('Should keep order of results', () => {
            const actual = parseStaffDetailsResults([result2, result1]);

            expect(actual[0].id).toBe('aaa');
            expect(actual[1].id).toBe('merlin');
        });

        it('Should ignore incorrect/empty results', () => {
            const actual = parseStaffDetailsResults([
                undefined,
                result1,
                null,
                {},
                result2,
                { results: null },
                { results: [] },
            ]);

            expect(actual.length).toBe(2);
            expect(actual[0].id).toBe('merlin');
            expect(actual[1].id).toBe('aaa');
        });
    });

    describe('parseTagsSuggestResults', () => {
        const result1 = [
            undefined,
            undefined,
            { _id: 1, _text: 'tag1', color: 'unconscious frog' },
        ];
        const result2 = [
            undefined,
            undefined,
            { _id: 2, _text: 'tag2', color: 'frightened nymph hips' },
        ];

        it('Should parse data from 3rd element', () => {
            const actual = parseTagsSuggestResults([result1]);

            expect(actual[0].id).toBe(1);
            expect(actual[0].name).toMatchObject({
                ru: 'tag1',
                en: 'tag1',
            });
            expect(actual[0].color).toBe('unconscious frog');
        });

        it('Should have type tag', () => {
            const actual = parseTagsSuggestResults([result1]);

            expect(actual[0]._type).toBe('tag');
        });

        it('Should keep results order', () => {
            const actual = parseTagsSuggestResults([result2, result1]);

            expect(actual[0].id).toBe(2);
            expect(actual[1].id).toBe(1);
        });
    });

    describe('parseTagsDetailsResults', () => {
        const item1 = {
            id: 42,
            name: { en: 'tag1', ru: 'тег1' },
            color: 'red',
        };
        const item2 = {
            id: 11,
            name: { en: 'tag2', ru: 'тег2' },
            color: 'green',
        };

        it('Should take data from results', () => {
            const actual = parseTagsDetailsResults([item1]);

            expect(actual[0].id).toBe(42);
            expect(actual[0].name).toMatchObject({ en: 'tag1', ru: 'тег1' });
            expect(actual[0].color).toBe('red');
        });

        it('Should have type tag', () => {
            const actual = parseTagsDetailsResults([item1]);

            expect(actual[0]._type).toBe('tag');
        });

        it('Should keep order of results', () => {
            const actual = parseTagsDetailsResults([item2, item1]);

            expect(actual[0].id).toBe(11);
            expect(actual[1].id).toBe(42);
        });

        it('Should ignore empty results', () => {
            const actual = parseTagsDetailsResults([
                undefined,
                item1,
                null,
                item2,
            ]);

            expect(actual.length).toBe(2);
            expect(actual[0].id).toBe(42);
            expect(actual[1].id).toBe(11);
        });
    });
});
