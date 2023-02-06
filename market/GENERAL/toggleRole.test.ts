import { MboRole, TaskType } from 'src/java/definitions';
import { getEmptyRole } from './getEmptyRole';
import { toggleRole } from './toggleRole';

describe('toggleRole', () => {
  it('works with MboRole.SUPER', () => {
    let role = getEmptyRole(undefined, undefined);

    expect(role.projects).toEqual([]);
    expect(role.roles).toEqual([MboRole.OPERATOR]);

    role = toggleRole(role, MboRole.SUPER);

    expect(role.projects).toEqual([]);
    expect(role.roles).toEqual([MboRole.OPERATOR, MboRole.SUPER]);

    role = toggleRole(role, MboRole.SUPER);

    expect(role.projects).toEqual([]);
    expect(role.roles).toEqual([MboRole.OPERATOR]);
  });
  it('works with TaskType', () => {
    let role = getEmptyRole(undefined, undefined);

    expect(role.projects).toEqual([]);
    expect(role.roles).toEqual([MboRole.OPERATOR]);

    role = toggleRole(role, TaskType.BLUE_LOGS);

    expect(role.projects).toEqual([TaskType.BLUE_LOGS]);
    expect(role.roles).toEqual([MboRole.OPERATOR]);

    role = toggleRole(role, TaskType.BLUE_LOGS);

    expect(role.projects).toEqual([]);
    expect(role.roles).toEqual([MboRole.OPERATOR]);
  });
});
