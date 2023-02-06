import * as environment from 'configs/environment';

import SettingsRecord from '../SettingsRecord';
import {ViewModes} from '../settingsConstants';
import {
  getCurrentUser,
  getScheduleEnabled,
  getIsSchedule,
  getIsSpaceshipActivated
} from '../settingsSelectors';

describe('settingsSelectors', () => {
  describe('getCurrentUser', () => {
    test('должен возвращать персональные настройки текущего пользователя', () => {
      const state = {
        settings: new SettingsRecord({
          email: 'test@yandex.ru',
          name: 'Test Test',
          login: 'test',
          currentOfficeId: 2,
          uid: 10,
          orgId: 123
        })
      };

      expect(getCurrentUser(state)).toEqual({
        email: 'test@yandex.ru',
        name: 'Test Test',
        login: 'test',
        officeId: 2,
        uid: 10,
        orgId: 123
      });
    });
  });
  describe('getIsSpaceshipActivated', () => {
    describe('корп', () => {
      beforeEach(() => {
        sinon.stub(environment, 'isCorp').value(true);
      });

      test('должен возвращать значение isSpaceshipActivated', () => {
        const state = {
          appStatus: {
            features: {
              forceSpaceship: false
            }
          },
          settings: new SettingsRecord({
            isSpaceshipActivated: true
          })
        };

        expect(getIsSpaceshipActivated(state)).toEqual(true);
      });

      test('должен возвращать true, если космолет у юзера не активирован, но включен глобально принудительно', () => {
        const state = {
          appStatus: {
            features: {
              forceSpaceship: true
            }
          },
          settings: new SettingsRecord({
            isSpaceshipActivated: false
          })
        };

        expect(getIsSpaceshipActivated(state)).toEqual(true);
      });

      test('должен возвращать true, если космолет у юзера не активирован, не включен глобально, но сотрудник "новый"', () => {
        const state = {
          appStatus: {
            features: {
              forceSpaceship: false
            }
          },
          settings: new SettingsRecord({
            isSpaceshipActivated: false,
            hiredAt: '2022-06-02'
          })
        };

        expect(getIsSpaceshipActivated(state)).toEqual(true);
      });

      // eslint-disable-next-line max-len
      test('должен возвращать false, если космолет у юзера не активирован, не включен глобально, и сотрудник не "новый"', () => {
        const state = {
          appStatus: {
            features: {
              forceSpaceship: false
            }
          },
          settings: new SettingsRecord({
            isSpaceshipActivated: false,
            hiredAt: '2022-05-02'
          })
        };

        expect(getIsSpaceshipActivated(state)).toEqual(false);
      });
    });

    describe('паблик', () => {
      beforeEach(() => {
        sinon.stub(environment, 'isCorp').value(false);
      });

      test('должен возвращать значение isSpaceshipActivated', () => {
        const state = {
          appStatus: {
            features: {
              forceSpaceship: false
            }
          },
          settings: new SettingsRecord({
            isSpaceshipActivated: true
          })
        };

        expect(getIsSpaceshipActivated(state)).toEqual(true);
      });
    });
  });
  describe('getScheduleEnabled', () => {
    describe('desktop', () => {
      test('должен возвращать true, если в настройках включено расписание по дефолту', () => {
        const state = {
          settings: new SettingsRecord({
            viewMode: ViewModes.SCHEDULE
          })
        };

        expect(getScheduleEnabled(state)).toEqual(true);
      });

      test('должен возвращать false, если в настройках не включено расписание по дефолту', () => {
        const state = {
          settings: new SettingsRecord({
            viewMode: 'ololo'
          })
        };

        expect(getScheduleEnabled(state)).toEqual(false);
      });
    });

    describe('touch', () => {
      beforeEach(() => {
        sinon.stub(environment, 'isTouch').value(true);
      });
      test('должен возвращать true, если в настройках включено расписание по дефолту', () => {
        const state = {
          settings: new SettingsRecord({
            touchViewMode: ViewModes.SCHEDULE
          })
        };

        expect(getScheduleEnabled(state)).toEqual(true);
      });

      test('должен возвращать false, если в настройках не включено расписание по дефолту', () => {
        const state = {
          settings: new SettingsRecord({
            touchViewMode: 'ololo'
          })
        };

        expect(getScheduleEnabled(state)).toEqual(false);
      });
    });

    test('должен возвращать false, если в настройках нет данных о расписании', () => {
      const state = {
        settings: new SettingsRecord({})
      };

      expect(getScheduleEnabled(state)).toEqual(false);
    });
  });
  describe('getIsSchedule', () => {
    test('должен возвращать true, если на странице с расписанием', () => {
      const state = {
        router: {
          location: {
            pathname: '/schedule'
          }
        }
      };

      expect(getIsSchedule(state)).toEqual(true);
    });
    test('должен возвращать true, если на странице с расписанием другого пользователя', () => {
      const state = {
        router: {
          location: {
            pathname: '/schedule/rideorgtfo'
          }
        }
      };

      expect(getIsSchedule(state)).toEqual(true);
    });
    test('должен возвращать false, если не на странице с расписанием', () => {
      const state = {
        router: {
          location: {
            pathname: '/day'
          }
        }
      };

      expect(getIsSchedule(state)).toEqual(false);
    });
  });
});
