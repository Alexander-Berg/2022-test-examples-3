const updateReminder = require('../do-update-reminder');

describe('models:reminders -> do-update-reminder', () => {
  let coreMock;
  let serviceFn;
  let remindersFn;
  const uid = 1234567890;
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    serviceFn = jest.fn();
    remindersFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid})
      },
      config: {
        connectionid
      }
    };

    serviceFn.mockReturnValue(remindersFn);
  });

  test('должен вызывать сервис reminders', () => {
    remindersFn.mockResolvedValue({});

    updateReminder({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('reminders');
  });

  test('должен ходить в ручку reminders с нужными параметрами', () => {
    const params = {id: 'tet4enko'};

    remindersFn.mockResolvedValue({});

    updateReminder(params, coreMock);

    expect(remindersFn).toHaveBeenCalledTimes(1);
    expect(remindersFn).toHaveBeenCalledWith({
      method: 'PUT',
      headers: {
        'content-type': 'application/json'
      },
      query: {
        id: params.id
      },
      body: params
    });
  });
});
