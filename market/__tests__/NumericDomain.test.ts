import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { ParameterValue } from '@yandex-market/mbo-parameter-editor';

import * as mocks from '../mocks';
import * as testUtils from '../mocks/testUtils';
import { NumericDomain } from '../NumericDomain';

const { intersection, validation, commonAssertion } = testUtils;

const parameter = mocks.getParameterMock({
  xslName: 'param',
  valueType: ValueType.NUMERIC,
});

const val = (value?: number): ParameterValue =>
  mocks.getParameterValueMock({
    parameterId: parameter.id,
    type: ValueType.NUMERIC,
    numericValue: String(value),
  });

describe('numeric domain', () => {
  test('any domain and any domain intersection', () => {
    intersection(NumericDomain.any(parameter), NumericDomain.any(parameter), NumericDomain.any(parameter));
  });

  test('any domain and empty domain intersection', () => {
    intersection(NumericDomain.any(parameter), NumericDomain.empty(parameter), NumericDomain.empty(parameter));
  });

  test('any domain and domain without empty value intersection', () => {
    intersection(
      NumericDomain.any(parameter),
      NumericDomain.notContainsEmpty(parameter),
      NumericDomain.notContainsEmpty(parameter)
    );
  });

  test('empty value domain and domain without empty value intersection', () => {
    intersection(
      NumericDomain.singleEmptyValue(parameter),
      NumericDomain.notContainsEmpty(parameter),
      NumericDomain.empty(parameter)
    );
  });

  test('empty domain and empty domain intersection', () => {
    intersection(NumericDomain.empty(parameter), NumericDomain.empty(parameter), NumericDomain.empty(parameter));
  });

  test('empty and single empty intersection', () => {
    intersection(
      NumericDomain.empty(parameter),
      NumericDomain.singleEmptyValue(parameter),
      NumericDomain.empty(parameter)
    );
  });

  test('empty and range intersection', () => {
    intersection(
      NumericDomain.empty(parameter),
      NumericDomain.range(parameter, 80, 120),
      NumericDomain.empty(parameter)
    );
  });

  test('single empty and range intersection', () => {
    intersection(
      NumericDomain.singleEmptyValue(parameter),
      NumericDomain.range(parameter, 80, 120),
      NumericDomain.empty(parameter)
    );
  });

  test('single empty and empty plus range intersection', () => {
    intersection(
      NumericDomain.singleEmptyValue(parameter),
      NumericDomain.rangeAndEmpty(parameter, 80, 120),
      NumericDomain.singleEmptyValue(parameter)
    );
  });

  test('range and range intersection1', () => {
    intersection(
      NumericDomain.range(parameter, 10, 100),
      NumericDomain.range(parameter, 80, 120),
      NumericDomain.range(parameter, 80, 100)
    );
  });

  test('range and range intersection2', () => {
    intersection(
      NumericDomain.range(parameter, 10, 100),
      NumericDomain.range(parameter, 100, 120),
      NumericDomain.single(parameter, 100)
    );
  });

  test('range and range intersection3', () => {
    intersection(
      NumericDomain.range(parameter, 10, 100),
      NumericDomain.range(parameter, 110, 120),
      NumericDomain.empty(parameter)
    );
  });

  test('range and empty plus range intersection1', () => {
    intersection(
      NumericDomain.range(parameter, 10, 100),
      NumericDomain.rangeAndEmpty(parameter, 80, 120),
      NumericDomain.range(parameter, 80, 100)
    );
  });

  test('range and empty plus range intersection2', () => {
    intersection(
      NumericDomain.range(parameter, 10, 100),
      NumericDomain.rangeAndEmpty(parameter, 100, 120),
      NumericDomain.single(parameter, 100)
    );
  });

  test('range and empty plus range intersection3', () => {
    intersection(
      NumericDomain.range(parameter, 10, 100),
      NumericDomain.rangeAndEmpty(parameter, 110, 120),
      NumericDomain.empty(parameter)
    );
  });

  test('empty plus range and empty plus range intersection1', () => {
    intersection(
      NumericDomain.rangeAndEmpty(parameter, 10, 100),
      NumericDomain.rangeAndEmpty(parameter, 80, 120),
      NumericDomain.rangeAndEmpty(parameter, 80, 100)
    );
  });

  test('empty plus range and empty plus range intersection2', () => {
    intersection(
      NumericDomain.rangeAndEmpty(parameter, 10, 100),
      NumericDomain.rangeAndEmpty(parameter, 100, 120),
      NumericDomain.singleAndEmpty(parameter, 100)
    );
  });

  test('empty plus range and empty plus range intersection3', () => {
    intersection(
      NumericDomain.rangeAndEmpty(parameter, 10, 100),
      NumericDomain.rangeAndEmpty(parameter, 110, 120),
      NumericDomain.singleEmptyValue(parameter)
    );
  });

  test('unbounded ranges intersection1', () => {
    intersection(
      NumericDomain.greaterOrEqual(parameter, 10),
      NumericDomain.greaterOrEqual(parameter, 20),
      NumericDomain.greaterOrEqual(parameter, 20)
    );
  });

  test('unbounded ranges intersection2', () => {
    intersection(
      NumericDomain.greaterOrEqual(parameter, 20),
      NumericDomain.lessOrEqual(parameter, 10),
      NumericDomain.empty(parameter)
    );
  });

  test('unbounded ranges intersection3', () => {
    intersection(
      NumericDomain.greaterOrEqual(parameter, 10),
      NumericDomain.lessOrEqual(parameter, 10),
      NumericDomain.single(parameter, 10)
    );
  });

  test('unbounded ranges intersection4', () => {
    intersection(
      NumericDomain.greaterOrEqual(parameter, 15),
      NumericDomain.range(parameter, 10, 20),
      NumericDomain.range(parameter, 15, 20)
    );
  });

  test('unbounded ranges intersection5', () => {
    intersection(
      NumericDomain.greaterOrEqual(parameter, 30),
      NumericDomain.range(parameter, 10, 20),
      NumericDomain.empty(parameter)
    );
  });

  test('unbounded ranges intersection6', () => {
    intersection(
      NumericDomain.lessOrEqual(parameter, 15),
      NumericDomain.range(parameter, 10, 20),
      NumericDomain.range(parameter, 10, 15)
    );
  });

  test('unbounded ranges intersection7', () => {
    intersection(
      NumericDomain.lessOrEqual(parameter, 5),
      NumericDomain.range(parameter, 10, 20),
      NumericDomain.empty(parameter)
    );
  });

  test('unbounded ranges intersection8', () => {
    intersection(
      NumericDomain.any(parameter),
      NumericDomain.lessOrEqualAndEmpty(parameter, 20),
      NumericDomain.lessOrEqualAndEmpty(parameter, 20)
    );
  });

  test('unbounded ranges intersection9', () => {
    intersection(
      NumericDomain.not(NumericDomain.any(parameter)),
      NumericDomain.lessOrEqualAndEmpty(parameter, 20),
      NumericDomain.lessOrEqual(parameter, 20)
    );
  });

  test('unbounded ranges intersection10', () => {
    intersection(
      NumericDomain.any(parameter),
      NumericDomain.greaterOrEqualAndEmpty(parameter, 20),
      NumericDomain.greaterOrEqualAndEmpty(parameter, 20)
    );
  });

  test('unbounded ranges intersection11', () => {
    intersection(
      NumericDomain.not(NumericDomain.any(parameter)),
      NumericDomain.greaterOrEqualAndEmpty(parameter, 20),
      NumericDomain.greaterOrEqual(parameter, 20)
    );
  });

  test('range and empty throw error', () => {
    expect(() => NumericDomain.rangeAndEmpty(parameter, 100, 10)).toThrow();
  });

  test('validate not empty value', () => {
    validation(NumericDomain.empty(parameter), [val(10)], 'Пустая область значений');
    validation(NumericDomain.singleEmptyValue(parameter), [val(10)], 'Значение должно отсутствовать');
    validation(NumericDomain.single(parameter, 10), [val(10)], '');
    validation(NumericDomain.range(parameter, 10, 100), [val(10)], '');
    validation(NumericDomain.range(parameter, 10, 100), [val(20)], '');
    validation(NumericDomain.greaterOrEqual(parameter, 10), [val(10)], '');
    validation(NumericDomain.lessOrEqual(parameter, 100), [val(100)], '');
  });

  test('get values', () => {
    expect(NumericDomain.empty(parameter).getValues()).toBeUndefined();
    expect(NumericDomain.any(parameter).getValues()).toBeUndefined();
    expect(
      commonAssertion(parameter, NumericDomain.singleEmptyValue(parameter).getValues()!, ValueType.NUMERIC)[0]
        .numericValue
    ).toBeUndefined();
    expect(
      commonAssertion(parameter, NumericDomain.single(parameter, 10).getValues()!, ValueType.NUMERIC)[0].numericValue
    ).toBe('10');
    expect(NumericDomain.range(parameter, 10, 100).getValues()).toBeUndefined();
    expect(NumericDomain.rangeAndEmpty(parameter, 10, 100).getValues()).toBeUndefined();
    expect(NumericDomain.greaterOrEqual(parameter, 10).getValues()).toBeUndefined();
    expect(NumericDomain.lessOrEqual(parameter, 10).getValues()).toBeUndefined();
  });
});
