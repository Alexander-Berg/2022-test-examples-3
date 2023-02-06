'use strict';

const partition = require('./../../../utils/partition');

describe('partition', () => {
    test('should return empty object if partitions is undefined', () => {
        const data = {
            just: 'literal'
        };

        const expected = undefined;
        const actual = partition(data);

        expect(actual).toEqual(expected);
    });

    test('should return default result (undefined: ...)', () => {
        const partitions = {
            by: (data) => data.expected,
            default: 'expected'
        };

        const actual = partition(partitions);

        expect(actual).toBeUndefined();
    });

    test('should return correct result (simple)', () => {
        const data = {
            expected: true
        };

        const partitions = {
            by: (data) => data.expected,
            partitions: {
                true: {
                    result: 'expected'
                },
                false: {
                    result: 'unexpected'
                }
            }
        };

        const expected = { result: 'expected' };
        const actual = partition(data, partitions);

        expect(actual).toEqual(expected);
    });

    test('should return correct result (complex)', () => {
        const data = {
            fields: {
                a: 'a',
                b: 'b',
                c: 'c',
                d: 'd'
            },
            numbers: [1, 2, 3, 4, 5]
        };

        const partitions = {
            by: (data) => data.numbers.length,
            partitions: {
                4: {
                    result: 'unexpected'
                },
                5: {
                    by: (data) => data.fields.b,
                    partitions: {
                        a: {
                            result: 'unexpected'
                        },
                        b: {
                            result: 'expected'
                        }
                    }
                }
            }
        };

        const expected = { result: 'expected' };
        const actual = partition(data, partitions);

        expect(actual).toEqual(expected);
    });

    test('should return correct result (country example from the description)', () => {
        const unrecognized = 'Unrecognized';
        const expectedCountry = 'Kazakhstan';

        const partitionObj = {
            by: (person) => person.nationality,
            def: unrecognized,
            partitions: {
                kazakh: {
                    by: (person) => person.age > 17 && person.age < 26,
                    partitions: {
                        true: expectedCountry
                    }
                }
            }
        };

        const ivandyach = {
            age: 24,
            nationality: 'kazakh'
        };

        const danillewin = {
            age: 25,
            nationality: 'polish'
        };

        const elergy = {
            age: 24,
            nationality: 'russian'
        };

        expect(partition(ivandyach, partitionObj)).toBe(expectedCountry);
        expect(partition(elergy, partitionObj)).toBe(unrecognized);
        expect(partition(danillewin, partitionObj)).toBe(unrecognized);
    });
});
