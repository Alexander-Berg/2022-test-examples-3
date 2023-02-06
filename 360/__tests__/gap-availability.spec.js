const gapAvailability = require('../gap-availability');

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

    gapAvailability({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('staff');
  });

  test('должен ходить в ручку staff с нужными параметрами', () => {
    const params = {a: 1};

    staffFn.mockResolvedValue({});

    gapAvailability(params, coreMock);

    expect(staffFn).toHaveBeenCalledTimes(1);
    expect(staffFn).toHaveBeenCalledWith(`/gap-api/api/export_gaps`, params);
  });
});
