import { UserRole } from 'src/constants';
import { User } from 'src/rest/definitions';
import canShow from './canShow';

const user: User = {
  id: 1,
  login: 'some-login',
  roles: [],
};

describe('canShow', () => {
  it('should return true', () => {
    expect(canShow(user)).toEqual(true);
    expect(canShow({ ...user, roles: [UserRole.ACCEPTOR] })).toEqual(true);
    expect(canShow({ ...user, roles: [UserRole.ACCEPTOR] }, [UserRole.ACCEPTOR])).toEqual(true);
  });

  it('should return false', () => {
    expect(canShow()).toEqual(false);
    expect(canShow(undefined, [UserRole.ACCEPTOR])).toEqual(false);
    expect(canShow(user, [UserRole.ACCEPTOR])).toEqual(false);
    expect(canShow({ ...user, roles: [UserRole.ADMIN] }, [UserRole.ACCEPTOR])).toEqual(false);
  });
});
