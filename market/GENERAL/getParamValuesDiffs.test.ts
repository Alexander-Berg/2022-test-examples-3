import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { isEqualWrapper, getDiff } from './getParamValuesDiffs';
import { NormalizedImage, ParameterValue } from 'src/entities';
import { ValuesType } from './types';

const numericValue1 = {
  type: ValueType.NUMERIC,
  numericValue: '5',
} as ParameterValue;

const numericValue2 = {
  type: ValueType.NUMERIC,
  numericValue: '6',
} as ParameterValue;

const stringValue1 = {
  type: ValueType.STRING,
  stringValue: [
    {
      value: 'test1',
    },
  ],
} as ParameterValue;

const stringValue2 = {
  type: ValueType.STRING,
  stringValue: [
    {
      value: 'test2',
    },
  ],
} as ParameterValue;

const booleanValue1 = {
  type: ValueType.BOOLEAN,
  booleanValue: true,
} as ParameterValue;

const booleanValue2 = {
  type: ValueType.BOOLEAN,
  booleanValue: false,
} as ParameterValue;

const enumValue1 = {
  type: ValueType.ENUM,
  optionId: 1,
} as ParameterValue;

const enumValue2 = {
  type: ValueType.ENUM,
  optionId: 2,
} as ParameterValue;

const image1 = {
  url: '1',
} as NormalizedImage;

const image2 = {
  url: '2',
} as NormalizedImage;

const values1 = [numericValue1, stringValue1, booleanValue1, enumValue1];

const values2 = [numericValue1, stringValue2, booleanValue1, enumValue2];

describe('getParamValuesDiffs', () => {
  it('isEqualWrapper with different numeric values', () => {
    expect(isEqualWrapper(numericValue1, numericValue2, ValuesType.Params)).toBeFalsy();
  });
  it('isEqualWrapper with equal numeric values', () => {
    expect(isEqualWrapper(numericValue1, numericValue1, ValuesType.Params)).toBeTruthy();
  });
  it('isEqualWrapper with different string values', () => {
    expect(isEqualWrapper(stringValue1, stringValue2, ValuesType.Params)).toBeFalsy();
  });
  it('isEqualWrapper with different boolean values', () => {
    expect(isEqualWrapper(booleanValue1, booleanValue2, ValuesType.Params)).toBeFalsy();
  });
  it('isEqualWrapper with different enum values', () => {
    expect(isEqualWrapper(enumValue1, enumValue2, ValuesType.Params)).toBeFalsy();
  });
  it('isEqualWrapper with different image values', () => {
    expect(isEqualWrapper(image1, image2, ValuesType.Images)).toBeFalsy();
  });
  it('isEqualWrapper with skus', () => {
    expect(isEqualWrapper(numericValue1, numericValue1, ValuesType.Skus)).toBeFalsy();
  });

  it('getDiff with parameters', () => {
    expect(getDiff(values1, values2, ValuesType.Params)).toEqual([stringValue1, enumValue1]);
  });
});
