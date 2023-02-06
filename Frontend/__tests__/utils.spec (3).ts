import dayjs from 'dayjs';

import { mapValues, sortBy, uniqueId } from '../utils';

describe('utils/utils uniqueId', () => {
    it('increments id every time', () => {
        expect(uniqueId()).toBe('1');
        expect(uniqueId()).toBe('2');
        expect(uniqueId()).toBe('3');
    });

    it('adds prefix when a singe argument provided', () => {
        expect(uniqueId('id_')).toBe('id_4');
    });
});

describe('utils/utils mapValues', () => {
    it('increments all history values', () => {
        const obj = {
            a: 1,
            b: 2,
            c: 3,
        };
        const targetObj = {
            a: 2,
            b: 3,
            c: 4,
        };
        const transformFunction = (a: number) => a + 1;

        expect(mapValues(obj, transformFunction)).toMatchObject(targetObj);
    });

    it('transforms all numbers to strings in history values', () => {
        const obj = {
            a: 1,
            b: 2,
            c: 3,
        };
        const targetObj = {
            a: '1',
            b: '2',
            c: '3',
        };
        const transformFunction = (a: number) => a.toString();

        expect(mapValues(obj, transformFunction)).toMatchObject(targetObj);
    });

    it('does nothing on empty history', () => {
        const obj = {};
        const targetObj = {};
        const transformFunction = (a: number) => a.toString();

        expect(mapValues(obj, transformFunction)).toMatchObject(targetObj);
    });
});

describe('utils/utils sortBy', () => {
    it('sorts objects by string field', () => {
        const arr = [
            {
                name: 'JavaScript',
            },
            {
                name: 'C++',
            },
            {
                name: 'TypeScript',
            },
        ];
        const targetArr = [
            {
                name: 'C++',
            },
            {
                name: 'JavaScript',
            },
            {
                name: 'TypeScript',
            },
        ];

        expect(sortBy(arr, ['name'])).toEqual(targetArr);
    });
    it('sorts objects by number field', () => {
        const arr = [
            {
                name: 'JavaScript',
                year: 1995,
            },
            {
                name: 'C++',
                year: 1985,
            },
            {
                name: 'TypeScript',
                year: 2012,
            },
        ];
        const targetArr = [
            {
                name: 'C++',
                year: 1985,
            },
            {
                name: 'JavaScript',
                year: 1995,
            },
            {
                name: 'TypeScript',
                year: 2012,
            },
        ];

        expect(sortBy(arr, ['year'])).toEqual(targetArr);
    });
    it('sorts objects by date field', () => {
        const arr = [
            {
                name: 'JavaScript',
                year: dayjs('Jan 1 1995').valueOf(),
            },
            {
                name: 'C++',
                year: dayjs('Jan 1 1985').valueOf(),
            },
            {
                name: 'TypeScript',
                year: dayjs('Jan 1 2012').valueOf(),
            },
        ];
        const targetArr = [
            {
                name: 'C++',
                year: dayjs('Jan 1 1985').valueOf(),
            },
            {
                name: 'JavaScript',
                year: dayjs('Jan 1 1995').valueOf(),
            },
            {
                name: 'TypeScript',
                year: dayjs('Jan 1 2012').valueOf(),
            },
        ];

        expect(sortBy(arr, ['year'])).toEqual(targetArr);
    });
    it('sorts objects by multiple fields', () => {
        const arr = [
            {
                name: 'JavaScript',
                usedByProjects: 3,
            },
            {
                name: 'C++',
                usedByProjects: 2,
            },
            {
                name: 'TypeScript',
                usedByProjects: 3,
            },
        ];
        const targetArr = [
            {
                name: 'C++',
                usedByProjects: 2,
            },
            {
                name: 'JavaScript',
                usedByProjects: 3,
            },
            {
                name: 'TypeScript',
                usedByProjects: 3,
            },
        ];

        expect(sortBy(arr, ['usedByProjects', 'name'])).toEqual(targetArr);
    });
});
