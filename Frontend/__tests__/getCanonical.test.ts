import { URL } from 'url';
import { getCanonical } from 'news/lib/getCanonical';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';

test('getCanonical main', () => {
  const serverCtx = getServerCtxStub({
    specialArgs: {
      neo: {
        url: new URL('https://yandex.ru/news?utm_source=yxnews'),
      },
    },
  });

  const actualLink = [getCanonical(serverCtx)];
  const expectedLink = ['https://yandex.ru/news'];

  expect(actualLink).toMatchObject(expectedLink);
});

test('getCanonical story', () => {
  const serverCtx = getServerCtxStub({
    specialArgs: {
      neo: {
        url: new URL('https://yandex.ru/news/story/Vrio_glavy_KHabarovskogo_kraya_predstavili_pravitelstvu_regiona--66a38840dd5d54624dac566d2983e16b?lang=ru&rubric=index&stid=DxVZVB0rApqv2l5CAEy9&t=1595333792&tt=true&persistent_id=106887185'),
      },
    },
  });

  const actualLink = [getCanonical(serverCtx)];
  const expectedLink = ['https://yandex.ru/news/story/Vrio_glavy_KHabarovskogo_kraya_predstavili_pravitelstvu_regiona--66a38840dd5d54624dac566d2983e16b?persistent_id=106887185'];

  expect(actualLink).toMatchObject(expectedLink);
});

test('getCanonical search', () => {
  const serverCtx = getServerCtxStub({
    specialArgs: {
      neo: {
        url: new URL('https://yandex.ru/news/search?text=хабаровский+край&p=3&utm_source=yxnews'),
      },
    },
  });

  const actualLink = [getCanonical(serverCtx)];
  const expectedLink = ['https://yandex.ru/news/search?text=%D1%85%D0%B0%D0%B1%D0%B0%D1%80%D0%BE%D0%B2%D1%81%D0%BA%D0%B8%D0%B9+%D0%BA%D1%80%D0%B0%D0%B9&p=3'];

  expect(actualLink).toMatchObject(expectedLink);
});
