const filter = require('../suggest-contacts');

describe('filter/abook-suggest', () => {
  test('должен возвращать пустой список контактов, если нет данных', () => {
    expect(filter()).toEqual({
      contacts: []
    });
  });

  test('должен возвращать пустой список контактов, если нет контактов', () => {
    expect(filter({})).toEqual({
      contacts: []
    });
  });

  test('должен возвращать список контактов с пустой занятостью, если её нет', () => {
    const data = {
      contacts: [
        {
          name: 'contact_1_name',
          email: 'contact_1_email',
          login: 'contact_1_login'
        },
        {
          name: 'contact_2_name',
          email: 'contact_2_email',
          login: 'contact_2_login'
        }
      ]
    };

    expect(filter(data)).toEqual({
      contacts: [
        {
          name: 'contact_1_name',
          email: 'contact_1_email',
          login: 'contact_1_login',
          availability: null
        },
        {
          name: 'contact_2_name',
          email: 'contact_2_email',
          login: 'contact_2_login',
          availability: null
        }
      ]
    });
  });

  test('должен возвращать список контактов с занятостью, если она есть', () => {
    const data = {
      contacts: [
        {
          name: 'contact_1_name',
          email: 'contact_1_email',
          login: 'contact_1_login'
        },
        {
          name: 'contact_2_name',
          email: 'contact_2_email',
          login: 'contact_2_login'
        }
      ]
    };
    const availabilities = [
      {
        availability: 'busy'
      },
      {
        availability: 'available'
      }
    ];

    expect(filter(data, availabilities)).toEqual({
      contacts: [
        {
          name: 'contact_1_name',
          email: 'contact_1_email',
          login: 'contact_1_login',
          availability: 'busy'
        },
        {
          name: 'contact_2_name',
          email: 'contact_2_email',
          login: 'contact_2_login',
          availability: 'available'
        }
      ]
    });
  });
});
