jest.mock('../../../filters/staff/staff-person-card.js');

const staffPersonCard = require('../staff-person-card');

describe('models:staff-api -> staff-person-card', () => {
  let coreMock;
  let requestFn;
  const login = 'tet4enko';

  beforeEach(() => {
    requestFn = jest.fn();
    coreMock = {
      request: requestFn,
      config: {
        i18n: {}
      }
    };
  });

  test('должен дернуть три ручки стаффа c нужными параметрами', async () => {
    requestFn.mockResolvedValue({});

    await staffPersonCard({login}, coreMock);

    expect(requestFn).toHaveBeenCalledTimes(3);
    expect(requestFn).toHaveBeenCalledWith('staff-persons', {
      login,
      _one: 1,
      _fields:
        'work_email,login,name,location,phones,work_phone,official,department_group,personal.gender,telegram_accounts'
    });
    expect(requestFn).toHaveBeenCalledWith('gap-availability', {
      person_logins: [login],
      fields: ['date_from', 'date_to', 'workflow'],
      only_one: true
    });
    expect(requestFn).toHaveBeenCalledWith('staff-activity', {
      login
    });
  });
});
