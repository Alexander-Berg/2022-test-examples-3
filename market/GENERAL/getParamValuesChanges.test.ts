import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { getChanges } from './getParamValuesChanges';
import { ParameterValue } from 'src/entities';
import { ParamValuesChangedBy, ValuesType } from './types';

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

const enumValue1 = {
  type: ValueType.ENUM,
  optionId: 1,
} as ParameterValue;

const our = [numericValue1, stringValue2, booleanValue1, enumValue1];

const their = [numericValue2, stringValue1, booleanValue1, enumValue1];

const proto = [numericValue1, stringValue1, booleanValue1, enumValue1];

describe('getParamValuesChanges', () => {
  it('getChanges with conflicts', () => {
    expect(getChanges(our, their, proto, ValuesType.Params)).toEqual({
      conflicts: {
        addedInOur: [stringValue2],
        addedInTheir: [numericValue2],
        deletedInOur: [stringValue1],
        deletedInTheir: [numericValue1],
      },
    });
  });

  it('getChanges with our changes', () => {
    expect(getChanges(our, proto, proto, ValuesType.Params)).toEqual({
      updates: {
        added: [stringValue2],
        deleted: [stringValue1],
        changedBy: ParamValuesChangedBy.Our,
      },
    });
  });

  it('getChanges with their changes', () => {
    expect(getChanges(proto, their, proto, ValuesType.Params)).toEqual({
      updates: {
        added: [numericValue2],
        deleted: [numericValue1],
        changedBy: ParamValuesChangedBy.Their,
      },
    });
  });
});
