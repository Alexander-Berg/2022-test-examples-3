import { getRegionAreas } from 'news/lib/dataSource/getRegionAreas';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';

test('getRegionAreas', () => {
  const serverCtx = getServerCtxStub({
    specialArgs: {
      neo: {
        timestamp: Number(new Date('2020-11-16T15:26:28')),
        tld: 'ru',
      },
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
      },
    },
  });
  const expected = [
    {
      alias: 'russia',
      api: 'https://news.yandex.ru/api/v2/regions?alias=russia&sign=a5a2d390193d748d587310d912c2ac64',
      title: 'Россия',
    },
    {
      alias: 'ukraine',
      api: 'https://news.yandex.ru/api/v2/regions?alias=ukraine&sign=91194b0922f9b4e846f3251e99c2c24d',
      title: 'Украина',
    },
    {
      alias: 'abroad',
      api: 'https://news.yandex.ru/api/v2/regions?alias=abroad&sign=7186a731ab6ab220e32e82f81cf77c59',
      title: 'Зарубежные новости',
    },
  ];

  expect(getRegionAreas(serverCtx)).toEqual(expected);
});
