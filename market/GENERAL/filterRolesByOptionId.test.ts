import { MboRole, UserCategoryRoles } from 'src/java/definitions';
import { filterRolesByOptionId } from './filterRolesByOptionId';

describe('filterRolesByOptionId', () => {
  it('works empty', () => {
    expect(filterRolesByOptionId([], '', 'set')).toEqual([]);
  });
  it('works in "set" mode', () => {
    expect(
      filterRolesByOptionId(
        [
          { userId: 1, roles: [MboRole.SUPER] } as UserCategoryRoles,
          { userId: 2, roles: [MboRole.OPERATOR] } as UserCategoryRoles,
          { userId: 3, roles: [MboRole.SUPER] } as UserCategoryRoles,
          { userId: 4, roles: [MboRole.OPERATOR] } as UserCategoryRoles,
        ],
        MboRole.SUPER,
        'set'
      )
    ).toEqual([
      { userId: 1, roles: [MboRole.SUPER] },
      { userId: 3, roles: [MboRole.SUPER] },
    ]);
  });
  it('works in "unset" mode', () => {
    expect(
      filterRolesByOptionId(
        [
          { userId: 1, roles: [MboRole.SUPER] } as UserCategoryRoles,
          { userId: 2, roles: [MboRole.OPERATOR] } as UserCategoryRoles,
          { userId: 3, roles: [MboRole.SUPER] } as UserCategoryRoles,
          { userId: 4, roles: [MboRole.OPERATOR] } as UserCategoryRoles,
        ],
        MboRole.SUPER,
        'unset'
      )
    ).toEqual([
      { userId: 2, roles: [MboRole.OPERATOR] },
      { userId: 4, roles: [MboRole.OPERATOR] },
    ]);
  });
});
