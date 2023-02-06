import * as environment from 'configs/environment';

import {getYupiShowDate, getShowAllResources, getIsSpaceshipActive} from '../yupiSelectors';

describe('yupiSelectors', () => {
  describe('getYupiShowDate', () => {
    test('должен возвращать showDate из yupi', () => {
      const showDate = 123;
      const state = {
        yupi: {
          showDate
        }
      };
      expect(getYupiShowDate(state)).toEqual(showDate);
    });
  });
  describe('getShowAllResources', () => {
    test('должен возвращать showAllResources из yupi', () => {
      const showAllResources = false;
      const state = {
        yupi: {
          showAllResources
        }
      };
      expect(getShowAllResources(state)).toEqual(showAllResources);
    });
  });
  describe('getIsSpaceshipActive', () => {
    test('должен возвращать true при нахождении на странице создания события и включенном Космолёте', () => {
      const location = {
        pathname: '/event'
      };
      expect(getIsSpaceshipActive.resultFunc(location, true)).toEqual(true);
    });
    test('должен возвращать true при нахождении на странице просмотра события и включенном Космолёте', () => {
      const location = {
        pathname: '/event/123456'
      };
      sinon.stub(environment, 'isTouch').value(false);
      expect(getIsSpaceshipActive.resultFunc(location, true)).toEqual(true);
    });
    test('должен возвращать false при нахождении не на странице события и включенном Космолёте', () => {
      const location = {
        pathname: '/'
      };
      expect(getIsSpaceshipActive.resultFunc(location, true)).toEqual(false);
    });
    test('должен возвращать false при нахождении на странице события и выключенном Космолёте', () => {
      const location = {
        pathname: '/event'
      };
      expect(getIsSpaceshipActive.resultFunc(location, false)).toEqual(false);
    });
    test('должен возвращать false при нахождении на странице события в таче', () => {
      const location = {
        pathname: '/event'
      };
      sinon.stub(environment, 'isTouch').value(true);
      expect(getIsSpaceshipActive.resultFunc(location, false)).toEqual(false);
    });
  });
});
