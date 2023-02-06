import { getEmptyRole } from './getEmptyRole';

describe('getEmptyRole', () => {
  it('works empty', () => {
    expect(getEmptyRole(undefined, undefined)).toEqual({
      categoryId: undefined,
      projects: [],
      roles: ['OPERATOR'],
      userId: undefined,
    });
  });
  it('works with data', () => {
    expect(getEmptyRole(123, 321)).toEqual({
      categoryId: 123,
      projects: [],
      roles: ['OPERATOR'],
      userId: 321,
    });
  });
});
