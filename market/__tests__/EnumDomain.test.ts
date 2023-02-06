import { ModificationSource } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { ParameterValue } from '@yandex-market/mbo-parameter-editor';

import { EnumDomain } from '../EnumDomain';
import { ExecutionContext } from '../ExecutionContext/ExecutionContext';
import * as mocks from '../mocks';
import * as testUtils from '../mocks/testUtils';

const { intersection, validation, commonAssertion } = testUtils;

const options = [
  mocks.getOptionMock({ id: 1, name: 'One' }),
  mocks.getOptionMock({ id: 2, name: 'Two' }),
  mocks.getOptionMock({ id: 3, name: 'Three' }),
  mocks.getOptionMock({ id: 4, name: 'Four' }),
  mocks.getOptionMock({ id: 5, name: 'Five' }),
  mocks.getOptionMock({ id: 6, name: 'Six' }),
  mocks.getOptionMock({ id: 7, name: 'TRUE' }),
  mocks.getOptionMock({ id: 8, name: 'FALSE' }),
  mocks.getOptionMock({ id: 9, name: 'TRUE1' }),
];
const parameters = [
  mocks.getParameterMock({ valueType: ValueType.ENUM, xslName: 'param', optionIds: [1, 2, 3, 4] }),
  mocks.getParameterMock({ valueType: ValueType.ENUM, xslName: 'bigEnum', optionIds: [1, 2, 3, 4, 5, 6] }),
  mocks.getParameterMock({ valueType: ValueType.BOOLEAN, xslName: 'boolParam', optionIds: [7, 8] }),
  mocks.getParameterMock({ valueType: ValueType.BOOLEAN, xslName: 'boolParamNoOptions' }),
  mocks.getParameterMock({ valueType: ValueType.BOOLEAN, xslName: 'boolParamIllegalOptions', optionIds: [8, 9] }),
];
const categoryData = mocks.getCategoryDataMock({ parameters, options });
const model = mocks.getModelMock();

const param = parameters[0];
const bigEnum = parameters[1];
const boolParam = parameters[2];
const boolParamNoOptions = parameters[3];
const boolParamIllegalOptions = parameters[4];

const context = new ExecutionContext(categoryData, model, ModificationSource.RULE);

const enumVal = (optionId?: number): ParameterValue =>
  mocks.getParameterValueMock({
    parameterId: param.id,
    type: ValueType.ENUM,
    optionId,
  });

describe('enum domain', () => {
  test('enum string representation', () => {
    expect(EnumDomain.of(context, param, [1]).toString()).toBe('Значение должно быть из списка: One');
    expect(EnumDomain.of(context, param, [1, 2]).toString()).toBe('Значение должно быть из списка: One; Two');
    expect(EnumDomain.of(context, param, [1, 2, 3]).toString()).toBe('Значение должно быть из списка: One; Two; Three');
    expect(EnumDomain.of(context, param, [1, 2, 3, 4]).toString()).toBe(
      'Значение должно быть из списка: One; Two; Three... (еще 1 знач.)'
    );
    expect(EnumDomain.of(context, bigEnum, [1, 2, 3, 4, 5]).toString()).toBe(
      'Значение должно быть из списка: One; Two; Three... (еще 2 знач.)'
    );
    expect(EnumDomain.of(context, bigEnum, [1, 2, 3, 4, 5, 6]).toString()).toBe(
      'Значение должно быть из списка: One; Two; Three... (еще 3 знач.)'
    );
  });

  test('boolean no options creation fail', () => {
    expect(() => EnumDomain.bool(context, boolParamNoOptions, true)).toThrow();
  });

  test('boolean illegal options creation fail', () => {
    expect(() => EnumDomain.bool(context, boolParamIllegalOptions, true)).toThrow();
  });

  test('wrong option creation fail', () => {
    expect(() => EnumDomain.of(context, param, [1, 2, 3, 4, 5])).toThrow();
  });

  test('creation with duplicated options', () => {
    expect(EnumDomain.of(context, param, [1, 2, 2, 3]).isEqual(EnumDomain.of(context, param, [1, 2, 3]))).toBe(true);
  });

  test('empty domain and empty domain intersection', () => {
    intersection(EnumDomain.empty(context, param), EnumDomain.empty(context, param), EnumDomain.empty(context, param));
  });

  test('empty domain and not empty domain intersection', () => {
    intersection(
      EnumDomain.empty(context, param),
      EnumDomain.of(context, param, [1, 2]),
      EnumDomain.empty(context, param)
    );
  });

  test('not empty and not empty intersection success1', () => {
    intersection(
      EnumDomain.of(context, param, [1, 2, 3]),
      EnumDomain.of(context, param, [2, 3, 4]),
      EnumDomain.of(context, param, [2, 3])
    );
  });

  test('not empty and not empty intersection success2', () => {
    intersection(
      EnumDomain.of(context, param, [1, 2]),
      EnumDomain.of(context, param, [2, 3]),
      EnumDomain.of(context, param, [2])
    );
  });

  test('not empty and not empty intersection conflict', () => {
    intersection(
      EnumDomain.of(context, param, [1, 2]),
      EnumDomain.of(context, param, [3, 4]),
      EnumDomain.empty(context, param)
    );
  });

  test('intersection not logic', () => {
    intersection(
      EnumDomain.not(context, param, [1]),
      EnumDomain.not(context, param, [2]),
      EnumDomain.not(context, param, [1, 2])
    );

    intersection(
      EnumDomain.not(context, param, [1, 3]),
      EnumDomain.of(context, param, [1, 2, 3, 4]),
      EnumDomain.of(context, param, [2, 4])
    );

    intersection(
      EnumDomain.not(context, param, [1, 3]),
      EnumDomain.of(context, param, [2, 4]),
      EnumDomain.of(context, param, [2, 4])
    );

    intersection(
      EnumDomain.not(context, param, [1, 2]),
      EnumDomain.of(context, param, [1, 2]),
      EnumDomain.empty(context, param)
    );
  });

  test('validate not', () => {
    validation(EnumDomain.not(context, param, [1]), [enumVal(2)], '');
    validation(EnumDomain.not(context, param, [1]), [enumVal(1)], 'Значение не должно содержать: One');
    validation(EnumDomain.not(context, param, [1, 2]), [enumVal(2)], 'Значение не должно содержать: One; Two');
  });

  test('validate not empty value', () => {
    validation(EnumDomain.empty(context, param), [enumVal(2)], 'Пустая область значений');
    validation(EnumDomain.of(context, param, [EnumDomain.EMPTY]), [enumVal(2)], 'Значение должно отсутствовать');
    validation(EnumDomain.of(context, param, [1, 2, 3]), [enumVal(2)], '');
    validation(EnumDomain.of(context, param, [1]), [enumVal(4)], 'Значение должно быть из списка: One');
    validation(
      EnumDomain.of(context, param, [1, 2, 3]),
      [enumVal(4)],
      'Значение должно быть из списка: One; Two; Three'
    );
    validation(
      EnumDomain.of(context, param, [EnumDomain.EMPTY, 1, 2, 3]),
      [enumVal(4)],
      'Значение должно отсутствовать или содержаться в следующем списке: One; Two; Three'
    );
  });

  test('get values', () => {
    expect(EnumDomain.empty(context, param).getValues()).toBeUndefined();
    expect(commonAssertion(param, EnumDomain.of(context, param, [EnumDomain.EMPTY]).getValues()!, ValueType.ENUM));
    expect(commonAssertion(param, EnumDomain.of(context, param, [1]).getValues()!, ValueType.ENUM)[0].optionId).toBe(1);
    expect(EnumDomain.of(context, param, [EnumDomain.EMPTY, 1]).getValues()).toBeUndefined();
    expect(EnumDomain.of(context, param, [1, 2]).getValues()).toBeUndefined();
    expect(EnumDomain.not(context, param, [1]).getValues()).toBeUndefined();
  });

  test('get single boolean value', () => {
    expect(EnumDomain.empty(context, boolParam).getValues()).toBeUndefined();
    expect(
      commonAssertion(boolParam, EnumDomain.of(context, boolParam, [7]).getValues()!, ValueType.BOOLEAN)[0].booleanValue
    ).toBe(true);
    expect(
      commonAssertion(boolParam, EnumDomain.of(context, boolParam, [8]).getValues()!, ValueType.BOOLEAN)[0].booleanValue
    ).toBe(false);
    expect(
      commonAssertion(boolParam, EnumDomain.of(context, boolParam, [7]).getValues()!, ValueType.BOOLEAN)[0].optionId
    ).toBe(7);
    expect(EnumDomain.of(context, boolParam, [7, 8]).getValues()).toBeUndefined();
    expect(EnumDomain.not(context, boolParam, [7]).getValues()).toBeUndefined();
  });
});
