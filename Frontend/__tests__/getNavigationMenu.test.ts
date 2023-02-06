import { getNavigationMenu } from 'news/lib/dataSource/getNavigationMenu';
import { EOS, EBrowser } from 'neo/types/browser';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import {
  EXPECTED_NAVIGATION_MENU,
  EXPECTED_NAVIGATION_MENU_VERTICAL_ANDROID,
  EXPECTED_NAVIGATION_MENU_VERTICAL_APPSEARCH,
} from 'news/tests/expected/getNavigationMenu';

test('getNavigationMenu', () => {
  const actualNavigationMenu = [
    getNavigationMenu(getServerCtxStub({
      specialArgs: {
        news: {
          request: {
            scheme: 'https',
            hostname: 'yandex.ru',
          },
        },
      },
    })),

    getNavigationMenu(getServerCtxStub({
      specialArgs: {
        neo: {
          browserInfo: {
            os: {
              family: EOS.android,
            },
            browser: {},
          },
          isVertical: true,
        },
        news: {
          request: {
            scheme: 'https',
            hostname: 'yandex.ru',
          },
        },
      },
    })),

    getNavigationMenu(getServerCtxStub({
      specialArgs: {
        neo: {
          browserInfo: {
            os: {},
            browser: {
              name: EBrowser.yandexSearch,
            },
          },
          isVertical: true,
        },
        news: {
          request: {
            scheme: 'https',
            hostname: 'yandex.ru',
          },
        },
      },
    })),
  ];

  expect(actualNavigationMenu).toEqual([
    EXPECTED_NAVIGATION_MENU,
    EXPECTED_NAVIGATION_MENU_VERTICAL_ANDROID,
    EXPECTED_NAVIGATION_MENU_VERTICAL_APPSEARCH,
  ]);
});
