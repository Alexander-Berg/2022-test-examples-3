import statesContext from '../../../../components/redux/store/reducers/states-context';
import {
    DESELECT_RESOURCES,
    DESELECT_ALL,
    SET_SELECTED,
    SET_HIGHLIGHTED,
    SET_VIEW,
    UPDATE_SORT
} from '../../../../components/redux/store/actions/types';
import deepFreeze from 'deep-freeze';

describe('states-context reducer', () => {
    describe('DESELECT_RESOURCES', () => {
        it('Не должен ничего делать если ресурс не выделен', () => {
            const state = {
                selected: ['resource-1', 'resource-2']
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: DESELECT_RESOURCES,
                payload: {
                    resourcesIds: ['resource-3']
                }
            });
            expect(newState).toBe(state);
        });
        it('Должен удалить ресурс из выделения', () => {
            const state = {
                selected: ['resource-1', 'resource-2']
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: DESELECT_RESOURCES,
                payload: {
                    resourcesIds: ['resource-1']
                }
            });
            expect(newState.selected).toEqual(['resource-2']);
        });
        it('Должен удалить несколько ресурсов из выделения', () => {
            const state = {
                selected: ['resource-1', 'resource-2', 'resource-3']
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: DESELECT_RESOURCES,
                payload: {
                    resourcesIds: ['resource-2', 'resource-3', 'resource-4']
                }
            });
            expect(newState.selected).toEqual(['resource-1']);
        });
    });

    describe('DESELECT_ALL', () => {
        it('Должен сбросить выделение', () => {
            const state = {
                selected: ['resource-1', 'resource-2']
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: DESELECT_ALL
            });
            expect(newState).toEqual({
                selected: []
            });
        });
        it('Не должен ничего делать если нет выделения', () => {
            const state = {
                selected: []
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: DESELECT_ALL
            });
            expect(newState).toBe(state);
        });
    });

    describe('SET_SELECTED', () => {
        it('Должен обновить `selected`', () => {
            const state = {
                selected: ['resource-1', 'resource-2']
            };
            deepFreeze(state);
            const newSelected = ['resource-3', 'resource-4'];
            const newState = statesContext(state, {
                type: SET_SELECTED,
                payload: {
                    selected: newSelected
                }
            });
            expect(newState).toEqual({
                selected: newSelected
            });
        });
    });

    describe('SET_HIGHLIGHTED', () => {
        it('Должен обновить `highlighted`', () => {
            const state = {
                highlighted: ['resource-1', 'resource-2']
            };
            deepFreeze(state);
            const newHighlighted = ['resource-3', 'resource-4'];
            const newState = statesContext(state, {
                type: SET_HIGHLIGHTED,
                payload: {
                    highlighted: newHighlighted
                }
            });
            expect(newState).toEqual({
                highlighted: newHighlighted
            });
        });
    });

    describe('SET_VIEW', () => {
        it('Должен обновить `view`', () => {
            const state = {
                view: 'icons'
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: SET_VIEW,
                payload: {
                    view: 'tile'
                }
            });
            expect(newState).toEqual({
                view: 'tile'
            });
        });
    });

    describe('UPDATE_SORT', () => {
        it('Не должен ничего делать если не передали `idContext`', () => {
            const state = {
                sort: {}
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: UPDATE_SORT,
                payload: {
                    sort: 'name',
                    order: '1'
                }
            });
            expect(newState).toBe(state);
        });

        it('Должен добавить сортировку для переданного `idContext`', () => {
            const state = {
                sort: {}
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: UPDATE_SORT,
                payload: {
                    idContext: '/disk',
                    sort: 'name',
                    order: '1'
                }
            });
            expect(newState).toEqual({
                sort: {
                    '/disk': {
                        sort: 'name',
                        order: '1'
                    }
                }
            });
        });

        it('Должен обновить сортировку существующего `idContext`', () => {
            const state = {
                sort: {
                    '/disk': {
                        sort: 'name',
                        order: '1'
                    }
                }
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: UPDATE_SORT,
                payload: {
                    idContext: '/disk',
                    sort: 'mdate',
                    order: '0'
                }
            });
            expect(newState).toEqual({
                sort: {
                    '/disk': {
                        sort: 'mdate',
                        order: '0'
                    }
                }
            });
        });

        it('Должен добавить сортировку для нового `idContext`', () => {
            const state = {
                sort: {
                    '/disk': {
                        sort: 'name',
                        order: '1'
                    }
                }
            };
            deepFreeze(state);
            const newState = statesContext(state, {
                type: UPDATE_SORT,
                payload: {
                    idContext: '/recent',
                    sort: 'mdate',
                    order: '0'
                }
            });
            expect(newState).toEqual({
                sort: {
                    '/disk': {
                        sort: 'name',
                        order: '1'
                    },
                    '/recent': {
                        sort: 'mdate',
                        order: '0'
                    }
                }
            });
        });
    });
});
