import Bluebird from 'bluebird';
import { PageableDTO } from 'types/pagination/PageableDTO';
import { PageableItemDTO } from 'types/pagination/PageableItemDTO';
import { runPendingPromises } from 'utils/runPendingPromises';
import { PaginationLoaderByCallback } from './PaginationLoaderByCallback';

describe('PaginationLoaderByCallback', () => {
  const page1 = [{ id: '1' }];
  const page2 = [{ id: '2' }];
  const handleLoadMock = jest.fn<Bluebird<PageableDTO<PageableItemDTO>>, []>();
  const handleLoadItemsMock = jest.fn();

  beforeEach(() => {
    handleLoadMock.mockClear();
    handleLoadMock.mockReset();
    handleLoadItemsMock.mockClear();
  });

  it('does not load data on create', () => {
    handleLoadMock.mockReturnValueOnce(
      Bluebird.resolve({ data: page1, pagination: { current: 'page1', next: 'page2' } }),
    );
    const _paginationLoaderByCallback = new PaginationLoaderByCallback(
      handleLoadMock,
      handleLoadItemsMock,
    );

    expect(handleLoadMock).toBeCalledTimes(0);
    expect(handleLoadItemsMock).toBeCalledTimes(0);
  });

  it('loads data on load', async () => {
    handleLoadMock.mockReturnValueOnce(
      Bluebird.resolve({ data: page1, pagination: { current: 'page1', next: 'page2' } }),
    );
    const paginationLoaderByCallback = new PaginationLoaderByCallback(
      handleLoadMock,
      handleLoadItemsMock,
    );

    await paginationLoaderByCallback.load();

    expect(handleLoadMock).toBeCalledWith(undefined);
    expect(handleLoadItemsMock).toBeCalledWith(page1);
  });

  it('loads next page', async () => {
    handleLoadMock
      .mockReturnValueOnce(
        Bluebird.resolve({ data: page1, pagination: { current: 'page1', next: 'page2' } }),
      )
      .mockReturnValueOnce(Bluebird.resolve({ data: page2, pagination: { current: 'page2' } }));
    const paginationLoaderByCallback = new PaginationLoaderByCallback(
      handleLoadMock,
      handleLoadItemsMock,
    );

    await paginationLoaderByCallback.load();
    await paginationLoaderByCallback.load();

    expect(handleLoadMock).toBeCalledWith('page2');
    expect(handleLoadItemsMock).toBeCalledWith(page2);
  });

  describe('if next page not exist', () => {
    it('throws error', async () => {
      handleLoadMock.mockReturnValueOnce(
        Bluebird.resolve({ data: page1, pagination: { current: 'page1' } }),
      );
      const paginationLoaderByCallback = new PaginationLoaderByCallback(
        handleLoadMock,
        handleLoadItemsMock,
      );

      await paginationLoaderByCallback.load();
      try {
        await paginationLoaderByCallback.load();
      } catch (e) {
        expect((e as Error).message).toMatch(/load data has no next url/);
      }
    });
  });

  it('supports destroy', async () => {
    handleLoadMock.mockReturnValueOnce(
      Bluebird.resolve({ data: page1, pagination: { current: 'page1', next: 'page2' } }),
    );
    const paginationLoaderByCallback = new PaginationLoaderByCallback(
      handleLoadMock,
      handleLoadItemsMock,
    );

    paginationLoaderByCallback.load();
    paginationLoaderByCallback.destroy();

    await runPendingPromises();

    expect(handleLoadMock).toBeCalledTimes(1);
    expect(handleLoadItemsMock).toBeCalledTimes(0);
  });
});
