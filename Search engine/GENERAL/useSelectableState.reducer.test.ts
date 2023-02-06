import {getInitialState} from './getInitialState';
import {reducerFactory} from './useSelectableState.reducer';
import {State, Reducer} from './useSelectableState.interfaces';

interface TestData {
    id: number;
}

const reducer = reducerFactory<TestData>({accessor: x => x.id});
const getTestData = (): TestData[] => [{id: 1}, {id: 2}, {id: 3}, {id: 4}];

const getTestDataState = () =>
    reducer(getInitialState(), {
        type: 'UPDATE_DATA',
        payload: {
            data: getTestData(),
        },
    });

describe('useSelectableState.reducer', () => {
    describe('action "UPDATE_DATA"', () => {
        test('work correctly at initial state', () => {
            expect(getTestDataState()).toMatchSnapshot();
        });
        test('work correctly with initial disabled and selected items', () => {
            let state = getTestDataState();
            state = reducer(state, {type: 'SET_DISABLED_ITEMS', payload: [2]});

            expect(
                reducer(state, {
                    type: 'UPDATE_DATA',
                    payload: {
                        data: [{id: 1}, {id: 2}],
                        selectedIds: [1],
                    },
                }),
            ).toMatchSnapshot();
        });
        test('by default hold selected items in state', () => {
            let state = reducer(getTestDataState(), {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {mode: 'single', rowIdx: 0},
            });

            state = reducer(state, {
                type: 'UPDATE_DATA',
                payload: {
                    data: [{id: 5}],
                },
            });

            // order is important
            expect(state.itemsIds).toEqual(['5', '1']);
            expect(state.selectedIds).toEqual({1: 'user'});
        });

        describe('defaultSelectionAt option', () => {
            let reducerWithDefaultSelected: Reducer<TestData>;

            beforeEach(() => {
                reducerWithDefaultSelected = reducerFactory<TestData>({
                    accessor: x => x.id,
                    defaultSelectionAt: 1,
                });
            });

            test('work as expected', () => {
                const state = reducerWithDefaultSelected(getInitialState(), {
                    type: 'UPDATE_DATA',
                    payload: {data: [{id: 1}]},
                });

                expect(state.selectedIds).toEqual({1: 'auto'});
            });
            test('work as expected when state updated', () => {
                const state = reducerWithDefaultSelected(getInitialState(), {
                    type: 'UPDATE_DATA',
                    payload: {data: [{id: 1}]},
                });
                const nextState = reducerWithDefaultSelected(state, {
                    type: 'UPDATE_DATA',
                    payload: {data: [{id: 2}, {id: 1}]},
                });

                expect(nextState.selectedIds).toEqual({});
            });

            test('work as expected with before setted selected value', () => {
                let state = reducerWithDefaultSelected(getInitialState(), {
                    type: 'UPDATE_DATA',
                    payload: {data: [{id: 1}, {id: 2}], selectedIds: [2]},
                });
                state = reducerWithDefaultSelected(state, {
                    type: 'UPDATE_DATA',
                    payload: {data: [{id: 3}]},
                });

                expect(state.selectedIds).toEqual({2: 'user'});
                expect(state.itemsIds).toEqual(['3', '2']);
            });
            test('work as expected with initialSelectedIds', () => {
                const state = reducerWithDefaultSelected(getInitialState(), {
                    type: 'UPDATE_DATA',
                    payload: {data: [{id: 1}], selectedIds: [1]},
                });

                expect(state.selectedIds).toEqual({1: 'user'});
            });

            test('work as expected with items more then threshold', () => {
                const state = reducerWithDefaultSelected(getInitialState(), {
                    type: 'UPDATE_DATA',
                    payload: {data: [{id: 1}, {id: 2}]},
                });

                expect(state.selectedIds).toEqual({});
            });
        });

        describe('work correctly with already existing state data', () => {
            let state: State<TestData> = getInitialState();

            beforeEach(() => {
                state = getTestDataState();
            });

            test('when add more data with duplicate keys', () => {
                expect(
                    reducer(state, {
                        type: 'UPDATE_DATA',
                        payload: {
                            data: [{id: 5}, {id: 4}],
                        },
                    }),
                ).toMatchSnapshot();
            });
            test('work correctly with already existing state data then has selected items', () => {
                state = reducer(state, {
                    type: 'TOGGLE_ROW_SELECTION',
                    payload: {
                        mode: 'single',
                        rowIdx: 1,
                    },
                });

                const newState = reducer(state, {
                    type: 'UPDATE_DATA',
                    payload: {
                        data: [{id: 9}],
                    },
                });

                expect(newState.selectedIds).toHaveProperty('2', 'user');
            });
        });
    });

    describe('action "TOGGLE_ROW_SELECTION"', () => {
        let state: State<TestData> = getInitialState();

        beforeEach(() => {
            state = getTestDataState();
        });

        test('work correctly in multiple mode', () => {
            state = reducer(state, {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {
                    mode: 'multi',
                    rowIdx: 0,
                },
            });
            state = reducer(state, {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {
                    mode: 'multi',
                    rowIdx: 1,
                },
            });
            state = reducer(state, {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {
                    mode: 'multi',
                    rowIdx: 0,
                },
            });

            expect(state.selectedIds).toEqual({1: 'none', 2: 'user'});
        });
        test('work correctly in single mode', () => {
            state = reducer(state, {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {
                    mode: 'single',
                    rowIdx: 0,
                },
            });
            state = reducer(state, {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {
                    mode: 'single',
                    rowIdx: 0,
                },
            });

            expect(state.selectedIds).toEqual({1: 'none'});
        });
        test('work correctly in select mode', () => {
            state = reducer(state, {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {
                    mode: 'select',
                    rowIdx: 0,
                },
            });
            state = reducer(state, {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {
                    mode: 'select',
                    rowIdx: 0,
                },
            });
            expect(state.selectedIds).toEqual({1: 'user'});
        });
        test('work correctly with empty state', () => {
            state = reducer(state, {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {
                    mode: 'select',
                    rowIdx: 0,
                },
            });
            state = reducer(state, {
                type: 'TOGGLE_ROW_SELECTION',
                payload: {
                    mode: 'select',
                    rowIdx: 0,
                },
            });
            expect(state.selectedIds).toEqual({1: 'user'});
        });
    });
    describe('action "TOGGLE_ALL_ROW_SELECTION"', () => {
        let state: State<TestData> = getInitialState();

        beforeEach(() => {
            state = getTestDataState();
        });

        test('work correctly at initial state', () => {
            state = reducer(state, {
                type: 'TOGGLE_ALL_ROW_SELECTION',
                payload: true,
            });

            expect(state.selectedIds).toMatchSnapshot();
        });

        test('work correctly with selected items', () => {
            state = reducer(state, {
                type: 'TOGGLE_ALL_ROW_SELECTION',
                payload: true,
            });
            state = reducer(state, {
                type: 'TOGGLE_ALL_ROW_SELECTION',
                payload: false,
            });
            expect(state.selectedIds).toMatchSnapshot();
        });
    });

    describe('action "RESET_STATE"', () => {
        test('work correctly', () => {
            expect(
                reducer(getTestDataState(), {type: 'RESET_STATE'}),
            ).toMatchObject(getInitialState());
        });
    });

    describe('action "REMOVE_BY_ID"', () => {
        test('work correctly with disabled values and selected values', () => {
            expect(
                reducer(getTestDataState(), {
                    type: 'REMOVE_BY_ID',
                    payload: '1',
                }),
            ).toMatchSnapshot();
        });
    });
});
