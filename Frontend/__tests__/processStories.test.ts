import { processStories } from 'news/lib/feed/processStories';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import { widget } from 'news/tests/stubs/prepareWidget';
import { expectedProcessStories } from 'news/tests/expected/processStories';

test('processStories', () => {
  const actualNavigationMenu = [processStories(getServerCtxStub({ specialArgs: {
    news: {
      request: {
        scheme: 'https',
        hostname: 'yandex.ru',
      },
    },
  },
  }), widget.stories!)];
  expect(actualNavigationMenu).toEqual([expectedProcessStories]);
});
