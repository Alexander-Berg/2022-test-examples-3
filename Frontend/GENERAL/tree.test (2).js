import {
    dfs,
    buildTree,
    createTreeNode,
    getLevelVar,
    updateNode,
    addChildren,
    mergeChildren,
    closeNode,
} from './tree';

import {
    errorCode,
} from './errors';

const { BEM_LANG } = process.env;

describe('Tree utils', () => {
    describe('dfs', () => {
        it('Should exclude root node from traversing', () => {
            const tree = {
                '0': { isRoot: true, children: [] },
            };

            let visited = false;

            dfs(tree, 0, () => {
                visited = true;
            }, true);

            expect(visited).toBe(false);
        });

        it('Should visit all nodes in DFS order', () => {
            const tree = {
                '0': { isRoot: true, children: ['1', '2'] },
                '1': { children: ['1.1', '1.2'] },
                '2': { children: ['2.1'] },
                '1.1': { children: [] },
                '1.2': { children: [] },
                '2.1': { children: [] },
            };

            let index = 0;

            dfs(tree, 0, node => {
                node.index = ++index;
            }, false, false);

            expect(tree).toEqual({
                '0': { isRoot: true, children: ['1', '2'] },
                '1': { index: 1, children: ['1.1', '1.2'] },
                '1.1': { index: 2, children: [] },
                '1.2': { index: 3, children: [] },
                '2': { index: 4, children: ['2.1'] },
                '2.1': { index: 5, children: [] },
            });
        });

        it('Should skip closed nodes', () => {
            const tree = {
                '0': { isRoot: true, children: ['1', '2'] },
                '1': { children: ['1.1', '1.2'] },
                '1.1': { children: [] },
                '1.2': { children: [] },
                '2': { isOpen: true, children: ['2.1'] },
                '2.1': { children: [] },
            };

            let index = 0;

            dfs(tree, 0, node => {
                node.index = ++index;
            }, true, false);

            expect(tree).toEqual({
                '0': { isRoot: true, children: ['1', '2'] },
                '1': { index: 1, children: ['1.1', '1.2'] },
                '1.1': { children: [] },
                '1.2': { children: [] },
                '2': { index: 2, isOpen: true, children: ['2.1'] },
                '2.1': { index: 3, children: [] },
            });
        });

        it('Should return first matched node', () => {
            const tree = {
                '0': { isRoot: true, children: ['1', '2'] },
                '1': { id: '1', data: 'irrelevant', children: ['1.1', '1.2'] },
                '2': { id: '2', data: 'important', children: ['2.1'] },
                '1.1': { id: '1.1', data: 'important', children: [] },
                '1.2': { id: '1.2', data: 'irrelevant', children: [] },
                '2.1': { id: '2.1', data: 'important', children: [] },
            };

            const node = dfs(tree, 0, node => node.data === 'important', false, true);

            expect(node.id).toBe('1.1');
        });
    });

    describe('buildTree', () => {
        const input = [
            { id: 1, parent: null, name: { [BEM_LANG]: 'Сервис 1' } },
            { id: 2, parent: null, name: { [BEM_LANG]: 'Сервис 2' } },
            { id: '1.1', parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 1.1' } },
            { id: '1.2', parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 1.2' } },
            { id: '2.1', parent: { id: 2 }, name: { [BEM_LANG]: 'Сервис 2.1' } },
        ];
        const rootServiceId = 0;

        it('Should add a root node with passed id', () => {
            expect(buildTree([], 42).tree).toEqual({
                '42': {
                    isRoot: true,
                    children: [],
                },
            });
        });

        it('Should build a tree', () => {
            const expectedTree = {
                '0': expect.objectContaining({
                    isRoot: true,
                    children: [1, 2],
                }),
                '1': expect.objectContaining({
                    data: { id: 1, parent: null, name: { [BEM_LANG]: 'Сервис 1' } },
                    isOpen: true,
                    childrenDataStatus: 'inited',
                    childrenError: null,
                    children: ['1.1', '1.2'],
                }),
                '2': expect.objectContaining({
                    data: { id: 2, parent: null, name: { [BEM_LANG]: 'Сервис 2' } },
                    isOpen: true,
                    childrenDataStatus: 'inited',
                    childrenError: null,
                    children: ['2.1'],
                }),
                '1.1': expect.objectContaining({
                    data: { id: '1.1', parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 1.1' } },
                    isOpen: false,
                    childrenDataStatus: 'pending',
                    childrenError: null,
                    children: [],
                }),
                '1.2': expect.objectContaining({
                    data: { id: '1.2', parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 1.2' } },
                    isOpen: false,
                    childrenDataStatus: 'pending',
                    childrenError: null,
                    children: [],
                }),
                '2.1': expect.objectContaining({
                    data: { id: '2.1', parent: { id: 2 }, name: { [BEM_LANG]: 'Сервис 2.1' } },
                    isOpen: false,
                    childrenDataStatus: 'pending',
                    childrenError: null,
                    children: [],
                }),
            };

            expect(buildTree(input, rootServiceId).tree).toEqual(expectedTree);
        });

        it('Should return error because of violation of the tree structure', () => {
            const input = [
                { id: '1.2', parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 1.2' } },
                { id: 1, parent: null, name: { [BEM_LANG]: 'Сервис 1' } },
                { id: 2, parent: null, name: { [BEM_LANG]: 'Сервис 2' } },
                { id: '1.1', parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 1.1' } },
                { id: '2.1', parent: { id: 2 }, name: { [BEM_LANG]: 'Сервис 2.1' } },
            ];
            const rootServiceId = 0;

            expect(buildTree(input, rootServiceId).error.data.code).toEqual(errorCode.TREE_CONSTRUCTION_ERROR);
        });
    });

    describe('createTreeNode', () => {
        it('Should create a node with default values', () => {
            expect(createTreeNode({
                foo: 'bar',
            })).toMatchObject({
                data: {
                    foo: 'bar',
                },
                isOpen: false,
                children: [],
                childrenDataStatus: 'pending',
                childrenError: null,
            });
        });
    });

    describe('getLevelVar', () => {
        it('Should return CSS variable', () => {
            expect(getLevelVar(10)).toEqual({
                '--level': 10,
            });
        });
    });

    describe('updateNode', () => {
        it('Should update tree branch, keeping references to untouched nodes', () => {
            const initialTree = {
                '1': {
                    data: { id: 1, parent: null },
                    children: ['1.1', '1.2'],
                },
                '2': {
                    data: { id: 2, parent: null },
                    children: [],
                },
                '1.1': {
                    data: { id: '1.1', parent: { id: 1 } },
                    children: ['1.1.1'],
                },
                '1.2': {
                    data: { id: '1.2', parent: { id: 1 } },
                    children: [],
                },
                '1.1.1': {
                    data: { id: '1.1.1', parent: { id: '1.1' } },
                    children: [],
                },
            };

            const newTree = updateNode(initialTree, '1.1.1', { isOpen: true });

            expect(newTree).not.toBe(initialTree);
            expect(newTree['1']).toBe(initialTree['1']);
            expect(newTree['1.1']).toBe(initialTree['1.1']);
            expect(newTree['1.1.1']).not.toBe(initialTree['1.1.1']);
            expect(newTree['1.1.1'].isOpen).toBe(true);
            expect(newTree['1.2']).toBe(initialTree['1.2']);
            expect(newTree['2']).toBe(initialTree['2']);
        });
    });

    describe('addChildren', () => {
        it('Should add children to a node', () => {
            const initialTree = {
                '1': {
                    data: { id: 1, parent: null },
                    children: [],
                },
            };

            const items = [
                { id: 2, parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 2' } },
                { id: 3, parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 3' } },
            ];

            const expected = {
                '1': expect.objectContaining({
                    data: { id: 1, parent: null },
                    children: [2, 3],
                }),
                '2': expect.objectContaining({
                    data: { id: 2, parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 2' } },
                    isOpen: false,
                    children: [],
                    childrenDataStatus: 'pending',
                    childrenError: null,
                }),
                '3': expect.objectContaining({
                    data: { id: 3, parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 3' } },
                    isOpen: false,
                    children: [],
                    childrenDataStatus: 'pending',
                    childrenError: null,
                }),
            };

            expect(addChildren(initialTree, 1, items)).toEqual(expected);
        });
    });

    describe('mergeChildren', () => {
        it('Should merge node children', () => {
            const initialTree = {
                '1': {
                    data: { id: 1, parent: null },
                    children: [2, 3],
                },
                '2': {
                    data: { id: 2, matched: true }, // должно дополниться данными
                    isOpen: false,
                    children: [],
                    childrenDataStatus: 'pending',
                },
                '3': {
                    data: { id: 3, matched: false }, // должно дополниться данными и оставить matched false
                    isOpen: true,
                    children: [],
                    childrenDataStatus: 'inited',
                    childrenError: 'oh no',
                },
            };

            const items = [
                { id: 2, parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 2' }, matched: true },
                { id: 3, parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 3' }, matched: true },
                { id: 4, parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 4' }, matched: true },
            ];

            const expected = {
                '1': expect.objectContaining({
                    data: { id: 1, parent: null },
                    children: [2, 3, 4],
                }),
                '2': expect.objectContaining({
                    data: items[0],
                    isOpen: false,
                    children: [],
                    childrenDataStatus: 'pending',
                    childrenError: null,
                }),
                '3': expect.objectContaining({
                    data: {
                        ...items[1],
                        matched: false,
                    },
                    isOpen: true,
                    children: [],
                    childrenDataStatus: 'inited',
                    childrenError: 'oh no',
                }),
                '4': expect.objectContaining({
                    data: {
                        ...items[2],
                        matched: false, // должно вытавиться в false, потому что изначально элемента не было
                    },
                    isOpen: false,
                    children: [],
                    childrenDataStatus: 'pending',
                    childrenError: null,
                }),
            };

            expect(mergeChildren(initialTree, 1, items)).toEqual(expected);
        });
    });

    describe('closeNode', () => {
        it('Should close all nodes in a subtree', () => {
            const initialTree = {
                '1': {
                    data: { id: '1', parent: null },
                    children: ['1.1', '1.2'],
                    isOpen: true,
                },
                '1.1': {
                    data: { id: '1.1', parent: { id: '1' } },
                    children: ['1.1.1'],
                    isOpen: true,
                },
                '1.2': {
                    data: { id: '1.2', parent: { id: '1' } },
                    children: [],
                    isOpen: true,
                },
                '1.1.1': {
                    data: { id: '1.1.1', parent: { id: '1.1' } },
                    children: ['1.1.1.1'],
                    isOpen: true,
                },
                '1.1.1.1': {
                    data: { id: '1.1.1.1', parent: { id: '1.1.1' } },
                    children: [],
                    isOpen: true,
                },
                '2': {
                    data: { id: '2', parent: null },
                    children: [],
                    isOpen: true,
                },
            };

            const newTree = closeNode(initialTree, '1.1');

            expect(newTree).not.toBe(initialTree);
            expect(newTree['1']).toBe(initialTree['1']);
            expect(newTree['2']).toBe(initialTree['2']);
            expect(newTree['1.1']).not.toBe(initialTree['1.1']);
            expect(newTree['1.2']).toBe(initialTree['1.2']);
            expect(newTree['1.1.1']).not.toBe(initialTree['1.1.1']);
            expect(newTree['1.1.1.1']).not.toBe(initialTree['1.1.1.1']);

            expect(newTree['1'].isOpen).toBe(true);
            expect(newTree['2'].isOpen).toBe(true);
            expect(newTree['1.1'].isOpen).toBe(false);
            expect(newTree['1.2'].isOpen).toBe(true);
            expect(newTree['1.1.1'].isOpen).toBe(false);
            expect(newTree['1.1.1.1'].isOpen).toBe(false);
        });
    });
});
