import { getNextPage } from 'news/lib/dataSource/getNextPage';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';

test('getNextPage null', () => {
  const request = getServerCtxStub({
    findLastItemArgs: {
      next_page: 'next_page_null',
    },
  });

  const actualNavigationMenu = [getNextPage(request)];
  expect(actualNavigationMenu).toEqual([null]);
});

test('getNextPage', () => {
  const request = getServerCtxStub({
    specialArgs: {
      neo: {
        isVertical: 1,
      },
      news: {
        isAppSearchHeader: 1,
      },
    },
  });

  const actualNavigationMenu = [getNextPage(request)];
  expect(actualNavigationMenu).toEqual(['/news/story/Sobyanin_obyavil?vertical=1&appsearch_header=1&ajax=1']);
});
