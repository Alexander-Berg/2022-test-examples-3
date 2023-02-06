import maybe from '@/utils/maybe';

const split = (separator: string) => (val: string) => val.split(separator);
const map = <T, R>(fn: (a: T) => R) => (val: T[]): R[] => val.map(fn);
const checkNull = (val: any): string | null =>
  typeof val !== 'string' ? val : null;
const checkVoid = (val: any): string | undefined =>
  typeof val !== 'string' ? val : undefined;

describe('maybe', () => {
  it('должен верно отрабатывать get', () => {
    expect(maybe(1).get()).toBe(1);
    expect(maybe(1).getOrElse(2)).toBe(1);
    expect(maybe(undefined).getOrElse(2)).toBe(2);
  });

  it('должен верно отрабатывать метод tap', () => {
    const before = jest.fn();
    const tap = jest.fn();
    const or = jest.fn();

    expect(
      maybe(1)
        .tap(n => expect(n).toBe(1))
        .get(),
    ).toBe(1);

    maybe('1').or(before).map(checkVoid).or(or);
    maybe('1').map(checkVoid).or(or);
    maybe('1').map(checkVoid).tap(tap).or(or);
    maybe('1').map(checkVoid).tapOr(tap, or);

    expect(before).not.toBeCalled();
    expect(tap).not.toBeCalled();
    expect(or).toBeCalledTimes(4);
  });

  it('должен верно отрабатывать метод map', () => {
    const splitComma = jest.fn(split(','));
    const mapNumber = jest.fn(map(Number));

    expect(maybe('1,2,3').map(splitComma).map(mapNumber).get()).toEqual([
      1,
      2,
      3,
    ]);

    expect(splitComma).toBeCalledTimes(1);
    expect(mapNumber).toBeCalledTimes(1);
  });

  it('должен верно отрабатывать появившийся undefined или null', () => {
    const splitComma = jest.fn(split(','));
    const mapNumber = jest.fn(map(Number));

    expect(
      maybe('1,2,3').map(checkNull).map(splitComma).map(mapNumber).get(),
    ).toEqual(null);
    expect(
      maybe('1,2,3').map(checkVoid).map(splitComma).map(mapNumber).get(),
    ).toEqual(null);

    expect(splitComma).not.toBeCalled();
    expect(mapNumber).not.toBeCalled();
  });
});
