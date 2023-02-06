const staffPersons = require('../staff-persons');

describe('models:staff-api -> staff-persons', () => {
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

  test('должен вызывать сервис staff-api', () => {
    staffFn.mockResolvedValue({});

    staffPersons({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('staff-api');
  });

  test('должен ходить в ручку staff-api с нужными параметрами', () => {
    const params = {a: 1};

    staffFn.mockResolvedValue({});

    staffPersons(params, coreMock);

    expect(staffFn).toHaveBeenCalledTimes(1);
    expect(staffFn).toHaveBeenCalledWith(`/persons`, params);
  });
});
