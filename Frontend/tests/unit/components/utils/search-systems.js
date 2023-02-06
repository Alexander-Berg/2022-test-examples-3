const { mergeFeatures } = require('../../../../src/client/components/utils/search-systems.js');

describe('components/utils/search-systems', () => {
    describe('mergeFeatures', () => {
        it('должен возвращать актуальный список включенных фич для новой системы', () => {
            const systemFeatures = {
                'features-map': {
                    a: { isCheckedByDefault: true },
                    b: { isCheckedByDefault: true },
                    c: { isCheckedByDefault: true },
                    d: { isCheckedByDefault: true },
                    e: { isCheckedByDefault: true },
                },
                'sys-type-features': {
                    'sys-type-0': ['a', 'b', 'c'],
                    'sys-type-1': ['c', 'd', 'e'],
                },
            };

            assert.deepEqual(mergeFeatures(['a', 'b', 'c'], 'sys-type-1', systemFeatures, 'sys-type-0'), ['d', 'e', 'c']);
        });

        it('должен возвращать актуальный список включенных фич для новой системы в случае, если предшествующий тип системы не передан', () => {
            const systemFeatures = {
                'features-map': {
                    a: { isCheckedByDefault: true },
                    b: { isCheckedByDefault: true },
                    c: { isCheckedByDefault: true },
                    d: { isCheckedByDefault: false },
                    e: { isCheckedByDefault: true },
                    f: { isCheckedByDefault: true },
                },
                'sys-type-features': {
                    'sys-type-0': ['a', 'b', 'c'],
                    'sys-type-1': ['c', 'd', 'e', 'f'],
                },
            };

            assert.deepEqual(mergeFeatures(['a', 'b', 'c'], 'sys-type-1', systemFeatures), ['c', 'e', 'f']);
        });

        it('должен возвращать пустой список, если тип системы не поддерживается', () => {
            const systemFeatures = {
                'features-map': {
                    a: { isCheckedByDefault: true },
                    b: { isCheckedByDefault: true },
                    c: { isCheckedByDefault: true },
                    d: { isCheckedByDefault: false },
                    e: { isCheckedByDefault: true },
                    f: { isCheckedByDefault: true },
                },
                'sys-type-features': {
                    'sys-type-0': ['a', 'b', 'c'],
                    'sys-type-1': ['c', 'd', 'e', 'f'],
                },
            };

            assert.deepEqual(mergeFeatures(['a', 'b', 'c'], 'sys-type-3', systemFeatures), []);
        });

        it('должен возвращать актуальный список включенных фич, игнорируя незарегестрированные фичи', () => {
            const systemFeatures = {
                'features-map': {
                    a: { isCheckedByDefault: true },
                    b: { isCheckedByDefault: true },
                    c: { isCheckedByDefault: true },
                    d: { isCheckedByDefault: false },
                    e: { isCheckedByDefault: true },
                    f: { isCheckedByDefault: true },
                },
                'sys-type-features': {
                    'sys-type-0': ['a', 'b', 'c'],
                    'sys-type-1': ['c', 'd', 'e', 'f'],
                },
            };

            assert.deepEqual(mergeFeatures(['a', 'b', 'c', 'z'], 'sys-type-1', systemFeatures), ['c', 'e', 'f']);
        });
    });
});
