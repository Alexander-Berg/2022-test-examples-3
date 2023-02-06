import SettingsRecord from 'features/settings/SettingsRecord';

import fieldNameToSettingName from '../utils/fieldNameToSettingName';
import {CREATE_POPUP_CONFIGURABLE_FIELDS, FORM_CONTEXTS} from '../eventCreatePopupConstants';
import {getPopupFieldsSettings} from '../eventCreatePopupSelectors';

describe('eventCreatePopupSelectors', () => {
  describe('getPopupFieldsSettings', () => {
    test('должен строить объект с настройками показа полей попапа', () => {
      const formContext = FORM_CONTEXTS.invite;
      const fieldSettings = CREATE_POPUP_CONFIGURABLE_FIELDS.reduce((fieldsMap, fieldName) => {
        fieldsMap[fieldName] = Boolean(Math.round(Math.random()));

        return fieldsMap;
      }, {});
      const settings = new SettingsRecord(
        Object.keys(fieldSettings).reduce((settingsMap, fieldName) => {
          settingsMap[fieldNameToSettingName(formContext, fieldName)] = fieldSettings[fieldName];

          return settingsMap;
        }, {})
      );

      expect(getPopupFieldsSettings.resultFunc(settings, formContext)).toEqual(fieldSettings);
    });
  });
});
