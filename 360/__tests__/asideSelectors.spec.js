import {Map} from 'immutable';

import {getIsAsideExpanded} from '../asideSelectors';

describe('asideSelectors', () => {
  describe('getIsAsideExpanded', () => {
    test('должен вернуть флаг раскрытия боковой панели', () => {
      const state = {
        aside: new Map({
          isAsideExpanded: false
        })
      };

      expect(getIsAsideExpanded(state)).toEqual(false);
    });
  });
});
