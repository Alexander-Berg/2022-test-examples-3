import getOfficesOrder from '../getOfficesOrder';

describe('getOfficesOrder', () => {
  test('должен формировать объект с порядком офисов для переговорок встречи', () => {
    const event = {
      resources: [
        {email: 'x@y.z', officeId: 3},
        {email: 'y@y.z', officeId: 2},
        {email: 'z@y.z', officeId: 1}
      ]
    };
    const officesOrder = {
      1: 2,
      2: 1,
      3: 0
    };
    expect(getOfficesOrder(event)).toEqual(officesOrder);
  });
});
