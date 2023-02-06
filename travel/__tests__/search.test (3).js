import {sortSegments} from '../../lib/sort/sortSegments';
import {search, defaultState} from '../search';

import {setSorting, setQuerying, setSearchContext} from '../../actions/search/';

jest.mock('../../lib/segments/updateSegments', () => () => []);
jest.mock('../../lib/sort/sortSegments');

// Исходные данные
const state = {...defaultState};
const sortSegmentsResult = {vector: []};

sortSegments.mockImplementation(() => sortSegmentsResult);

describe('search reducer', () => {
    it('should return current state for any unknown action', () => {
        const action = {
            type: Symbol('SOME_UNKNOWN_ACTION'),
            payload: 'Unknown action payload',
        };

        const newState = search(state, action);

        expect(newState).toBe(state);
    });

    it('should return default state for unknown action if state is not specified', () => {
        const action = {
            type: Symbol('SOME_UNKNOWN_ACTION'),
            payload: 'Unknown action payload',
        };

        const newState = search(undefined, action);

        expect(newState).toEqual(defaultState);
    });

    describe('handling SET_SEARCH_CONTEXT action', () => {
        it('should set new search context', () => {
            const newContext = {};
            const action = setSearchContext(newContext);
            const newState = search(state, action);

            expect(newState.context).toBe(newContext);
        });

        it('should set `queryingPrices` to false', () => {
            const oldState = {...state, queryingPrices: true};
            const newContext = {};
            const action = setSearchContext(newContext);
            const newState = search(oldState, action);

            expect(newState.queryingPrices).toBe(false);
        });
    });

    describe('handling SET_SORTING action', () => {
        it('should sort segments', () => {
            const newSort = {};
            const action = setSorting(newSort);
            const newState = search(state, action);

            expect(newState.sortMapping).toBe(sortSegmentsResult);
        });
    });

    describe('handling SET_QUERYING action', () => {
        it('should update querying state', () => {
            const fullState = {search: state, environment: {}};
            const plane = setQuerying({plane: true}, fullState);
            const stateWithPlanes = search(state, plane);
            const train = setQuerying({train: true, plane: false}, fullState);
            const stateWithTrain = search(stateWithPlanes, train);
            const bus = setQuerying(
                {train: false, plane: false, bus: true},
                fullState,
            );
            const stateWithBus = search(stateWithTrain, bus);

            expect(stateWithPlanes.querying).toEqual({
                plane: true,
                train: false,
                bus: false,
                transferAll: false,
                transferBus: false,
                transferPlane: false,
                transferSuburban: false,
                transferTrain: false,
            });
            expect(stateWithTrain.querying).toEqual({
                plane: false,
                train: true,
                bus: false,
                transferAll: false,
                transferBus: false,
                transferPlane: false,
                transferSuburban: false,
                transferTrain: false,
            });
            expect(stateWithBus.querying).toEqual({
                plane: false,
                train: false,
                bus: true,
                transferAll: false,
                transferBus: false,
                transferPlane: false,
                transferSuburban: false,
                transferTrain: false,
            });
        });
    });
});
