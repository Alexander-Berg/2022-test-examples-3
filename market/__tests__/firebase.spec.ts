import { getFCMToken, saveFCMToken } from '@/nativeModules/storage/firebase';

jest.mock('@react-native-community/async-storage');

describe('Получение и сохранение firebase токена', () => {
  it('Токен отсутствует', async () => {
    const token = await getFCMToken();

    expect(token).toBeNull();
  });

  it('Токен сохранился', async () => {
    const testToken = 'testToken';

    await saveFCMToken(testToken);

    const token = await getFCMToken();

    expect(token).toEqual(testToken);
  });
});
