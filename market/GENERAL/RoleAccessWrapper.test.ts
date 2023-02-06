import { getAllAbsent, hasAnyOf } from '.';

describe('Role array intersection test', () => {
  it('should get absent from "all of"', () => {
    expect(getAllAbsent(['1', '2', '3'], ['5'])).toEqual(['5']);

    expect(getAllAbsent(['1', '2', '3'], ['8'])).toEqual(['8']);

    expect(getAllAbsent(['1', '2', '3'], ['2'])).toEqual([]);

    expect(getAllAbsent(['1', '2', '3'], ['3', '1'])).toEqual([]);
  });

  it('should get all absent from "any of"', () => {
    expect(hasAnyOf(['1', '2', '3'], ['5', '6'])).toBe(false);

    expect(hasAnyOf(['1', '2', '3'], ['5'])).toBe(false);

    expect(hasAnyOf(['1', '2', '3'], ['2', '6'])).toBe(true);

    expect(hasAnyOf(['1', '2', '3'], ['1'])).toBe(true);
  });
});
