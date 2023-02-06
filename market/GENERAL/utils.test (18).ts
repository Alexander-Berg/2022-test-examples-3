import { stripISOTime, setEndOfDayISOTime, skipEmptyKeys } from './utils';

describe('Audit store utils', () => {
  const strDate = '2019-11-29T08:20:53.959Z';

  it('stripISOTime: should strip the time from the date', () => {
    const date = stripISOTime(strDate);

    expect(date).toEqual('2019-11-29');
  });

  it('setEndOfDayISOTime: should set the time to 23:59:59.999', () => {
    expect(setEndOfDayISOTime(strDate)).toEqual('2019-11-29T23:59:59.999Z');
  });

  it('skipEmptyKeys: should skip null, undefined, NaN and empty string values from the object', () => {
    const obj = {
      orderId: null,
      employeeId: undefined,
      nan: NaN,
      foo: 'bar',
      baz: '',
      arr: [],
      obj: {},
    };
    expect(skipEmptyKeys(obj as any)).toStrictEqual({ foo: 'bar', arr: [], obj: {} });
  });
});
