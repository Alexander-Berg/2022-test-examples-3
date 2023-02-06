import { URL } from 'url';
import { EService } from 'neo/types/EService';
import { IServerCtx } from 'news/types/contexts/IServerCtx';
import { getMeta } from 'news/lib/search/getMeta';
import { serverCtxStub as serverCtxStubNeo } from 'neo/tests/stubs/contexts';
import {
  expectedNonEmpty,
  expectedEmptyResult,
  expectedEmptyQuery,
} from 'news/tests/expected/getMeta';

function getServerCtxStub(args: { query?: string }) {
  return {
    neo: {
      ...serverCtxStubNeo.neo,
      service: EService.NEWS,
      url: new URL(`https://yandex.ru/news/search?text=${args.query ?? ''}`),
    },
    news: {
      header: {
        query: args.query,
      },
    },
  } as IServerCtx;
}

test('getMeta should return object with NonEmpty title', () => {
  const serverCtx = getServerCtxStub({ query: 'бобр' });
  const isEmptyResult = false;

  const actualMeta = [getMeta(serverCtx, isEmptyResult)];
  expect(actualMeta).toEqual([expectedNonEmpty]);
});

test('getMeta should return object with EmptyResult title', () => {
  const serverCtx = getServerCtxStub({ query: 'abraqadabradabrafoo' });
  const isEmptyResult = true;

  const actualMeta = [getMeta(serverCtx, isEmptyResult)];
  expect(actualMeta).toEqual([expectedEmptyResult]);
});

test('getMeta should return object with EmptyQuery title', () => {
  const serverCtx = getServerCtxStub({});
  const isEmptyResult = false;

  const actualMeta = [getMeta(serverCtx, isEmptyResult)];
  expect(actualMeta).toEqual([expectedEmptyQuery]);
});
