'use strict';

const baobab = require('../../utils/baobab');

describe('baobab', () => {
    describe('query', () => {
        it('Не должен найти узел в пустом объекте', () => {
            assert.isEmpty(baobab.query('/$page/$main', {}));
        });

        it('Должен найти узел $main', () => {
            let tree = {
                name: '$page',
                children: [
                    { name: '$header' },
                    { name: '$main' }
                ]
            };
            const expected = [{ name: '$main' }];

            assert.deepEqual(baobab.query('/$page/$main', tree), expected);
        });

        it('Должен найти все узлы $result', () => {
            let tree = {
                name: '$page',
                children: [
                    { name: '$header' },
                    {
                        name: '$main',
                        children: [
                            { name: '$result', attrs: { type: 'organic' }, children: [] },
                            { name: '$result', children: [] },
                            { name: '$adv' }
                        ]
                    }
                ]
            };
            const expected = [
                { name: '$result', attrs: { type: 'organic' }, children: [] },
                { name: '$result', children: [] }
            ];

            assert.deepEqual(baobab.query('/$page/$main/$result', tree), expected);
        });

        it('Должен найти все узел $result с атрибутами', () => {
            let tree = {
                name: '$page',
                children: [
                    { name: '$header' },
                    {
                        name: '$main',
                        children: [
                            { name: '$result', attrs: { type: 'organic' }, children: [] },
                            { name: '$result', children: [] },
                            { name: '$adv' }
                        ]
                    }
                ]
            };
            const expected = [
                { name: '$result', attrs: { type: 'organic' }, children: [] }
            ];

            assert.deepEqual(baobab.query('/$page/$main/$result', tree, { type: 'organic' }), expected);
        });

        it('Не должен найти узел $result с атрибутами без заданных атрибутов', () => {
            let tree = {
                name: '$page',
                children: [
                    {
                        name: '$main',
                        children: [
                            { name: '$result', attrs: { type: 'organic' }, children: [] }
                        ]
                    }
                ]
            };

            assert.isEmpty(baobab.query('/$page/$main/$result', tree, { type: 'adv' }));
        });

        it('Должен найти узел $result с игнорируя атрибуты oldPath и oldVars ', () => {
            let tree = {
                name: '$page',
                children: [
                    {
                        name: '$main',
                        children: [
                            {
                                name: '$result',
                                attrs: { type: 'organic', oldPath: 'web/item', oldVars: 'pos=p0' },
                                children: []
                            }
                        ]
                    }
                ]
            };
            const expected = [{
                name: '$result',
                attrs: { type: 'organic', oldPath: 'web/item', oldVars: 'pos=p0' },
                children: []
            }];

            assert.deepEqual(baobab.query('/$page/$main/$result', tree, { type: 'organic' }), expected);
        });
    });
});
