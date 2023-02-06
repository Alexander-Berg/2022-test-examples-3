import AsyncStorage from '@react-native-community/async-storage';

import { UpdateSettingsData } from '@/apollo/generated/graphql';
import { SETTINGS_STORAGE_KEY, defaultSettings } from '@/constants/storage';
import {
  clearUserSettings,
  getUserSettings,
  updateUserSettings,
} from '@/nativeModules/storage/settings';

jest.mock('@react-native-community/async-storage');

const testUserUid = '12343';
const testChangeSettingsInput: UpdateSettingsData = {
  isProduction: false,
  isBarcodeScannerEnabled: false,
};

describe('Тестирование получение и сохранение настроек', () => {
  it('Настроек нет в async storage', async () => {
    const settings = await AsyncStorage.getItem(
      `${SETTINGS_STORAGE_KEY}_${testUserUid}`,
    );

    expect(settings).toBeNull();
  });

  it('Функция возвращает настройки по дефолту', async () => {
    const settings = await getUserSettings(testUserUid);

    expect(settings).toEqual(defaultSettings);
  });

  it('Настройки изменяются', async () => {
    const changedSettings = await updateUserSettings(
      testUserUid,
      testChangeSettingsInput,
    );

    expect(changedSettings).toMatchObject(testChangeSettingsInput);
  });

  it('Настройки сброшены', async () => {
    const newSettings = await clearUserSettings(testUserUid);

    expect(newSettings).toEqual(defaultSettings);

    const settings = await getUserSettings(testUserUid);

    expect(settings).toEqual(defaultSettings);
  });
});
