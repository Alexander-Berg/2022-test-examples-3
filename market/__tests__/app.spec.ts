import AsyncStorage from '@react-native-community/async-storage';

import { APP_INSTALLATION_ID_STORAGE_KEY } from '@/constants/storage';
import { getAppInstallationId } from '@/nativeModules/storage/app';

jest.mock('uuid');
jest.mock('@react-native-community/async-storage');

describe('Тест получение app id', () => {
  it('В async storage нет app id', async () => {
    const id = await AsyncStorage.getItem(APP_INSTALLATION_ID_STORAGE_KEY);

    expect(id).toBeNull();
  });

  it('Сгенерировался новый app id', async () => {
    const id = await getAppInstallationId();

    expect(id).toEqual('test-uuid-v4');
  });
});
