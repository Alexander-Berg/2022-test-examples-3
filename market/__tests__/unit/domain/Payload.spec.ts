import Payload from '../../../src/domain/models/domain-info/Payload';
import DataUtils from '../../../src/utils/DataUtils';

describe('Payload', () => {
    describe('clean', () => {
        it('should return undefined', () => {
            expect(DataUtils.cleanData(undefined)).toBeUndefined();
            expect(DataUtils.cleanData(null)).toBeUndefined();
            expect(DataUtils.cleanData('')).toBeUndefined();

            expect(DataUtils.cleanData({})).toBeUndefined();
            expect(DataUtils.cleanData({ empty: '' })).toBeUndefined();
            expect(DataUtils.cleanData({ empty: {} })).toBeUndefined();
            expect(DataUtils.cleanData({ null: null })).toBeUndefined();
            expect(DataUtils.cleanData({ undefined })).toBeUndefined();

            expect(DataUtils.cleanData([])).toBeUndefined();
            expect(DataUtils.cleanData(['', ''])).toBeUndefined();
            expect(DataUtils.cleanData([undefined])).toBeUndefined();
            expect(DataUtils.cleanData([null])).toBeUndefined();

            expect(DataUtils.cleanData([{ empty: '' }])).toBeUndefined();
            expect(DataUtils.cleanData([{ empty: [] }])).toBeUndefined();
            expect(DataUtils.cleanData([{ empty: [null, undefined] }])).toBeUndefined();
            expect(DataUtils.cleanData([{ empty: [undefined, ''] }, ''])).toBeUndefined();
        });

        it('should remove empty arrays', () => {
            expect(DataUtils.cleanData([])).toBeUndefined();
            expect(DataUtils.cleanData([[], 1, [], 2, []])).toEqual([1, 2]);
            expect(DataUtils.cleanData({ empty: [[]], number: 1 })).toEqual({ number: 1 });
        });

        it('should remove empty objects', () => {
            expect(DataUtils.cleanData({})).toBeUndefined();
            expect(DataUtils.cleanData({ empty: {} })).toBeUndefined();
            expect(DataUtils.cleanData({ empty: { num: 1, empty: {} }, other: {} })).toEqual({ empty: { num: 1 } });
        });

        it('should remove duplicates from array', () => {
            expect(DataUtils.cleanData([1, 2, 3, 1, 2, 3, undefined])).toEqual([1, 2, 3]);
            expect(DataUtils.cleanData([[{ empty: '' }, 2, 2, ''], [null, 2]])).toEqual([[2]]);
        });

        it('should remove productPageSelector if urlTemplates presents', () => {
            const payload = new Payload({
                urlTemplates: ['string1', 'string2'],
                productPageSelector: 'string1',
            });
            payload.clean();
            expect(payload.data).toEqual({ urlTemplates: ['string1', 'string2'] });
        });

        it("shouldn't remove productPageSelector if urlTemplates doesn't present", () => {
            const payload = new Payload({
                productPageSelector: 'string1',
            });
            payload.clean();
            expect(payload.data).toEqual({ productPageSelector: 'string1' });
        });

        it("shouldn't remove urlTemplates if productPageSelector doesn't present", () => {
            const payload = new Payload({
                urlTemplates: ['string1', 'string2'],
            });
            payload.clean();
            expect(payload.data).toEqual({ urlTemplates: ['string1', 'string2'] });
        });
    });

    describe('diff', () => {
        test('difference between empty payloads', () => {
            const first = new Payload({});
            const second = new Payload({});

            expect(Payload.diff(first, second)).toEqual({});
            expect(Payload.diff(second, first)).toEqual({});
        });

        test('1 ToDo: name', () => {
            const first = new Payload({});
            const second = new Payload({ a: 1, b: 2, c: { d: 3 }, e: [{ f: 4 }, 5] });

            expect(Payload.diff(first, second)).toEqual({ a: 1, b: 2, c: { d: 3 }, e: [{ f: 4 }, 5] });
        });

        test('2 ToDo: name', () => {
            const first = new Payload({ a: 1, b: 2, c: { d: 3 }, e: [{ f: 4 }, 5] });
            const second = new Payload({});

            expect(Payload.diff(first, second)).toEqual({});
        });

        test('3 ToDo: name', () => {
            const first = new Payload({ a: 1, b: 2, c: [3, 4, 5], d: [{ e: 6 }, { g: 7 }] });
            const second = new Payload({ b: 2, a: 1, c: [3, 5], d: [{ g: 7 }, { e: 5 }] });

            expect(Payload.diff(first, second)).toEqual({ d: [{ e: 5 }] });
        });

        test('4 ToDo: name', () => {
            const first = new Payload({ b: 2, a: 1, c: [3, 5], d: [{ g: 7 }, { e: 5 }] });
            const second = new Payload({ a: 1, b: 2, c: [3, 4, 5], d: [{ e: 6 }, { g: 7 }] });

            expect(Payload.diff(first, second)).toEqual({ c: [4], d: [{ e: 6 }] });
        });

        test('5 ToDo: name', () => {
            const first = new Payload({ a: { b: 1, c: 2 }, d: { e: [1, 2, 3] } });
            const second = new Payload({ a: { c: 1 }, d: { e: [3, 4, 5] } });

            expect(Payload.diff(first, second)).toEqual({ a: { c: 1 }, d: { e: [4, 5] } });
        });

        test('6 ToDo: name', () => {
            const first = new Payload({ a: { c: 1 } });
            const second = new Payload({ a: { b: 1, c: 2 }, d: { e: [1, 2, 3] } });

            expect(Payload.diff(first, second)).toEqual({ a: { b: 1, c: 2 }, d: { e: [1, 2, 3] } });
        });

        test('7 ToDo: name', () => {
            const first = new Payload({ a: { b: 1, c: 2 }, d: { e: [1, 2, 2, 3] } });
            const second = new Payload({ d: { e: [3, 1, 2, 3] }, a: { c: 2, b: 1 } });

            expect(Payload.diff(first, second)).toEqual({});
        });
    });

    describe('full diff ', () => {
        test('8 ToDo: name', () => {
            const first = new Payload({ a: 'test', d: 'test' });
            const second = new Payload({ a: 'test' });

            expect(Payload.fullDiff(first, second)).toEqual({ d: 'test' });
        });

        test('9 ToDo: name', () => {
            const first = new Payload({ a: 'test', d: { c: 'test', d: 'test' } });
            const second = new Payload({ a: 'test', d: { c: 'test' } });

            expect(Payload.fullDiff(first, second)).toEqual({ d: { d: 'test' } });
        });

        test('10 ToDo: name', () => {
            const first = new Payload({ a: 'a', d: { c: 'c', d: 'd', e: 'e' } });
            const second = new Payload({ a: 'a', d: { c: 'c' }, b: 'b' });

            expect(Payload.fullDiff(first, second)).toEqual({ d: { d: 'd', e: 'e' }, b: 'b' });
        });

        test('11 ToDo: name', () => {
            const first = new Payload({ a: 'a', d: { c: 'c' }, b: 'b' });
            const second = new Payload({ a: 'a', d: { c: 'c' }, b: 'b' });

            expect(Payload.fullDiff(first, second)).toEqual({});
        });
    });
});
