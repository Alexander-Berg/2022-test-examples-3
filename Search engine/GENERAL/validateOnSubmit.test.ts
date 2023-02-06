import {validateOnSubmit} from './validateOnSubmit';

describe('validateOnSubmit', () => {
    describe('RAW mode', () => {
        test('has errors', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'RAW',
                space: 'KPI',
            });
            expect(errors).not.toBeNull();
            expect(data).toBeNull();
        });
        test('success', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'RAW',
                space: 'KPI',
                queries: JSON.stringify(
                    [{text: 'some query', regionId: 213}],
                    null,
                    4,
                ),
            });
            expect(errors).toBeNull();
            expect(data).not.toBeNull();
        });
    });
    describe('MERGING mode', () => {
        test('has errors', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'MERGING',
            });
            expect(errors).not.toBeNull();
            expect(data).toBeNull();
        });
        test('success', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'MERGING',
                mergedBasketIds: [101],
            });
            expect(errors).toBeNull();
            expect(data).not.toBeNull();
        });
    });
    describe('INTERSECTING mode', () => {
        test('has errors', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'INTERSECTING',
            });
            expect(errors).not.toBeNull();
            expect(data).toBeNull();
        });
        test('success', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'INTERSECTING',
                intersectedBasketIds: [101],
            });
            expect(errors).toBeNull();
            expect(data).not.toBeNull();
        });
    });
    describe('FILTERING mode', () => {
        test('has errors', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'FILTERING',
            });
            expect(errors).not.toBeNull();
            expect(data).toBeNull();
        });
        test('has errors filteredBasketId', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'FILTERING',
                expression: 'some',
            });
            expect(errors).not.toBeNull();
            expect(data).toBeNull();
        });
        test('has errors expression', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'FILTERING',
                filteredBasketId: 100,
            });
            expect(errors).not.toBeNull();
            expect(data).toBeNull();
        });
        test('success', () => {
            const [errors, data] = validateOnSubmit({
                author: 'robot',
                name: 'test-basket',
                type: 'FILTERING',
                filteredBasketId: 101,
                expression: 'some',
            });
            expect(errors).toBeNull();
            expect(data).not.toBeNull();
        });
    });
});
