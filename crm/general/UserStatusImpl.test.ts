import { autorun } from 'mobx';
import { UserStatusStore } from './UserStatusStore';
import { UserStatusImpl } from './UserStatusImpl';

describe('UserStatusImpl', () => {
  const userStatusStore: UserStatusStore = {
    statuses: new Map(),
    syncStatuses: jest.fn(),
    getUserStatusById: jest.fn(),
    removeUserStatusById: jest.fn(),
  };

  it('has unknown status by default', () => {
    const status = new UserStatusImpl(userStatusStore, 100);

    expect(status.status).toStrictEqual(UserStatusImpl.UNKNOWN_STATUS);
  });

  it('call removeUserStatusById on unobserved', () => {
    const status = new UserStatusImpl(userStatusStore, 100);

    const disposer1 = autorun(() => {
      status.status.id;
    });

    const disposer2 = autorun(() => {
      status.status.id;
    });

    disposer1();

    expect(userStatusStore.removeUserStatusById).toBeCalledTimes(0);

    disposer2();

    expect(userStatusStore.removeUserStatusById).toBeCalledTimes(1);
    expect(userStatusStore.removeUserStatusById).toBeCalledWith(100);
  });
});
