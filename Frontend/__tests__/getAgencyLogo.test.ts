import { IDoc } from 'mg/types/story';
import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getAgencyLogo } from '../getAgencyLogo';

describe('getAgencyLogo', () => {
  beforeEach(() => {
    jest.resetModules();
    jest.dontMock('mg/lib/getAgencyById');
    jest.dontMock('lib/getAgencyLogoByUrl');
  });

  it('Формирует урл лого по logo_square, если поле есть в IDoc', () => {
    const doc = {
      logo_square: 'logo_square_example',
    } as IDoc;

    const serverCtx = getServerCtxStub();
    const resultUrl = getAgencyLogo(serverCtx, doc);

    expect(resultUrl).toStrictEqual(
      {
        url: 'logo_square_example/logo-square',
      },
    );
  });

  it('Формирует урл лого по agency.logo_square, если поле есть в IAgency', async() => {
    jest.doMock('mg/lib/getAgencyById', () => ({
      __esModule: true,
      getAgencyById: () => ({
        logo_square: '__test__',
      }),
    }));

    const { getAgencyLogo } = await import('../getAgencyLogo');
    const serverCtx = getServerCtxStub();
    const resultUrl = getAgencyLogo(serverCtx, {} as IDoc);

    expect(resultUrl).toStrictEqual({
      url: 'https://avatars.mds.yandex.net/get-ynews-logo/__test__/logo-square',
    });
  });

  it('Формирует урл лого по source.host, если поле есть в IDoc', () => {
    const doc = {
      source: {
        host: '__test__',
      },
    } as IDoc;

    const serverCtx = getServerCtxStub();
    const resultUrl = getAgencyLogo(serverCtx, doc);

    expect(resultUrl).toStrictEqual({
      url: 'https://favicon.yandex.net/favicon/__test__?stub=1&size=32',
      isFavicon: true,
    });
  });

  it('Формирует урл лого getAgencyLogoByUrl в остальных случаях', async() => {
    jest.doMock('lib/getAgencyLogoByUrl', () => ({
      __esModule: true,
      getAgencyLogoByUrl: () => (
        'https://favicon.yandex.net/favicon/test.ru?stub=1&size=32'
      ),
    }));

    const { getAgencyLogo } = await import('../getAgencyLogo');
    const serverCtx = getServerCtxStub();
    const resultUrl = getAgencyLogo(serverCtx, {} as IDoc);

    expect(resultUrl).toStrictEqual({
      url: 'https://favicon.yandex.net/favicon/test.ru?stub=1&size=32',
      isFavicon: true,
    });
  });
});
