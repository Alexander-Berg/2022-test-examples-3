import isNeedToUpdateResourcesBooking from '../isNeedToUpdateResourcesBooking';

describe('isNeedToUpdateResourcesBooking', () => {
  test('вернуть false, если списки офисов эквивалентны', () => {
    expect(isNeedToUpdateResourcesBooking([1, 2, 3], [3, 2, 1])).toBe(false);
  });

  test('вернуть true, если списки офисов не эквивалентны', () => {
    expect(isNeedToUpdateResourcesBooking([1, 2, 3], [3, 2, 9])).toBe(true);
  });
});
