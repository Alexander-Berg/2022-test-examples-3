import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { runPendingPromises } from 'utils/runPendingPromises';
import { PageableDTO } from 'types/pagination/PageableDTO';
import { PageableItemDTO } from 'types/pagination/PageableItemDTO';
import { GridServiceByUrl } from './GridService';
import { SortOrder, WithGridMeta } from './types/GridMeta';

const urlSpy = jest.fn((_url: URL) => {});

const server = setupServer(
  rest.get(`${window.CRM_SPACE_API_HOST}/grid`, (req, res, ctx) => {
    urlSpy(req.url);

    const data: WithGridMeta & PageableDTO<PageableItemDTO> = {
      meta: { fieldsVisibility: [], sort: [], fields: [] },
      data: [{ id: '1' }],
      pagination: {
        current: 'page1',
      },
    };

    return res(ctx.json(data));
  }),
);

beforeAll(() => server.listen());
beforeEach(() => {
  urlSpy.mockClear();
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const RUN_PENDING_PROMISES_DELAY = 10;

describe('GridServiceByUrl', () => {
  it('loads first page on init', async () => {
    const gridService = new GridServiceByUrl('/grid');

    await runPendingPromises(RUN_PENDING_PROMISES_DELAY);

    expect(gridService.store.getItemById('1')).toStrictEqual({ id: '1' });
    expect(gridService.store.getOrderIds()).toStrictEqual(['1']);
  });

  it('reloads data', async () => {
    const gridService = new GridServiceByUrl('/grid');

    await runPendingPromises(RUN_PENDING_PROMISES_DELAY);

    server.use(
      rest.get(`${window.CRM_SPACE_API_HOST}/grid`, (req, res, ctx) => {
        const data: WithGridMeta & PageableDTO<PageableItemDTO> = {
          meta: { fieldsVisibility: [], sort: [], fields: [] },
          data: [{ id: '2' }],
          pagination: {
            current: 'page1',
          },
        };

        return res(ctx.json(data));
      }),
    );

    gridService.reload();
    await runPendingPromises(RUN_PENDING_PROMISES_DELAY);

    expect(gridService.store.getItemById('2')).toStrictEqual({ id: '2' });
    expect(gridService.store.getOrderIds()).toStrictEqual(['2']);
  });

  it('reloads on sort change', async () => {
    const gridService = new GridServiceByUrl('/grid');

    await runPendingPromises(RUN_PENDING_PROMISES_DELAY);
    urlSpy.mockClear();

    gridService.sortStore.addItemById('name', { id: 'name', order: SortOrder.Asc });

    await runPendingPromises(RUN_PENDING_PROMISES_DELAY);

    expect(urlSpy).toBeCalledTimes(1);
    expect(urlSpy.mock.calls[0][0].searchParams.get('sort')).toBe(
      JSON.stringify([{ id: 'name', order: 'asc' }]),
    );
  });

  it('reloads on fields visibility change', async () => {
    const gridService = new GridServiceByUrl('/grid');

    await runPendingPromises(RUN_PENDING_PROMISES_DELAY);
    urlSpy.mockClear();

    gridService.fieldsVisibilityStore.addItemById('1', { id: '1' });

    await runPendingPromises(RUN_PENDING_PROMISES_DELAY);

    expect(urlSpy).toBeCalledTimes(1);
    expect(urlSpy.mock.calls[0][0].searchParams.get('fieldsVisibility')).toBe(
      JSON.stringify(['1']),
    );
  });

  it('supports destroy', async () => {
    const gridService = new GridServiceByUrl('/grid');

    await runPendingPromises(RUN_PENDING_PROMISES_DELAY);
    urlSpy.mockClear();

    gridService.destroy();

    gridService.fieldsVisibilityStore.addItemById('1', { id: '1' });
    gridService.sortStore.addItemById('name', { id: 'name', order: SortOrder.Asc });

    await runPendingPromises(RUN_PENDING_PROMISES_DELAY);

    expect(urlSpy).toBeCalledTimes(0);
  });
});
