var Regress = require('../../Regress/Regress').Regress,
    assert = require('assert');

describe('Regress', function() {
    var extractFlattenNodeClasses = Regress.extractFlattenNodeClasses,
        extractPrivList = Regress.extractPrivList;

    describe('extractFlattenNodeClasses', function() {
        it('получаем список классов для блока в bemjson', function() {
            var bemjson = [
                {
                    block: 'block-1',
                    content: [
                        {
                            block: 'block-2',
                            content: {
                                elem: 'elem'
                            }
                        },
                        {
                            block: 'block-4',
                            content: {
                                elem: 'elem'
                            }
                        }
                    ]
                },
                { block: 'block-3', content: 'block-3' }
            ];

            assert.deepEqual(extractFlattenNodeClasses(bemjson, 'block-2'), ['block-2', 'block-2__elem']);
        });

        it('получаем список классов для блока в bemjson, если mix является родителем', function() {
            var bemjson = {
                block: 'block-1',
                mix: {
                    block: 'block-2'
                },
                content: {
                    block: 'block-2',
                    content: 'test'
                }
            };

            assert.deepEqual(extractFlattenNodeClasses(bemjson, 'block-2'), ['block-1', 'block-2']);
        });

        it('получаем список классов для блока в bemjson, если присутсвует mix параллельно', function() {
            var bemjson = [
                {
                    block: 'block-2',
                    content: {
                        elem: 'elem'
                    }
                },
                {
                    block: 'block-1',
                    mix: {
                        block: 'block-2'
                    }
                }
            ];

            var expected = ['block-2', 'block-2__elem', 'block-1'];

            assert.deepEqual(extractFlattenNodeClasses(bemjson, 'block-2'), expected);
        });

        it('получаем список селекторов при поиске элемента блока', function() {
            var bemjson = [
                {
                    block: 'block-1',
                    content: {
                        elem: 'elem',
                        content: {
                            block: 'block-2',
                            content: 'test'
                        }
                    }
                },
                {
                    block: 'block-3'
                }
            ];

            var expected = ['block-1__elem', 'block-2'];

            assert.deepEqual(extractFlattenNodeClasses(bemjson, 'block-1__elem'), expected);
        });

        it('получаем пустой список классов, если в bemjson блок не присутствует', function() {
            var bemjson = [
                {
                    block: 'block-1',
                    content: {
                        elem: 'elem'
                    }
                },
                {
                    block: 'block-3',
                    mix: {
                        block: 'block-1'
                    }
                }
            ];

            assert.deepEqual(extractFlattenNodeClasses(bemjson, 'block-2'), []);
        });

        it('получаем список классов, если в bemjson содержимое в нестандартных полях', function() {
            var bemjson = [
                {
                    block: 'block-1',
                    content: {
                        elem: 'elem',
                        custom: {
                            block: 'block-2',
                            content: {
                                elem: 'elem'
                            }
                        }
                    }
                },
            ];

            assert.deepEqual(extractFlattenNodeClasses(bemjson, 'block-2'), ['block-2', 'block-2__elem']);
        });

        it('получаем список классов, если в bemjson содержимое в attrs', function() {
            var bemjson = [
                {
                    block: 'block-1',
                    content: {
                        elem: 'elem',
                        attrs: {
                            id: {
                                block: 'block-2',
                                content: {
                                    elem: 'elem'
                                }
                            }
                        }
                    }
                },
            ];

            assert.deepEqual(extractFlattenNodeClasses(bemjson, 'block-2'), ['block-2', 'block-2__elem']);
        });
    });

    describe('extractPrivList', function() {
        it('должен вернуть список привов из дерева для блока', function() {
            var privTree = [{
                name: 'z',
                children: [
                    {
                        name: 'a',
                        children: [
                            {
                                name: 'b',
                                children: [
                                    {
                                        name: 'c',
                                        children: []
                                    }
                                ]
                            },
                            {
                                name: 'd',
                                children: []
                            }
                        ]
                    }
                ]
            }];

            assert.deepEqual(extractPrivList(privTree, 'a'), ['a', 'b', 'c', 'd']);
        });

        it('должен очистить список от дубликатов', function() {
            var privTree = [{
                name: 'z',
                children: [
                    {
                        name: 'a',
                        children: [
                            {
                                name: 'b',
                                children: []
                            },
                            {
                                name: 'b',
                                children: []
                            }
                        ]
                    }
                ]
            }];

            assert.deepEqual(extractPrivList(privTree, 'a'), ['a', 'b']);
        });

        it('должен вернуть пустой массив, если искомый блок не найден в дереве привов', function() {
            var privTree = [{
                name: 'z',
                children: [
                    {
                        name: 'a',
                        children: []
                    }
                ]
            }];

            assert.deepEqual(extractPrivList(privTree, 'b'), []);
        });

        it('должен вернуть список всех вхождений блока в дереве', function() {
            var privTree = [{
                name: 'z',
                children: [
                    {
                        name: 'a',
                        children: [
                            {
                                name: 'b', children: []
                            }
                        ]
                    },
                    {
                        name: 'z', children: []
                    },
                    {
                        name: 'a',
                        children: [
                            {
                                name: 'd', children: []
                            }
                        ]
                    }
                ]
            }];

            assert.deepEqual(extractPrivList(privTree, 'a'), ['a', 'b', 'd']);
        });

        it('должен найти в дереве блок, если совпадение имени не полное', function() {
            var privTree = [{
                name: 'z',
                children: [
                    {
                        name: 'a_type_b',
                        children: [
                            {
                                name: 'b', children: []
                            }
                        ]
                    }
                ]
            }];

            assert.deepEqual(extractPrivList(privTree, 'a_type'), ['a_type_b', 'b']);
        });

        it('не должен найти в дереве блок, если начало имени не совпало', function() {
            var privTree = [{
                name: 'z',
                children: [
                    {
                        name: 'a_type_b',
                        children: [
                            {
                                name: 'b', children: []
                            }
                        ]
                    }
                ]
            }];

            assert.deepEqual(extractPrivList(privTree, 'type_b'), []);
        });
    });
});
