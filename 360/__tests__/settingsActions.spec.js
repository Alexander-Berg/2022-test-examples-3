import createActionMetaInfo from 'middlewares/offlineMiddleware/utils/createActionMetaInfo';

import {ActionTypes} from '../settingsConstants';
import {
  updateSettings,
  updateSettingsNetwork,
  updateSettingsOffline,
  updateSettingsSuccess
} from '../settingsActions';

describe('settingsActions', () => {
  describe('updateSettings', () => {
    test('должен вернуть экшен UPDATE_SETTINGS', () => {
      const params = {
        values: {weekStartDay: 1},
        options: {notify: true},
        resolveForm: () => {},
        rejectForm: () => {}
      };

      expect(updateSettings(params)).toEqual({
        type: ActionTypes.UPDATE_SETTINGS,
        payload: params,
        meta: createActionMetaInfo({
          network: updateSettingsNetwork(params),
          rollback: updateSettingsOffline({resolveForm: params.resolveForm})
        })
      });
    });
  });

  describe('updateSettingsSuccess', () => {
    test('должен кинуть экшен UPDATE_SETTINGS_SUCCESS', () => {
      const params = {
        oldSettings: {weekStartDay: 0},
        newSettings: {weekStartDay: 1}
      };

      expect(updateSettingsSuccess(params)).toEqual({
        type: ActionTypes.UPDATE_SETTINGS_SUCCESS,
        oldSettings: params.oldSettings,
        newSettings: params.newSettings
      });
    });
  });
});
