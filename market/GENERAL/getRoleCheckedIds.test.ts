import { MboRole, TaskType } from 'src/java/definitions';
import { getEmptyRole } from 'src/pages/Roles/utils/getEmptyRole';
import { getRoleCheckedIds } from 'src/pages/Roles/utils/getRoleCheckedIds';

describe('getRoleCheckedIds', () => {
  it('works', () => {
    expect(getRoleCheckedIds(getEmptyRole(undefined, undefined))).toEqual([]);
    const role = getEmptyRole(undefined, undefined);
    role.projects.push(TaskType.BLUE_LOGS);
    role.roles.push(MboRole.SUPER);
    expect(getRoleCheckedIds(role)).toEqual([TaskType.BLUE_LOGS, MboRole.SUPER]);
  });
});
