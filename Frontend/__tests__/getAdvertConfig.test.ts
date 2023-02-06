import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getAdvertConfig } from '../getAdvertConfig';

describe('getAdvertConfig', () => {
  beforeEach(() => {
    jest.resetModules();
  });

  it('Должен возвращать конфиг', async() => {
    jest.doMock('sport/configs/advert', () => ({
      __esModule: true,
      getDefaultConfig: () => ({
        phone: { main: { foo: 'bar' } },
      }),
    }));

    const { getAdvertConfig } = await import('../getAdvertConfig');
    const serverCtx = getServerCtxStub();
    const config = getAdvertConfig(serverCtx);
    expect(config).toEqual({ foo: 'bar' });
  });

  it('Должен кидать ошибку, если не удалось получить конфиг', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: { neo: { page: undefined } },
    });

    expect(
      () => getAdvertConfig(serverCtx),
    ).toThrowError(/Неверно заданы id рекламы/);
  });

  it('Должен доопределять конфиг из значений флага yxneo_sport_phone_main_adv-cfg', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxneo_sport_phone_main_adv-cfg': 'main:111,top:N222',
          },
        },
      },
    });

    const config = getAdvertConfig(serverCtx);

    expect(config.main).toBe('111');
    expect(config.top).toBe('N222');
  });

  it('Должен отключать рекламу для пользователей yandex plus', async() => {
    jest.doMock('mg/lib/advert', () => ({
      __esModule: true,
      isAdvertDisabledForPlusUsers: () => true,
    }));

    jest.doMock('sport/configs/advert', () => ({
      __esModule: true,
      getDefaultConfig: () => ({
        phone: { main: { foo: 'bar', some: 'test' } },
      }),
    }));

    const { getAdvertConfig } = await import('../getAdvertConfig');
    const serverCtx = getServerCtxStub();
    const config = getAdvertConfig(serverCtx);

    expect(config).toEqual({ foo: null, some: null });
  });
});
