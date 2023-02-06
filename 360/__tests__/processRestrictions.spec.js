import processRestrictions from '../processRestrictions';

describe('processRestrictions', () => {
  test('должен извлекать существующие ограничения в объект', () => {
    const res1 = {
      info: {
        type: 'room',
        email: '1@ya.ru',
        canBook: true
      },
      restrictions: [
        {start: '2019-05-08T10:00:00', end: '2019-05-08T20:00:00', title: 'restriction'}
      ]
    };
    const res2 = {
      info: {
        type: 'campus',
        email: '2@ya.ru',
        canBook: true
      },
      restrictions: [
        {start: '2019-05-08T20:00:00', end: '2019-05-08T23:00:00', title: 'restriction'}
      ]
    };

    expect(processRestrictions([res1, res2])).toEqual({
      '1@ya.ru': [{start: 1557298800000, end: 1557334800000, title: 'restriction'}],
      '2@ya.ru': [{start: 1557334800000, end: 1557345600000, title: 'restriction'}]
    });
  });
  test('должен добавлять start и end для ограничения без этих полей как начало и конец переданной даты', () => {
    const date = Number(new Date(2019, 20, 10, 10));
    const res1 = {
      info: {
        type: 'room',
        email: '1@ya.ru',
        canBook: true
      },
      restrictions: [{title: 'restriction'}]
    };

    expect(processRestrictions([res1], date)).toEqual({
      '1@ya.ru': [{start: 1599685200000, end: 1599771599999, title: 'restriction'}]
    });
  });
  test('должен создавать ограничения, если canBook === false', () => {
    const startOfDay = Number(new Date(2020, 10, 10));
    const endOfDay = Number(new Date(2020, 10, 11));
    const date = Number(new Date(2020, 10, 10));
    const res1 = {
      info: {
        email: '1@ya.ru',
        protection: '123',
        canBook: false
      },
      restrictions: []
    };

    expect(processRestrictions([res1], date)).toEqual({
      '1@ya.ru': [{start: startOfDay, end: endOfDay, title: '123'}]
    });
  });
});
