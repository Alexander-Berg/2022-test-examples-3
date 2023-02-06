import { getSearchResponse } from 'news/lib/search/getSearchResponse';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import {
  expectedEmpty,
  expectedNoNewsSearchResponse,
  expectedTypeStory,
  expectedTypeDoc,
  expectedAggregated,
} from 'news/tests/expected/getSearchResponse';

test('getSearchResponse empty', () => {
  const request = getServerCtxStub({
    findLastItemArgs: { text_info: 'text_info_empty', next_page: 'next_page_null' },
    specialArgs: {
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
      },
    },
  });

  const actualSearchResponse = getSearchResponse(request);
  expect(actualSearchResponse).toEqual(expectedEmpty);
});

test('getSearchResponse noNewsSearchResponse', () => {
  const request = getServerCtxStub({
    findLastItemArgs: { news_search_content: 'empty', next_page: 'next_page_null' },
    specialArgs: {
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
      },
    },
  });

  const actualSearchResponse = getSearchResponse(request);
  expect(actualSearchResponse).toEqual(expectedNoNewsSearchResponse);
});

test('getSearchResponse type story', () => {
  const actualSearchResponse = getSearchResponse(getServerCtxStub({
    findLastItemArgs: { next_page: 'next_page_null' },
    specialArgs: {
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
      },
    },
  }));
  expect(actualSearchResponse).toEqual(expectedTypeStory);
});

test('getSearchResponse type doc', () => {
  const request = getServerCtxStub({
    findLastItemArgs: { news_search_content: 'news_search_content_doc', next_page: 'next_page_null' },
    specialArgs: {
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
      },
    },
  });

  const actualSearchResponse = getSearchResponse(request);
  expect(actualSearchResponse).toEqual(expectedTypeDoc);
});

test('getSearchResponse aggregated', () => {
  const request = getServerCtxStub({
    findLastItemArgs: { news_search_content: 'news_search_aggregated', next_page: 'next_page_search' },
    specialArgs: {
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
      },
    },
  });

  const actualSearchResponse = getSearchResponse(request);
  expect(actualSearchResponse).toEqual(expectedAggregated);
});
