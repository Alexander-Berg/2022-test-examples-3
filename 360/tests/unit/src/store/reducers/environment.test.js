import { UPDATE_WINDOW_WIDTH } from '../../../../../src/store/types';
import { DESKTOP_LAYOUT_THRESHOLD } from '../../../../../src/consts';
import environmentReducer from '../../../../../src/store/reducers/environment';

const getMockedState = ({ windowWidth = DESKTOP_LAYOUT_THRESHOLD }) => ({
    windowWidth
});

describe('store/reducers/environment', () => {
    it('should return default state by default', () => {
        expect(environmentReducer(undefined, { type: 'any' })).toEqual({});
    });

    it('UPDATE_WINDOW_WIDTH', () => {
        const state = getMockedState({});
        const newState = environmentReducer(state, { type: UPDATE_WINDOW_WIDTH, payload: DESKTOP_LAYOUT_THRESHOLD + 1 });

        expect(newState).toEqual(Object.assign({}, state, { windowWidth: DESKTOP_LAYOUT_THRESHOLD + 1 }));
        expect(newState).not.toBe(state);
    });
});
