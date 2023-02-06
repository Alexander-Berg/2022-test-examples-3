const staffActivity = require('../staff-activity');

describe('models:staff -> gap-availability', () => {
  let coreMock;
  let serviceFn;
  let staffFn;
  const uid = 1234567890;
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    serviceFn = jest.fn();
    staffFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid})
      },
      config: {
        connectionid
      }
    };

    serviceFn.mockReturnValue(staffFn);
  });

  test('должен вызывать сервис staff', () => {
    staffFn.mockResolvedValue({});

    staffActivity({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('staff');
  });

  test('должен ходить в ручку staff с нужными параметрами', () => {
    const params = {login: 'tet4enko'};

    staffFn.mockResolvedValue({});

    staffActivity(params, coreMock);

    expect(staffFn).toHaveBeenCalledTimes(1);
    expect(staffFn).toHaveBeenCalledWith('/whistlah/where/' + params.login, null, {
      method: 'GET'
    });
  });
});
