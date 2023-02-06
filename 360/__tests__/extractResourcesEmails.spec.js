import extractResourcesEmails from '../extractResourcesEmails';

describe('eventForm/utils/extractResourcesEmails', () => {
  test('должен извлекать email адреса переговорок', () => {
    const values = {
      resources: [
        {
          officeId: 1,
          resource: null
        },
        {
          officeId: 2,
          resource: {
            email: 'room1@yandex-team.ru'
          }
        },
        {
          officeId: 3,
          resource: {
            email: 'room2@yandex-team.ru'
          }
        }
      ]
    };
    const expectedValues = ['room1@yandex-team.ru', 'room2@yandex-team.ru'];

    expect(extractResourcesEmails(values)).toEqual(expectedValues);
  });

  test('должен возвращать пустой массив, если нет переговорок', () => {
    const values = {};
    const expectedValues = [];

    expect(extractResourcesEmails(values)).toEqual(expectedValues);
  });

  test('должен возвращать пустой массив, если не передали данных', () => {
    const values = undefined;
    const expectedValues = [];

    expect(extractResourcesEmails(values)).toEqual(expectedValues);
  });
});
