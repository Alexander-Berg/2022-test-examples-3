const filter = require('../aceventura-suggest');

describe('filter/aceventura-suggest', () => {
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

  test('не должен возвращать контакты без email', () => {
    expect(
      filter({
        contacts: [
          {
            vcard: {
              emails: [{email: 's@s.s'}]
            }
          },
          {
            vcard: {
              emails: [],
              names: [{first: 'sd'}]
            }
          }
        ]
      })
    ).toEqual({
      contacts: [{email: 's@s.s'}]
    });
  });

  test('должен возвращать первый email для паблика', () => {
    expect(
      filter({
        contacts: [
          {
            vcard: {
              emails: [{email: 's@s.s'}, {email: 'z@s.s'}]
            }
          }
        ]
      })
    ).toEqual({
      contacts: [{email: 's@s.s'}]
    });
  });

  test('должен возвращать первый корповый email для корпа, если есть корповый email в списке', () => {
    expect(
      filter(
        {
          contacts: [
            {
              vcard: {
                emails: [{email: 's@s.s'}, {email: 'z@yandex-team.ru'}, {email: 'b@yandex-team.ru'}]
              }
            }
          ]
        },
        true
      )
    ).toEqual({
      contacts: [{email: 'z@yandex-team.ru'}]
    });
  });

  test('должен возвращать первый email для корпа, если нет корпового email в списке', () => {
    expect(
      filter(
        {
          contacts: [
            {
              vcard: {
                emails: [{email: 's@s.s'}, {email: 'z@s.ru'}]
              }
            }
          ]
        },
        true
      )
    ).toEqual({
      contacts: [{email: 's@s.s'}]
    });
  });

  test('должен собирать имя из частей', () => {
    expect(
      filter({
        contacts: [
          {
            vcard: {
              names: [{first: 'X', last: 'Y'}],
              emails: [{email: 's@s.s'}]
            }
          }
        ]
      })
    ).toEqual({
      contacts: [{name: 'X Y', email: 's@s.s'}]
    });
  });

  test('должен отдавать нормальное имя, если есть только first или last', () => {
    expect(
      filter({
        contacts: [
          {
            vcard: {
              names: [{last: 'Y'}],
              emails: [{email: 's@s.s'}]
            }
          },
          {
            vcard: {
              names: [{first: 'X'}],
              emails: [{email: 's@s.s'}]
            }
          }
        ]
      })
    ).toEqual({
      contacts: [{name: 'Y', email: 's@s.s'}, {name: 'X', email: 's@s.s'}]
    });
  });
});
