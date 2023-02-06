import handleActions from './Catalogue.reducers';
import {
    ALL_NODE_CHILDREN_SET,

    NODE_CHILDREN_UPDATE,
    NODE_CHILDREN_ERROR_SET,
    NODE_CHILDREN_DATA_STATUS_SET,
    NODE_OPENED_SET,
    NODE_CLOSED_SET,

    UPDATE,
    ERROR_SET,
    DATA_STATUS_SET,
    LABELS_SET,
} from './Catalogue.actions';

const { BEM_LANG } = process.env;

const TREE_MOCK = {
    '0': {
        isRoot: true,
        children: [1],
    },
    '1': {
        data: { id: 1, parent: null, name: { [BEM_LANG]: 'Родительский сервис' } },
        children: [2],
    },
    '2': {
        data: { id: 2, parent: { id: 1 }, name: { [BEM_LANG]: 'Дочерний сервис' } },
        children: [],
    },
};

const CHILDREN_RESULT_MOCK = {
    results: [
        {
            name: { [BEM_LANG]: 'Такси' },
            id: 10,
            parent: { id: 1 },
        },
        {
            name: { [BEM_LANG]: 'Арарат' },
            id: 11,
            parent: { id: 1 },
        },
    ],
};

const ALL_CHILDREN_RESULT_MOCK = {
    results: [
        ...CHILDREN_RESULT_MOCK.results,
        { ...TREE_MOCK[2].data },
    ],
};

describe('Should handle actions', () => {
    // initial conditions
    expect(TREE_MOCK[1].children.length).toBe(1);
    expect(TREE_MOCK[2]).not.toBeUndefined();
    expect(TREE_MOCK[10]).toBeUndefined();
    expect(TREE_MOCK[11]).toBeUndefined();

    describe('ALL_NODE_CHILDREN_SET', () => {
        let actual;

        beforeEach(() => {
            actual = handleActions({
                tree: TREE_MOCK,
                status: 'pending',
            }, {
                type: ALL_NODE_CHILDREN_SET,
                payload: {
                    nodeId: 1,
                    children: ALL_CHILDREN_RESULT_MOCK,
                },
                meta: {},
            });
        });

        it('Should add new children to the node', () => {
            expect(actual.tree[1].children.length).toBe(3);
            expect(actual.tree[2].data).toMatchObject(TREE_MOCK[2].data);
            expect(actual.tree[10].data).toMatchObject(CHILDREN_RESULT_MOCK.results[0]);
            expect(actual.tree[11].data).toMatchObject(CHILDREN_RESULT_MOCK.results[1]);
        });
    });

    describe('NODE_CHILDREN_UPDATE', () => {
        let actual;

        beforeEach(() => {
            actual = handleActions({
                tree: TREE_MOCK,
                status: 'pending',
            }, {
                type: NODE_CHILDREN_UPDATE,
                payload: {
                    nodeId: 1,
                    children: CHILDREN_RESULT_MOCK,
                },
                meta: {},
            });
        });

        it('Should replace node children with new ones', () => {
            expect(actual.tree[1].children).toEqual([10, 11]);
            expect(actual.tree[10].data).toMatchObject(CHILDREN_RESULT_MOCK.results[0]);
            expect(actual.tree[11].data).toMatchObject(CHILDREN_RESULT_MOCK.results[1]);

            // данные не удаляются из стора
            // но в реальности не может быть ситуации, когда новые данные не дополнят существующие, а изменят их
            expect(actual.tree[2]).not.toBeUndefined();
        });
    });

    it('NODE_CHILDREN_ERROR_SET', () => {
        expect(TREE_MOCK[2].childrenError).toBeUndefined(); // initial condition

        const actual = handleActions({
            tree: TREE_MOCK,
        }, {
            type: NODE_CHILDREN_ERROR_SET,
            payload: {
                nodeId: 2,
                childrenError: { foo: 'bar' },
            },
        });

        expect(actual.tree[2].childrenError).toEqual({ foo: 'bar' });
    });

    it('NODE_CHILDREN_DATA_STATUS_SET', () => {
        expect(TREE_MOCK[1].childrenDataStatus).toBeUndefined(); // initial condition

        const actual = handleActions({
            tree: TREE_MOCK,
        }, {
            type: NODE_CHILDREN_DATA_STATUS_SET,
            payload: {
                nodeId: 1,
                childrenDataStatus: 'inited',
            },
        });

        expect(actual.tree[1].childrenDataStatus).toBe('inited');
    });

    it('NODE_OPENED_SET', () => {
        expect(TREE_MOCK[1].isOpen).toBeFalsy(); // initial condition

        const actual = handleActions({
            tree: TREE_MOCK,
        }, {
            type: NODE_OPENED_SET,
            payload: 1,
        });

        expect(actual.tree[1].isOpen).toBe(true);
    });

    it('NODE_CLOSED_SET', () => {
        expect(TREE_MOCK[1].isOpen).toBeUndefined(); // initial condition

        const actual = handleActions({
            tree: TREE_MOCK,
        }, {
            type: NODE_CLOSED_SET,
            payload: 1,
        });

        expect(actual.tree[1].isOpen).toBe(false);
    });

    describe('UPDATE', () => {
        let actual;

        beforeEach(() => {
            actual = handleActions({
                status: 'pending',
                tree: {},
            }, {
                type: UPDATE,
                payload: {
                    results: [
                        { id: 1, parent: null, name: { [BEM_LANG]: 'Сервис 1' } },
                        { id: 2, parent: null, name: { [BEM_LANG]: 'Сервис 2' } },
                        { id: '1.1', parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 1.1' } },
                        { id: '1.2', parent: { id: 1 }, name: { [BEM_LANG]: 'А это Сервис 1.2' } },
                    ],
                },
                meta: {
                    rootServiceId: 0,
                },
            });
        });

        it('Response is stored as a tree', () => {
            expect(actual.tree).toEqual({
                '0': expect.objectContaining({ children: [1, 2] }),
                '1': expect.objectContaining({
                    data: { id: 1, parent: null, name: { [BEM_LANG]: 'Сервис 1' } },
                    children: ['1.1', '1.2'],
                }),
                '2': expect.objectContaining({
                    data: { id: 2, parent: null, name: { [BEM_LANG]: 'Сервис 2' } },
                    children: [],
                }),
                '1.1': expect.objectContaining({
                    data: { id: '1.1', parent: { id: 1 }, name: { [BEM_LANG]: 'Сервис 1.1' } },
                    children: [],
                }),
                '1.2': expect.objectContaining({
                    data: { id: '1.2', parent: { id: 1 }, name: { [BEM_LANG]: 'А это Сервис 1.2' } },
                    children: [],
                }),
            });
        });

        it('Stores current filter mode', () => {
            actual = handleActions({}, {
                type: UPDATE,
                payload: {
                    results: [],
                    filter_mode: 'anything,really',
                },
                meta: { rootServiceId: 0 },
            });

            expect(actual.filterMode).toEqual('anything,really');
        });
    });

    it('ERROR_SET', () => {
        const actual = handleActions({
            status: 'pending',
            error: null,
        }, {
            type: ERROR_SET,
            payload: { foo: 'bar' },
        });

        const expected = {
            status: 'pending',
            error: { foo: 'bar' },
        };

        expect(actual).toEqual(expected);
    });

    it('DATA_STATUS_SET', () => {
        const actual = handleActions({
            status: 'pending',
            error: null,
        }, {
            type: DATA_STATUS_SET,
            payload: 'inited',
        });

        const expected = {
            status: 'inited',
            error: null,
        };

        expect(actual).toEqual(expected);
    });

    it('creates correct data for LABELS_SET action', () => {
        const actual = handleActions({
            status: 'pending',
            error: null,
            labels: {},
        }, {
            type: LABELS_SET,
            payload: {
                control: 'member',
                results: [{
                    id: '213',
                    name: { ru: 'ru', en: 'en' },
                }],
            },
        });

        const expected = {
            '213': 'ru',
        };

        expect(actual.labels.member).toEqual(expected);
    });
});
