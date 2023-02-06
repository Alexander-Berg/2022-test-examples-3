import { MboRole, TaskType, UserCategoryRoles } from 'src/java/definitions';
import { toggleMultiRoles } from 'src/pages/Roles/utils/toggleMultiRoles';

describe('toggleMultiRoles', () => {
  it('works empty', () => {
    expect(toggleMultiRoles([], '', 'set')).toEqual([]);
  });
  it('works with roles in "set" mode ', () => {
    expect(
      toggleMultiRoles(
        [
          { userId: 1, roles: [MboRole.OPERATOR, MboRole.SUPER] } as UserCategoryRoles,
          { userId: 2, roles: [MboRole.OPERATOR] } as UserCategoryRoles,
          { userId: 3, roles: [MboRole.OPERATOR, MboRole.SUPER] } as UserCategoryRoles,
          { userId: 4, roles: [MboRole.OPERATOR] } as UserCategoryRoles,
        ],
        MboRole.SUPER,
        'set'
      )
    ).toEqual([
      { userId: 2, projects: undefined, roles: [MboRole.OPERATOR, MboRole.SUPER] },
      { userId: 4, projects: undefined, roles: [MboRole.OPERATOR, MboRole.SUPER] },
    ]);
  });
  it('works with roles in "unset" mode ', () => {
    expect(
      toggleMultiRoles(
        [
          { userId: 1, roles: [MboRole.OPERATOR, MboRole.SUPER] } as UserCategoryRoles,
          { userId: 2, roles: [MboRole.OPERATOR] } as UserCategoryRoles,
          { userId: 3, roles: [MboRole.OPERATOR, MboRole.SUPER] } as UserCategoryRoles,
          { userId: 4, roles: [MboRole.OPERATOR] } as UserCategoryRoles,
        ],
        MboRole.SUPER,
        'unset'
      )
    ).toEqual([
      { userId: 1, projects: undefined, roles: [MboRole.OPERATOR] },
      { userId: 3, projects: undefined, roles: [MboRole.OPERATOR] },
    ]);
  });
  it('works with projects in "set" mode ', () => {
    expect(
      toggleMultiRoles(
        [
          { userId: 1, projects: [TaskType.WHITE_LOGS] } as UserCategoryRoles,
          { userId: 2, projects: [TaskType.BLUE_LOGS] } as UserCategoryRoles,
          { userId: 3, projects: [TaskType.WHITE_LOGS] } as UserCategoryRoles,
          { userId: 4, projects: [TaskType.BLUE_LOGS] } as UserCategoryRoles,
        ],
        TaskType.BLUE_LOGS,
        'set'
      )
    ).toEqual([
      { userId: 1, roles: undefined, projects: [TaskType.WHITE_LOGS, TaskType.BLUE_LOGS] },
      { userId: 3, roles: undefined, projects: [TaskType.WHITE_LOGS, TaskType.BLUE_LOGS] },
    ]);
  });
  it('works with projects in "unset" mode ', () => {
    expect(
      toggleMultiRoles(
        [
          { userId: 1, projects: [TaskType.WHITE_LOGS] } as UserCategoryRoles,
          { userId: 2, projects: [TaskType.BLUE_LOGS] } as UserCategoryRoles,
          { userId: 3, projects: [TaskType.WHITE_LOGS] } as UserCategoryRoles,
          { userId: 4, projects: [TaskType.BLUE_LOGS] } as UserCategoryRoles,
        ],
        TaskType.WHITE_LOGS,
        'unset'
      )
    ).toEqual([
      { userId: 1, roles: undefined, projects: [] },
      { userId: 3, roles: undefined, projects: [] },
    ]);
  });
});
