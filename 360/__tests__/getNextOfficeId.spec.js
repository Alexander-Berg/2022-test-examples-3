import getNextOfficeId from '../getNextOfficeId';

const offices = [{id: 1}, {id: 2}, {id: 3}, {id: 4}, {id: 5}, {id: 6}];

describe('getNextOfficeId', () => {
  test('должен возвращать первый офис в списке, если нет данных', () => {
    expect(
      getNextOfficeId({
        offices,
        resources: []
      })
    ).toEqual(1);
  });

  test('должен возвращать текущий офис, если нет переговорки с ним', () => {
    expect(
      getNextOfficeId({
        offices,
        resources: [],
        currentOfficeId: 3,
        attendees: [],
        organizer: {}
      })
    ).toEqual(3);
  });

  test('должен возвращать текущий офис во встрече без участников и переговорок', () => {
    expect(
      getNextOfficeId({
        offices,
        resources: [],
        currentOfficeId: 3,
        attendees: [],
        organizer: {}
      })
    ).toEqual(3);
  });

  test('должен возвращать офис организатора, если есть текущий, но нет офиса организатора', () => {
    expect(
      getNextOfficeId({
        offices,
        resources: [{officeId: 3}],
        currentOfficeId: 3,
        attendees: [],
        organizer: {officeId: 4}
      })
    ).toEqual(4);
  });

  test('должен возвращать офис организатора, только если он у организатора отмечен', () => {
    expect(
      getNextOfficeId({
        offices,
        resources: [{officeId: 3}],
        currentOfficeId: 3,
        attendees: [{officeId: 4}],
        organizer: {email: '123'}
      })
    ).toEqual(4);
  });

  test('должен возвращать офисы участников, если есть текущий и организатора', () => {
    expect(
      getNextOfficeId({
        offices,
        resources: [{officeId: 3}, {officeId: 4}],
        currentOfficeId: 3,
        attendees: [{officeId: 5}],
        organizer: {officeId: 4}
      })
    ).toEqual(5);
  });

  test('должен возвращать следующий в списке офис, если есть текущий, организатора и участников', () => {
    expect(
      getNextOfficeId({
        offices,
        resources: [{officeId: 3}, {officeId: 4}, {officeId: 5}],
        currentOfficeId: 3,
        attendees: [{officeId: 5}],
        organizer: {officeId: 4}
      })
    ).toEqual(1);
  });

  test('должен возвращать текущий офис, если есть все офисы из списка', () => {
    expect(
      getNextOfficeId({
        offices,
        resources: [
          {officeId: 1},
          {officeId: 2},
          {officeId: 3},
          {officeId: 4},
          {officeId: 5},
          {officeId: 6}
        ],
        currentOfficeId: 3,
        attendees: [{officeId: 5}],
        organizer: {officeId: 4}
      })
    ).toEqual(3);
  });
});
