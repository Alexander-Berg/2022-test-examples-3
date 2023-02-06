import {Map} from 'immutable';

import {ActionTypes} from '../asideConstants';
import createAsideReducer, {createInitialState} from '../asideReducer';

const asideReducer = createAsideReducer();

describe('asideReducer', () => {
  test('должен инициализировать состояние', () => {
    expect(asideReducer(undefined, {})).toEqual(createInitialState());
  });

  describe('SET_ASIDE_EXPANSION', () => {
    test('должен устанавливать переданный флаг раскрытия боковой панели', () => {
      const action = {
        type: ActionTypes.SET_ASIDE_EXPANSION,
        isExpanded: true
      };
      const state = new Map({isAsideExpanded: false});
      const expectedState = new Map({isAsideExpanded: true});

      expect(asideReducer(state, action)).toEqual(expectedState);
    });
  });
});
