import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { getRussianValues } from '../getRussianValues';

describe('getRussianValues', () => {
  test('return empty array', () => {
    expect(getRussianValues([])).toEqual([]);
    expect(
      getRussianValues([
        {
          parameterId: 1,
          type: ValueType.STRING,
          stringValue: [
            {
              isoCode: 'en',
              value: 'some',
            },
          ],
        },
        {
          parameterId: 1,
          type: ValueType.STRING,
          stringValue: [
            {
              isoCode: 'ru',
              value: '',
            },
          ],
        },
        {
          parameterId: 1,
          type: ValueType.STRING,
          stringValue: [
            {
              isoCode: 'ru',
            },
          ],
        },
      ])
    ).toEqual([]);
  });

  test('return non empty array', () => {
    expect(
      getRussianValues([
        {
          parameterId: 1,
          type: ValueType.STRING,
          stringValue: [
            {
              isoCode: 'ru',
              value: 'foo',
            },
          ],
        },
      ])
    ).toEqual(['foo']);
    expect(
      getRussianValues([
        {
          parameterId: 1,
          type: ValueType.STRING,
          stringValue: [
            {
              isoCode: 'ru',
              value: 'foo',
            },
          ],
        },
        {
          parameterId: 1,
          type: ValueType.STRING,
          stringValue: [
            {
              isoCode: 'ru',
              value: 'bar',
            },
          ],
        },
        {
          parameterId: 1,
          type: ValueType.STRING,
          stringValue: [
            {
              isoCode: 'en',
              value: 'baz',
            },
          ],
        },
      ])
    ).toEqual(['foo', 'bar']);
  });
});
