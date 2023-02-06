import { mocked } from 'ts-jest/utils';
import {
  VirtualListDataProvider,
  VirtualListGetOptions,
  VirtualList,
  VirtualListServiceLoadInitOptions,
} from 'types/VirtualList';
import { AsyncTaskStatus } from 'types/AsyncTaskStatus';
import { waitFor } from '@testing-library/react';
import { VirtualListService } from '../VirtualListService';

jest.mock('utils/increasingTimerRetryStrategy');

const createDataProvider = (list: VirtualList) =>
  mocked({
    get: jest.fn(() => Promise.resolve(list)),
    getMeta: jest.fn(() => Promise.resolve(list.meta)),
  } as VirtualListDataProvider);

const simpleListData = {
  data: [
    { id: 2, prevId: 1, seqId: 1, etype: '1' },
    { id: 3, prevId: 2, seqId: 2, etype: '1' },
    { id: 4, prevId: 3, seqId: 3, etype: '1' },
  ],
  meta: { firstSeqId: 1, firstId: 2, lastSeqId: 3, lastId: 4 },
};

const dataProvider = createDataProvider(simpleListData);

const cleanupDataProviderMock = () => {
  dataProvider.get.mockClear();
  dataProvider.getMeta.mockClear();
};

const createInitVirtualListService = async (
  dp: VirtualListDataProvider = dataProvider,
  options: VirtualListServiceLoadInitOptions = { from: 'start' },
) => {
  const virtualListService = new VirtualListService(dp);
  virtualListService.init(options);
  await waitFor(() =>
    expect(virtualListService.loadInitTask.status).toBe(AsyncTaskStatus.Complete),
  );
  cleanupDataProviderMock();

  return virtualListService;
};

describe('VirtualListService', () => {
  beforeEach(() => {
    jest.useRealTimers();
    cleanupDataProviderMock();
  });

  describe('.loadInitTask', () => {
    it('has status complete', async () => {
      const virtualListService = new VirtualListService(dataProvider);
      virtualListService.init({ from: 'start' });
      await waitFor(() =>
        expect(virtualListService.loadInitTask.status).toBe(AsyncTaskStatus.Complete),
      );
      virtualListService.destroy();
    });

    it('has status error', async () => {
      const virtualListService = new VirtualListService({
        get: () => Promise.reject('error'),
        getMeta: () => Promise.reject('error'),
      });
      virtualListService.init({ from: 'start' });
      await waitFor(() =>
        expect(virtualListService.loadInitTask.status).toBe(AsyncTaskStatus.Error),
      );
      virtualListService.destroy();
    });
  });

  describe('.init', () => {
    describe('load from start', () => {
      it('calls provider', () => {
        const virtualListService = new VirtualListService(dataProvider);
        virtualListService.init({ from: 'start' });
        virtualListService.destroy();

        expect(dataProvider.get).toBeCalledTimes(1);
        expect(dataProvider.get).toBeCalledWith<[VirtualListGetOptions]>({
          direction: 'next',
          limit: VirtualListService.PAGE_ITEMS_LIMIT,
        });
      });
    });

    describe('load from end', () => {
      it('calls provider', () => {
        const virtualListService = new VirtualListService(dataProvider);
        virtualListService.init({ from: 'end' });
        virtualListService.destroy();

        expect(dataProvider.get).toBeCalledTimes(1);
        expect(dataProvider.get).toBeCalledWith<[VirtualListGetOptions]>({
          direction: 'previous',
          limit: VirtualListService.PAGE_ITEMS_LIMIT,
        });
      });
    });

    describe('load from center', () => {
      it('calls provider', () => {
        const virtualListService = new VirtualListService(dataProvider);
        virtualListService.init({
          from: 'center',
          exactId: 'exactId',
          getInitialTopMostItemId: () => null,
        });
        virtualListService.destroy();

        expect(dataProvider.get).toBeCalledTimes(1);
        expect(dataProvider.get).toBeCalledWith<[VirtualListGetOptions]>({
          exactId: 'exactId',
          limit: VirtualListService.PAGE_ITEMS_LIMIT,
        });
      });
    });
  });

  describe('polling meta', () => {
    describe('when not init', () => {
      it('does not poll meta', () => {
        jest.useFakeTimers('modern');

        const virtualListService = new VirtualListService(dataProvider);
        virtualListService.destroy();

        jest.advanceTimersByTime(VirtualListService.META_POLLING_INTERVAL_MS);

        expect(dataProvider.getMeta).toBeCalledTimes(0);
      });
    });

    describe('when init', () => {
      it('polls meta', () => {
        jest.useFakeTimers('modern');

        const virtualListService = new VirtualListService(dataProvider);
        virtualListService.init({ from: 'start' });
        jest.advanceTimersByTime(VirtualListService.META_POLLING_INTERVAL_MS);
        virtualListService.destroy();

        expect(dataProvider.getMeta).toBeCalledTimes(0);
      });
    });
  });

  describe('.firstItemIndex', () => {
    it('is 0 by default', () => {
      const virtualListService = new VirtualListService(dataProvider);
      expect(virtualListService.firstItemIndex).toBe(0);
    });

    it('is "fistItem.seqId" after init', async () => {
      const dataProvider = createDataProvider({
        data: [{ id: 1, prevId: 0, seqId: 2 }],
        meta: { firstSeqId: 2, firstId: 1, lastSeqId: 2, lastId: 1 },
      });
      const virtualListService = await createInitVirtualListService(dataProvider);

      expect(virtualListService.firstItemIndex).toBe(2);
      virtualListService.destroy();
    });
  });

  describe('.hasMorePrevious', () => {
    it('is false by default', () => {
      const virtualListService = new VirtualListService(dataProvider);
      expect(virtualListService.hasMorePrevious).toBe(false);
    });

    describe('when has more previous after init', () => {
      it('is true', async () => {
        const dataProvider = createDataProvider({
          data: [{ id: 2, prevId: 1, seqId: 2 }],
          meta: { firstSeqId: 0, firstId: 1, lastSeqId: 2, lastId: 2 },
        });
        const virtualListService = await createInitVirtualListService(dataProvider);

        expect(virtualListService.hasMorePrevious).toBe(true);
        virtualListService.destroy();
      });
    });

    describe('when has no more previous after init', () => {
      it('is false', async () => {
        const dataProvider = createDataProvider({
          data: [{ id: 2, prevId: 0, seqId: 0 }],
          meta: { firstSeqId: 0, firstId: 2, lastSeqId: 0, lastId: 2 },
        });
        const virtualListService = await createInitVirtualListService(dataProvider);

        expect(virtualListService.hasMorePrevious).toBe(false);
        virtualListService.destroy();
      });
    });
  });

  describe('.hasMoreNext', () => {
    it('is false by default', () => {
      const virtualListService = new VirtualListService(dataProvider);
      expect(virtualListService.hasMoreNext).toBe(false);
    });

    describe('when has more next after init', () => {
      it('is true', async () => {
        const dataProvider = createDataProvider({
          data: [{ id: 2, prevId: 1, seqId: 2 }],
          meta: { firstSeqId: 0, firstId: 2, lastSeqId: 3, lastId: 3 },
        });
        const virtualListService = await createInitVirtualListService(dataProvider);

        expect(virtualListService.hasMoreNext).toBe(true);
        virtualListService.destroy();
      });
    });

    describe('when has no more next after init', () => {
      it('is false', async () => {
        const dataProvider = createDataProvider({
          data: [{ id: 2, prevId: 0, seqId: 0 }],
          meta: { firstSeqId: 0, firstId: 2, lastSeqId: 0, lastId: 2 },
        });
        const virtualListService = await createInitVirtualListService(dataProvider);

        expect(virtualListService.hasMoreNext).toBe(false);
        virtualListService.destroy();
      });
    });
  });

  describe('.initialTopMostItemIndex', () => {
    const dataProvider = createDataProvider(simpleListData);

    it('is 0 by default', () => {
      const virtualListService = new VirtualListService(dataProvider);
      expect(virtualListService.initialTopMostItemIndex).toBe(0);
    });

    describe('when init with from = start', () => {
      it('is 0', async () => {
        const virtualListService = await createInitVirtualListService(dataProvider);

        expect(virtualListService.initialTopMostItemIndex).toBe(0);
        virtualListService.destroy();
      });
    });

    describe('when init with from = center', () => {
      it('is calculated by getInitialTopMostItemId', async () => {
        const middleItemId = simpleListData.data[1].id;

        const virtualListService = await createInitVirtualListService(dataProvider, {
          from: 'center',
          exactId: middleItemId,
          getInitialTopMostItemId: () => middleItemId,
        });

        expect(virtualListService.initialTopMostItemIndex).toBe(1);
        virtualListService.destroy();
      });
    });

    describe('when init with from = end', () => {
      it('is "initPage.length - 1"', async () => {
        const virtualListService = await createInitVirtualListService(dataProvider, {
          from: 'end',
        });

        expect(virtualListService.initialTopMostItemIndex).toEqual({ align: 'end', index: 'LAST' });
        virtualListService.destroy();
      });
    });
  });

  describe('.length', () => {
    it('is 0 by default', () => {
      const virtualListService = new VirtualListService(dataProvider);
      expect(virtualListService.length).toBe(0);
    });

    it('is has correct value', async () => {
      const virtualListService = await createInitVirtualListService();

      expect(virtualListService.length).toBe(simpleListData.data.length);

      virtualListService.destroy();
    });
  });

  describe('.getItemIdByAbsoluteIndex', () => {
    it('returns correct data', async () => {
      const virtualListService = await createInitVirtualListService();

      expect(virtualListService.getItemIdByAbsoluteIndex(1)).toBe(simpleListData.data[0].id);
      expect(virtualListService.getItemIdByAbsoluteIndex(2)).toBe(simpleListData.data[1].id);
      expect(virtualListService.getItemIdByAbsoluteIndex(3)).toBe(simpleListData.data[2].id);

      expect(() => {
        virtualListService.getItemIdByAbsoluteIndex(4);
      }).toThrowError();

      virtualListService.destroy();
    });
  });

  describe('.getItemByAbsoluteIndex', () => {
    it('returns correct data', async () => {
      dataProvider.get.mockImplementationOnce(() =>
        Promise.resolve({
          data: [simpleListData.data[1]],
          meta: simpleListData.meta,
        }),
      );
      const virtualListService = await createInitVirtualListService();

      expect(virtualListService.getItemByAbsoluteIndex(2)).toBe(simpleListData.data[1]);

      virtualListService.destroy();
    });
  });

  describe('.appendNonOrderItems', () => {
    it('appends items correct', async () => {
      dataProvider.get.mockImplementationOnce(() =>
        Promise.resolve({
          data: [simpleListData.data[1]],
          meta: simpleListData.meta,
        }),
      );
      const virtualListService = await createInitVirtualListService();

      expect(virtualListService.length).toBe(1);

      virtualListService.appendNonOrderItems([simpleListData.data[2], simpleListData.data[0]]);
      await waitFor(() => expect(virtualListService.length).toBe(3));

      expect(virtualListService.getItemIdByAbsoluteIndex(1)).toBe(simpleListData.data[0].id);
      expect(virtualListService.getItemIdByAbsoluteIndex(2)).toBe(simpleListData.data[1].id);
      expect(virtualListService.getItemIdByAbsoluteIndex(3)).toBe(simpleListData.data[2].id);

      virtualListService.destroy();
    });
  });

  describe('.updateMeta', () => {
    it('update list meta', async () => {
      dataProvider.get.mockImplementationOnce(() =>
        Promise.resolve({
          data: [],
          meta: { firstId: 0, firstSeqId: 0, lastId: 0, lastSeqId: 0 },
        }),
      );
      const virtualListService = await createInitVirtualListService();

      expect(virtualListService.hasMoreNext).toBe(false);
      virtualListService.updateMeta({ firstId: 0, firstSeqId: 0, lastId: 2, lastSeqId: 2 });

      expect(virtualListService.hasMoreNext).toBe(true);

      virtualListService.destroy();
    });
  });

  describe('when list full load', function() {
    describe('on start reached', () => {
      it('does not try load more', async () => {
        const virtualListService = await createInitVirtualListService();

        virtualListService.startReached();

        expect(dataProvider.get).not.toBeCalled();
      });
    });

    describe('on end reached', () => {
      it('does not try load more', async () => {
        const virtualListService = await createInitVirtualListService();

        virtualListService.endReached();
        virtualListService.destroy();

        expect(dataProvider.get).not.toBeCalled();
      });
    });

    describe('on meta change', () => {
      it('does not try load more', async () => {
        const virtualListService = await createInitVirtualListService();

        virtualListService.updateMeta({
          ...simpleListData.meta,
          lastSeqId: simpleListData.meta.lastSeqId + 1,
          lastId: simpleListData.meta.lastId + 1,
        });
        virtualListService.destroy();

        expect(dataProvider.get).not.toBeCalled();
      });
    });

    describe('on meta change and on end reached', () => {
      it('tries load more', async () => {
        const virtualListService = await createInitVirtualListService();

        virtualListService.endReached();
        virtualListService.updateMeta({
          ...simpleListData.meta,
          lastId: simpleListData.meta.lastId + 1,
          lastSeqId: simpleListData.meta.lastSeqId + 1,
        });
        virtualListService.destroy();

        expect(dataProvider.get).toBeCalledWith({
          direction: 'next',
          excludeFrom: true,
          fromId: simpleListData.data[simpleListData.data.length - 1].id,
          limit: VirtualListService.PAGE_ITEMS_LIMIT,
        });
      });
    });

    describe('on meta change and on start reached', () => {
      it('tries load more', async () => {
        const virtualListService = await createInitVirtualListService();

        virtualListService.startReached();
        virtualListService.updateMeta({
          firstId: simpleListData.meta.firstId - 1,
          firstSeqId: simpleListData.meta.firstSeqId - 1,
          lastId: simpleListData.meta.lastId + 1,
          lastSeqId: simpleListData.meta.lastSeqId + 1,
        });
        virtualListService.destroy();

        expect(dataProvider.get).toBeCalledWith({
          direction: 'previous',
          excludeFrom: true,
          fromId: simpleListData.data[0].id,
          limit: VirtualListService.PAGE_ITEMS_LIMIT,
        });
      });
    });
  });

  describe('when init with empty and new non empty meta come', () => {
    it('calls dataProvider.get', async () => {
      dataProvider.get.mockImplementationOnce(() =>
        Promise.resolve({
          data: [],
          meta: { firstId: 0, firstSeqId: 0, lastId: 0, lastSeqId: 0 },
        }),
      );
      const virtualListService = await createInitVirtualListService();

      virtualListService.updateMeta({ firstId: 1, firstSeqId: 1, lastId: 2, lastSeqId: 2 });
      virtualListService.destroy();

      expect(dataProvider.get).toBeCalledWith({
        direction: 'next',
        limit: VirtualListService.PAGE_ITEMS_LIMIT,
      });
    });
  });

  describe('.retryInit', () => {
    it('calls get with same options', async () => {
      const virtualListService = await createInitVirtualListService();
      virtualListService.retryInit();
      virtualListService.destroy();

      expect(dataProvider.get).toBeCalledTimes(1);
      expect(dataProvider.get).toBeCalledWith<[VirtualListGetOptions]>({
        direction: 'next',
        limit: VirtualListService.PAGE_ITEMS_LIMIT,
      });
    });
  });
});
