import { MboRole, TaskType } from 'src/java/definitions';
import { getEmptyRole } from './getEmptyRole';
import { isOptionSetInRole } from './isOptionSetInRole';

describe('isOptionSetInRole', () => {
  it('works with empty', () => {
    expect(isOptionSetInRole(getEmptyRole(undefined, undefined), '')).toEqual(false);
  });
  it('works with projects', () => {
    const role = getEmptyRole(undefined, undefined);

    role.projects.push(TaskType.BLUE_LOGS);
    expect(isOptionSetInRole(role, TaskType.BLUE_LOGS)).toEqual(true);
  });
  it('works with roles', () => {
    const role = getEmptyRole(undefined, undefined);

    role.roles.push(MboRole.SUPER);
    expect(isOptionSetInRole(role, MboRole.SUPER)).toEqual(true);
  });
});
