import { getBans } from 'news/lib/search/getBans';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import {
  expectedFull,
  expectedAggregated,
} from 'news/tests/expected/getBans';

test('getBans should remove rurkn and ruoblivion, when merged exists', () => {
  const request = getServerCtxStub({
    findLastItemArgs: { saas_banfilter: 'saas_banfilter_full' },
  });

  const actualNavigationMenu = [getBans(request)];
  expect(actualNavigationMenu).toEqual([expectedFull]);
});

test('getBans should aggregate response and remove duplicates', () => {
  const request = getServerCtxStub({
    findLastItemArgs: { saas_banfilter: 'saas_banfilter_aggregated' },
  });

  const actualNavigationMenu = [getBans(request)];
  expect(actualNavigationMenu).toEqual([expectedAggregated]);
});
