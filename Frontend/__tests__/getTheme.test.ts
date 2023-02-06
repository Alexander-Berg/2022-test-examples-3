import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import { INewsNewsdResponseTheme } from 'news/types/apphost';
import { getTheme } from '../getTheme';

describe('getTheme', () => {
  it('Выставляет isTragic, если id совпадает с флагом трагичности', () => {
    const theme = getTheme(
      getServerCtxStub({
        additionalItemMap: {
          news_newsd_response: [
            getThemeResponse(),
          ],
        },
        specialArgs: {
          neo: {
            flags: {
              'yxneo_news_theme_tragic-ids': '555,666,777',
            },
          },
        },
      }),
    );

    expect(theme.isTragic).toBe(true);
  });

  it('Не выставляет isTragic, если id не совпадает с флагом трагичности', () => {
    const theme = getTheme(
      getServerCtxStub({
        additionalItemMap: {
          news_newsd_response: [
            getThemeResponse(),
          ],
        },
        specialArgs: {
          neo: {
            flags: {
              'yxneo_news_theme_tragic-ids': '444,999',
            },
          },
        },
      }),
    );

    expect(theme.isTragic).toBe(false);
  });
});

function getThemeResponse(data?: Partial<INewsNewsdResponseTheme>): INewsNewsdResponseTheme {
  return {
    type: 'news_newsd_response',
    handler: 'theme',
    data: {
      last_clusters: {
        stories: [],
      },
      theme_info: {
        name: '__test-name__',
        transliterated_name: '__test-tr-name__',
        id: 666,
        annotations: [{
          url: '__test-url__',
          agency: 333,
        }],
        annotations_count: 1,
        main_url: '__test-url__',
        description: '__test-url__',
        main_agency: 222,
        rubric: {
          id: 455,
          is_region: false,
        },
      },
      widgets: {},
    },
    ...data,
  };
}
