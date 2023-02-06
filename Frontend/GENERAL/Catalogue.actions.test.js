import {
    ALL_NODE_CHILDREN_REQUEST,
    ALL_NODE_CHILDREN_SET,
    NODE_CHILDREN_UPDATE_REQUEST,
    NODE_CHILDREN_UPDATE,
    NODE_CHILDREN_ERROR_SET,
    NODE_CHILDREN_DATA_STATUS_SET,
    NODE_OPENED_SET,
    NODE_CLOSED_SET,

    UPDATE_REQUEST,
    UPDATE,
    ERROR_SET,
    DATA_STATUS_SET,
    LABELS_SET,

    FILTERS_SET,
    FILTERS_CLEAR,
} from './Catalogue.actions';

import {
    requestAllNodeChildren,
    setAllNodeChildren,
    requestNodeChildrenUpdate,
    updateNodeChildren,
    setNodeChildrenError,
    setNodeChildrenDataStatus,
    setNodeOpened,
    setNodeClosed,

    update,
    setError,
    requestUpdate,
    setDataStatus,
    setLabels,

    setFilters,
    clearFilters,
} from './Catalogue.actions';

describe('Testing catalogue actions', () => {
    it('Should create payload for all node children request', () => {
        expect(requestAllNodeChildren(100, 42)).toEqual({
            type: ALL_NODE_CHILDREN_REQUEST,
            payload: {
                nodeId: 100,
                rootServiceId: 42,
            },
        });
    });
    it('Should create payload for all node children setting', () => {
        expect(setAllNodeChildren(100, [1, 2, 3])).toEqual({
            type: ALL_NODE_CHILDREN_SET,
            payload: {
                nodeId: 100,
                children: [1, 2, 3],
            },
        });
    });
    it('Should create payload for node children update request', () => {
        expect(requestNodeChildrenUpdate(100, { foo: 'bar' }, 123)).toEqual({
            type: NODE_CHILDREN_UPDATE_REQUEST,
            payload: {
                nodeId: 100,
                filters: { foo: 'bar' },
                rootServiceId: 123,
            },
        });
    });
    it('Should create payload for node children update', () => {
        expect(updateNodeChildren(100, [1, 2, 3])).toEqual({
            type: NODE_CHILDREN_UPDATE,
            payload: {
                nodeId: 100,
                children: [1, 2, 3],
            },
        });
    });
    it('Should create payload for setting node children error', () => {
        expect(setNodeChildrenError(42, { foo: 'bar' })).toEqual({
            type: NODE_CHILDREN_ERROR_SET,
            payload: {
                nodeId: 42,
                childrenError: { foo: 'bar' },
            },
        });
    });
    it('Should create payload for setting node children data status', () => {
        expect(setNodeChildrenDataStatus(42, 'loading')).toEqual({
            type: NODE_CHILDREN_DATA_STATUS_SET,
            payload: {
                nodeId: 42,
                childrenDataStatus: 'loading',
            },
        });
    });
    it('Should create payload for setting node opened', () => {
        expect(setNodeOpened()).toEqual({
            type: NODE_OPENED_SET,
        });
    });
    it('Should create payload for setting node closed', () => {
        expect(setNodeClosed()).toEqual({
            type: NODE_CLOSED_SET,
        });
    });
    it('Should create payload for catalogue update request', () => {
        expect(requestUpdate({ foo: 'bar' }, 42)).toEqual({
            type: UPDATE_REQUEST,
            payload: {
                filters: { foo: 'bar' },
                rootServiceId: 42,
            },
        });
    });
    it('Should create payload for catalogue update', () => {
        expect(update({ foo: 'bar' }, 42)).toEqual({
            type: UPDATE,
            payload: { foo: 'bar' },
            meta: { rootServiceId: 42 },
        });
    });
    it('Should create payload for setting catalogue error', () => {
        expect(setError({ foo: 'bar' })).toEqual({
            type: ERROR_SET,
            payload: { foo: 'bar' },
        });
    });
    it('Should create payload for setting catalogue data status', () => {
        expect(setDataStatus('nice-and-slow')).toEqual({
            type: DATA_STATUS_SET,
            payload: 'nice-and-slow',
        });
    });
    it('Should create payload for setting catalogue labels', () => {
        expect(setLabels('member', { foo: 'bar' })).toEqual({
            type: LABELS_SET,
            payload: { control: 'member', results: { foo: 'bar' } },
        });
    });
    it('Should create payload for setting filters', () => {
        expect(setFilters({ foo: 'bar' }, true)).toEqual({
            type: FILTERS_SET,
            payload: {
                newFilters: { foo: 'bar' },
                savingEnabled: true,
            },
        });
    });
    it('Should create payload for clearing filters', () => {
        expect(clearFilters({ foo: 'bar' }, true)).toEqual({
            type: FILTERS_CLEAR,
            payload: {
                filters: { foo: 'bar' },
                savingEnabled: true,
            },
        });
    });
});
