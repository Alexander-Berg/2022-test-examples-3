import { EFrom } from 'mg/types/dataSource';
import { getVhAdvParams } from 'news/lib/vh/getVhAdvParams';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';

test('getVhAdvParams from', () => {
  const actualParams = [getVhAdvParams(getServerCtxStub(), { isTragic: false })];
  const expectedParams = [{ from: EFrom.NEWS }];

  expect(actualParams).toMatchObject(expectedParams);
});

test('getVhAdvParams from isTragic', () => {
  const actualParams = [getVhAdvParams(getServerCtxStub(), { isTragic: true })];
  const expectedParams = [{ from: EFrom.NEWS_TRAGIC }];

  expect(actualParams).toMatchObject(expectedParams);
});

test('getVhAdvParams scenario', () => {
  const actualParams = [getVhAdvParams(getServerCtxStub({
    specialArgs: {
      neo: {
        flags: {
          'yxneo_news_vh-adv-id': 'VS-000000-0',
        },
      },
    },
  }), { isTragic: false })];
  const expectedParams = [{
    partner_id: '000000',
    vmap_id: '0',
  }];

  expect(actualParams).toMatchObject(expectedParams);
});

test('getVhAdvParams scenario isTragic', () => {
  const actualParams = [getVhAdvParams(getServerCtxStub({
    specialArgs: {
      neo: {
        flags: {
          'yxneo_news_vh-adv-id': 'VS-000000-0',
        },
      },
    },
  }), { isTragic: true })];
  const expectedParams = [{ from: EFrom.NEWS_TRAGIC }];

  expect(actualParams).toMatchObject(expectedParams);
});
