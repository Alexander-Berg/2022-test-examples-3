import {updateSettings} from 'features/settings/settingsActions';

import fieldNameToSettingName from '../utils/fieldNameToSettingName';
import {updateCreatePopupSetting} from '../eventCreatePopupActions';

jest.mock('../utils/fieldNameToSettingName');

describe('eventCreatePopupActions', () => {
  describe('updateCreatePopupSetting', () => {
    beforeEach(() => {
      fieldNameToSettingName.mockReset();
    });

    test('должен возвращать экшн обновления настроек', () => {
      const fieldName = '123';
      const formContext = '456';
      const updateSettingsActionType = updateSettings({}).type;

      expect(updateCreatePopupSetting(formContext, fieldName, true).type).toBe(
        updateSettingsActionType
      );
    });

    test('должен преобразовывать название поля в название настройки', () => {
      const fieldName = '123';
      const formContext = '456';
      const settingName = 'somePopupSetiingName';
      const settingValue = Symbol();

      fieldNameToSettingName.mockReturnValue(settingName);

      const action = updateCreatePopupSetting(formContext, fieldName, settingValue);

      expect(fieldNameToSettingName).toHaveBeenCalledTimes(1);
      expect(fieldNameToSettingName).toHaveBeenCalledWith(formContext, fieldName);
      expect(action.payload.values[settingName]).toBe(settingValue);
    });

    test('должен производить оптимистичное обновление настроек', () => {
      const fieldName = '123';
      const formContext = '456';
      const settingValue = Symbol();
      const action = updateCreatePopupSetting(formContext, fieldName, settingValue);

      expect(action.payload.options).toEqual({optimisticUpdate: true});
    });
  });
});
