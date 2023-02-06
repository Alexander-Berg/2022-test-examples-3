import extractAttendeesEmails from '../extractAttendeesEmails';

describe('eventForm/utils/extractAttendeesEmails', () => {
  test('должен извлекать email адреса участников', () => {
    const values = {
      attendees: [
        {
          email: 'test1@ya.ru'
        },
        {
          email: 'test2@ya.ru'
        }
      ]
    };
    const expectedValues = ['test1@ya.ru', 'test2@ya.ru'];

    expect(extractAttendeesEmails(values)).toEqual(expectedValues);
  });

  test('должен возвращать пустой массив, если нет участников', () => {
    const values = {};
    const expectedValues = [];

    expect(extractAttendeesEmails(values)).toEqual(expectedValues);
  });

  test('должен возвращать пустой массив, если не передали данных', () => {
    const values = undefined;
    const expectedValues = [];

    expect(extractAttendeesEmails(values)).toEqual(expectedValues);
  });
});
