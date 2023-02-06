import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { UserStatusesResponseDTO } from 'types/UserStatusesResponseDTO';
import { UserStatusImpl } from './UserStatusImpl';
import { UserStatusStoreImpl } from './UserStatusStoreImpl';

const apiStatusResponse: UserStatusesResponseDTO = {
  users: [
    {
      id: 100,
      status: {
        id: 1,
        text: 'text',
        icon: 'help',
        color: 'color',
      },
    },
    {
      id: 101,
      status: {
        id: 2,
        text: 'text1',
        icon: 'help',
        color: 'color1',
      },
    },
  ],
};

const logRequestIds = jest.fn();

const server = setupServer(
  rest.get(`${window.CRM_SPACE_API_HOST}/users/status`, (req, res, ctx) => {
    logRequestIds(req.url.searchParams.getAll('ids').map(Number));
    return res(ctx.json(apiStatusResponse));
  }),
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  logRequestIds.mockClear();
});
afterAll(() => server.close());

describe('UserStatusStoreImpl', () => {
  describe('.getUserStatusById', () => {
    it('creates user status if not exits', () => {
      const userStatusStore = new UserStatusStoreImpl(UserStatusImpl);

      expect(userStatusStore.statuses.size).toBe(0);

      userStatusStore.getUserStatusById(100);

      expect(userStatusStore.statuses.size).toBe(1);
    });

    it('returns exist user status', () => {
      const userStatusStore = new UserStatusStoreImpl(UserStatusImpl);

      const status = userStatusStore.getUserStatusById(100);

      expect(userStatusStore.getUserStatusById(100)).toBe(status);
    });
  });

  describe('.removeUserStatusById', () => {
    it('removes user status', () => {
      const userStatusStore = new UserStatusStoreImpl(UserStatusImpl);

      userStatusStore.getUserStatusById(100);
      userStatusStore.removeUserStatusById(100);

      expect(userStatusStore.statuses.size).toBe(0);
    });
  });

  describe('.syncStatuses', () => {
    it('syncs users statuses', async () => {
      const userStatusStore = new UserStatusStoreImpl(UserStatusImpl);

      userStatusStore.getUserStatusById(100);
      userStatusStore.getUserStatusById(101);

      await userStatusStore.syncStatuses();

      expect(logRequestIds).toBeCalledTimes(1);
      expect(logRequestIds).toBeCalledWith([100, 101]);
      expect(userStatusStore.statuses.size).toBe(2);
      expect(userStatusStore.statuses.get(100)!.status).toStrictEqual(
        apiStatusResponse.users[0].status,
      );
      expect(userStatusStore.statuses.get(101)!.status).toStrictEqual(
        apiStatusResponse.users[1].status,
      );
    });

    describe('when no users for watch', () => {
      it('does not call api', async () => {
        const userStatusStore = new UserStatusStoreImpl(UserStatusImpl);
        await userStatusStore.syncStatuses();
        expect(logRequestIds).not.toBeCalled();
      });

      it('returns empty array', async () => {
        const userStatusStore = new UserStatusStoreImpl(UserStatusImpl);
        expect(await userStatusStore.syncStatuses()).toStrictEqual([]);
      });
    });
  });
});
