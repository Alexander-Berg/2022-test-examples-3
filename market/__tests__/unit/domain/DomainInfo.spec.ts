import DataUtils from '../../../src/utils/DataUtils';

describe('DomainInfo', () => {
    describe('object to keys', () => {
        test('1 ToDo: name', () => {
            const keys = DataUtils.objectToKeys({});

            expect(keys).toEqual([]);
        });

        test('2 ToDo: name', () => {
            const keys = DataUtils.objectToKeys({ a: [1, 2, 3], b: { c: 4 } });

            expect(keys).toEqual(['a', 'b_c']);
        });

        test('3 ToDo: name', () => {
            const keys = DataUtils.objectToKeys({ a: [1, 2, 3], b: { c: 4, d: { e: { f: 5, g: [6, 7] } } } });

            expect(keys).toEqual(['a', 'b_c', 'b_d_e_f', 'b_d_e_g']);
        });

        test('4 ToDo: name', () => {
            const keys = DataUtils.objectToKeys({ a: { b: { c: { d: 2 } } } });

            expect(keys).toEqual(['a_b_c_d']);
        });

        test('5 ToDo: name', () => {
            const keys = DataUtils.objectToKeys({ a: { b: [1, 2, 3] }, c: ['first', 'second'], d: { e: { f: 1 } } });

            expect(keys).toEqual(['a_b', 'c', 'd_e_f']);
        });

        test('6 ToDo: name', () => {
            const keys = DataUtils.objectToKeys({ a: { b: [1, 2, 3] }, c: ['', 'second'], d: { e: { f: 1 } }, g: '' });

            expect(keys).toEqual(['a_b', 'c', 'd_e_f', 'g']);
        });
    });
});
