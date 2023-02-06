import { prepareWidget, prepareWidgetStory } from 'news/lib/dataSource/prepareWidget';
import { IServerCtx } from 'news/types/contexts/IServerCtx';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import { widget, appearedWidget } from 'news/tests/stubs/prepareWidget';
import { expectedWidget } from 'news/tests/expected/prepareWidget';
import { story, storyNoSnippetsDoc } from 'news/tests/stubs/story/story';

test('prepareWidget undefined', () => {
  const request = {} as IServerCtx;
  const actualWidget = [prepareWidget(request, {})];
  expect(actualWidget).toEqual([undefined]);
});

test('prepareWidget less than three stories', () => {
  const request = getServerCtxStub({
    specialArgs: {
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
      },
    },
  });
  const actualWidget = [prepareWidget(request, widget)];

  expect(actualWidget).toEqual([undefined]);
});

test('prepareWidget', () => {
  const request = getServerCtxStub({
    specialArgs: {
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
        rubricsInfo: {
          regions: {},
          rubrics: {
            '0': {
              alias: 'index',
              names: {
                ru: 'Главное',
                ru_genitive: '',
                ru_genitive_full: 'Новости',
                ruuk: 'Головне',
                ua_genitive: '',
                ua_genitive_full: 'Новини',
              },
              url: '/news',
            },
          },
        },
      },
    },
  });
  const actualWidget = [prepareWidget(request, appearedWidget)];
  expect(actualWidget).toEqual([expectedWidget]);
});

test('prepareWidgetStory snippets source_name', () => {
  const actualWidgetStory = [prepareWidgetStory(getServerCtxStub({}), story)];
  const expectedWidgetStory = [{
    title: 'Определена эффективность вакцины «КовиВак»',
    sourceName: 'Lenta.ru (snippets)',
    isFavoriteSource: false,
    time: '01.01.1970 в 03:00',
    url: 'https://yandex.ru/news/story/Opredelena_ehffektivnost_vakciny_KoviVak--0083b5d7d36ce7cc53f8f9d8f1c85cee?lang=ru&rubric=index&fan=1&persistent_id=145432497',
    target: '_self',
  }];

  expect(actualWidgetStory).toMatchObject(expectedWidgetStory);
});

test('prepareWidgetStory title source_name', () => {
  const actualWidgetStory = [prepareWidgetStory(getServerCtxStub({}), storyNoSnippetsDoc)];
  const expectedWidgetStory = [{
    title: 'Определена эффективность вакцины «КовиВак»',
    sourceName: 'Lenta.ru (title)',
    isFavoriteSource: false,
    time: '01.01.1970 в 03:00',
    url: 'https://yandex.ru/news/story/Opredelena_ehffektivnost_vakciny_KoviVak--0083b5d7d36ce7cc53f8f9d8f1c85cee?lang=ru&rubric=index&fan=1&persistent_id=145432497',
    target: '_self',
  }];

  expect(actualWidgetStory).toMatchObject(expectedWidgetStory);
});
