import createSettingsReducer, {createInitialState} from '../settingsReducer';
import {ActionTypes} from '../settingsConstants';
import SettingsRecord from '../SettingsRecord';

const settings = createSettingsReducer();

describe('settingsReducer', () => {
  describe('createInitialState', () => {
    test('должен добавлять переданные свойства, которые не равны null', () => {
      const data = {
        hasNoNotificationsTimeRange: null,
        hasNoNotificationsDateRange: null,
        notifyAboutPlannedTodo: true,
        notifyAboutExpiredTodo: true
      };

      const settings = createInitialState(data);

      expect(settings).toEqual(
        expect.objectContaining({
          hasNoNotificationsTimeRange: false,
          hasNoNotificationsDateRange: false,
          notifyAboutPlannedTodo: true,
          notifyAboutExpiredTodo: true
        })
      );
    });
  });

  test('должен обрабатывать начальное состояние', () => {
    expect(settings(undefined, {})).toEqual(createInitialState());
  });

  describe('UPDATE_SETTINGS_SUCCESS', () => {
    test('должен обновлять настройки', () => {
      const state = new SettingsRecord({
        weekStartDay: 1
      });
      const expectedState = new SettingsRecord({
        weekStartDay: 2
      });
      const action = {
        type: ActionTypes.UPDATE_SETTINGS_SUCCESS,
        newSettings: {weekStartDay: 2}
      };
      expect(settings(state, action)).toEqual(expectedState);
    });
  });
});
