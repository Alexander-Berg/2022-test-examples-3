import {ActionTypes} from '../asideConstants';
import {toggleAsideExpansion, setAsideExpansion} from '../asideActions';

describe('asideActions', () => {
  describe('toggleAsideExpansion', () => {
    test('должен вернуть action TOGGLE_ASIDE_EXPANSION', () => {
      expect(toggleAsideExpansion()).toEqual({
        type: ActionTypes.TOGGLE_ASIDE_EXPANSION
      });
    });
  });

  describe('setAsideExpansion', () => {
    test('должен вернуть action SET_ASIDE_EXPANSION', () => {
      expect(setAsideExpansion(true)).toEqual({
        type: ActionTypes.SET_ASIDE_EXPANSION,
        isExpanded: true
      });
    });
  });
});
