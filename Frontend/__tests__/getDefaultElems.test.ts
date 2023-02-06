import { getDefaultElems } from '../getDefaultElems';

describe('getDefaultElems', () => {
  it('Функция возвращает элементы по очереди и по кругу', () => {
    const data = [48, 'test', -3];
    const getter = getDefaultElems<number | string>(data);

    const loopedData = [getter(), getter(), getter(), getter()];

    expect(loopedData).toEqual([48, 'test', -3, 48]);
  });

  it('Функция итерируется по пустому массиву', () => {
    const data: undefined[] = [];
    const getter = getDefaultElems(data);

    const loopedData = [getter(), getter()];

    expect(loopedData).toEqual([undefined, undefined]);
  });
});
