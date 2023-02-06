import {
  currentNotificationsSelector,
  readOnlySelector,
  isVideoControlEnabledSelector,
  featuresSelector,
  resourceValidationConfigSelector,
  offlineSelector
} from '../appStatusSelectors';

const state = {
  appStatus: {
    readOnly: false,
    offline: false,
    notifications: [10, 15],
    features: {feature: 1},
    resourceValidationConfig: {147: 1},
    corporateLayers: [1, 2],
    eventsToDisableAddButton: [3, 4]
  }
};

describe('appStatusSelectors', () => {
  describe('currentNotificationsSelector', () => {
    test('должен возвращать список uid текущих нотификаций', () => {
      expect(currentNotificationsSelector(state)).toEqual([10, 15]);
    });
  });

  describe('offlineSelector', () => {
    test('должен возвращать offline', () => {
      expect(offlineSelector(state)).toEqual(false);
    });
  });

  describe('resourceValidationConfigSelector', () => {
    test('должен возвращать resourceValidationConfig', () => {
      expect(resourceValidationConfigSelector(state)).toEqual({147: 1});
    });
  });

  describe('featuresSelector', () => {
    test('должен возвращать features', () => {
      expect(featuresSelector(state)).toEqual({feature: 1});
    });
  });

  describe('readOnlySelector', () => {
    test('должен возвращать текущий статус readOnly', () => {
      expect(readOnlySelector(state)).toBe(false);
    });
  });

  describe('isVideoControlEnabledSelector', () => {
    test(`должен возвращать false, если соответствующего поля нет в features`, () => {
      expect(
        isVideoControlEnabledSelector(
          {
            appStatus: {
              features: {}
            }
          },
          'tet4enko'
        )
      ).toBe(false);
    });
    test(`должен возвращать true, если в конфиге есть логин текущего пользователя`, () => {
      expect(
        isVideoControlEnabledSelector(
          {
            appStatus: {
              features: {enableVideoControl: ['tet4enko']}
            }
          },
          'tet4enko'
        )
      ).toBe(true);
    });
    test(`должен возвращать false, если в конфиге нет логина текущего пользователя`, () => {
      expect(
        isVideoControlEnabledSelector(
          {
            appStatus: {
              features: {enableVideoControl: ['tavria']}
            }
          },
          'tet4enko'
        )
      ).toBe(false);
    });
    test(`должен возвращать true, если в конфиге значение 1`, () => {
      expect(
        isVideoControlEnabledSelector(
          {
            appStatus: {
              features: {enableVideoControl: 1}
            }
          },
          'tet4enko'
        )
      ).toBe(true);
    });
    test(`должен возвращать false, если в конфиге значение 0`, () => {
      expect(
        isVideoControlEnabledSelector(
          {
            appStatus: {
              features: {enableVideoControl: 0}
            }
          },
          'tet4enko'
        )
      ).toBe(false);
    });
  });
});
