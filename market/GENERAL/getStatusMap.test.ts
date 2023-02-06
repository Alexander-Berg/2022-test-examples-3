import { DetachedImageStatus, OperationStatusType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { getStatusMap } from 'src/tasks/common-logs/store/images/epics/helpers/getStatusMap';

describe('getStatusMap', () => {
  it('works with empty', () => {
    expect(getStatusMap([], {})).toEqual({});
  });
  it('works with data', () => {
    expect(
      getStatusMap(['test', 'test2'], {
        status: [
          {
            id: 123,
            picture: { url: 'test', url_source: 'testSource' },
            status: { status: OperationStatusType.OK },
          },
          {
            id: 123,
            picture: { url: 'test3', url_source: 'testSource' },
            tatus: { status: OperationStatusType.OK },
          },
          {
            id: 123,
            picture: { url: 'test4', url_source: 'testSource' },
            tatus: { status: OperationStatusType.OK },
          },
        ] as DetachedImageStatus[],
      })
    ).toEqual({
      test: {
        id: 123,
        picture: {
          url: 'test',
          url_source: 'testSource',
        },
        status: {
          status: 'OK',
        },
      },
      test2: {
        id: 123,
        picture: {
          url: 'test3',
          url_source: 'testSource',
        },
        tatus: {
          status: 'OK',
        },
      },
    });
  });
});
