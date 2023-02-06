import { renderHook } from '@testing-library/react-hooks';
import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { useBunker } from './useBunker';

const nodePaths = [
  'components/grid/all-grids-in-lift',
  'components/grid/-v0-blocks-liftnextperiod-tablewithfilters',
  'components/grid/aaa',
];

const server = setupServer(
  rest.get(`/v0/bunker/cat`, (req, res, ctx) => {
    const node = req.url.searchParams.get('node');

    if (node === 'components/grid/all-grids-in-lift') {
      return res(ctx.status(200), ctx.json({ theme: 'clean', field1: 1 }));
    }

    if (node === 'components/grid/-v0-blocks-liftnextperiod-tablewithfilters') {
      return res(ctx.status(200), ctx.json({ theme: 'default', field1: 2 }));
    }

    if (node === 'components/grid/aaa') {
      return res(ctx.status(200), ctx.json({ theme: 'neo', field2: 2 }));
    }
  }),
);

jest.mock('./bunkersNodeKeys', () => {
  return {
    bunkersNodeKeys: new Set(['components/grid/all-grids-in-lift', 'components/grid/aaa']),
  };
});

describe('useBunker', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  it('receives correct data', async () => {
    const { result, waitForNextUpdate } = renderHook(() => useBunker(nodePaths));

    await waitForNextUpdate();

    expect(result.current).toEqual({
      data: { theme: 'neo', field1: 1, field2: 2 },
      isFetched: true,
    });
  });

  it('does not fail on error', async () => {
    const server = setupServer(
      rest.get(`/v0/bunker/cat`, (req, res, ctx) => {
        return res(ctx.status(500));
      }),
    );

    server.listen();

    const { result, waitForNextUpdate } = renderHook(() => useBunker(nodePaths));

    await waitForNextUpdate();

    expect(result.current).toEqual({
      data: undefined,
      isFetched: true,
    });

    server.close();
  });

  it('returns correct data for empty nodePaths', async () => {
    const { result } = renderHook(() => useBunker([]));

    expect(result.current).toEqual({
      data: undefined,
      isFetched: true,
    });

    server.close();
  });
});
