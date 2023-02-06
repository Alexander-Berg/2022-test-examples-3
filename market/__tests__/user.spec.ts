import AsyncStorage from '@react-native-community/async-storage';

import { User } from '@/apollo/generated/graphql';
import { USER_STORAGE_KEY } from '@/constants/storage';
import { clearUser, getUser, saveUser } from '@/nativeModules/storage/user';

jest.mock('@react-native-community/async-storage');

const testUser: User = {
  accessToken: 'testAccessToken',
  displayName: 'Test',
};

describe('Тест на работу с данными пользователя в async storage', () => {
  it('В async storage нет данных о пользователе', async () => {
    const data = await AsyncStorage.getItem(USER_STORAGE_KEY);

    expect(data).toBeNull();
  });

  it('Нет данных о пользователе', async () => {
    const user = await getUser();

    expect(user).toBeNull();
  });

  it('Пользовательские данные сохранились', async () => {
    let user = await saveUser(testUser);

    expect(user).toBeTruthy();
    expect(user).toEqual(testUser);

    user = await getUser();

    expect(user).toBeTruthy();
    expect(user).toEqual(testUser);
  });

  it('Данные пользователя удалены', async () => {
    const result = await clearUser();

    expect(result).toEqual(true);

    const user = await getUser();

    expect(user).toBeNull();
  });
});
