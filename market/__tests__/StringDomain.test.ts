import { ModificationSource, ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { getRuLocalizedString } from '@yandex-market/mbo-parameter-editor';

import { ExecutionContext } from '../ExecutionContext/ExecutionContext';
import * as mocks from '../mocks';
import * as testUtils from '../mocks/testUtils';
import { StringDomain } from '../StringDomain';

const { intersection, validation, commonAssertion } = testUtils;

const param = mocks.getParameterMock({
  xslName: 'param',
  valueType: ValueType.STRING,
});
const categoryData = mocks.getCategoryDataMock({ parameters: [param] });
const model = mocks.getModelMock();
const context: ExecutionContext = new ExecutionContext(categoryData, model, ModificationSource.RULE);

const val = (...args: string[]) => {
  const strings = args || [];

  return strings.map(value =>
    mocks.getParameterValueMock({
      stringValue: [
        {
          isoCode: 'ru',
          value,
        },
      ],
    })
  );
};

describe('string domain', () => {
  test('any domain and any domain intersection', () => {
    intersection(StringDomain.any(context, param), StringDomain.any(context, param), StringDomain.any(context, param));
  });

  test('empty domain and empty domain intersection', () => {
    intersection(
      StringDomain.empty(context, param),
      StringDomain.empty(context, param),
      StringDomain.empty(context, param)
    );
  });

  test('empty domain and any domain intersection', () => {
    intersection(
      StringDomain.empty(context, param),
      StringDomain.any(context, param),
      StringDomain.empty(context, param)
    );
  });

  test('any domain and domain without empty value intersection', () => {
    intersection(
      StringDomain.any(context, param),
      StringDomain.notContainsEmpty(context, param),
      StringDomain.notContainsEmpty(context, param)
    );
  });

  test('intersection of two domains without empty values', () => {
    intersection(
      StringDomain.notContainsEmpty(context, param),
      StringDomain.notContainsEmpty(context, param),
      StringDomain.notContainsEmpty(context, param)
    );
  });

  test('empty value domain and domain without empty value intersection', () => {
    intersection(
      StringDomain.match(context, param, StringDomain.EMPTY),
      StringDomain.notContainsEmpty(context, param),
      StringDomain.empty(context, param)
    );
  });

  test('empty and match intersection', () => {
    intersection(
      StringDomain.empty(context, param),
      StringDomain.match(context, param, ['aaa', 'bbb']),
      StringDomain.empty(context, param)
    );
  });

  test('empty and substring intersection', () => {
    intersection(
      StringDomain.empty(context, param),
      StringDomain.substring(context, param, ['aaa', 'bbb']),
      StringDomain.empty(context, param)
    );
  });

  test('match and match intersection success', () => {
    intersection(
      StringDomain.match(context, param, ['aaa', 'bbb']),
      StringDomain.match(context, param, ['aaa', 'bbb']),
      StringDomain.match(context, param, ['aaa', 'bbb'])
    );
  });

  test('match and match intersection fail', () => {
    intersection(
      StringDomain.match(context, param, ['aaa', 'bbb']),
      StringDomain.match(context, param, ['aaa', 'ccc']),
      StringDomain.empty(context, param)
    );
  });

  test('match and substring intersection success1', () => {
    intersection(
      StringDomain.match(context, param, ['aaa', 'bbb']),
      StringDomain.substring(context, param, ['aa', 'bb']),
      StringDomain.match(context, param, ['aaa', 'bbb'])
    );
  });

  test('match and substring intersection success2', () => {
    intersection(
      StringDomain.match(context, param, ['aaa', 'bbb']),
      StringDomain.substring(context, param, ['aa', 'bb', 'aaa']),
      StringDomain.match(context, param, ['aaa', 'bbb'])
    );
  });

  test('match and substring intersection fail1', () => {
    intersection(
      StringDomain.match(context, param, ['aaa', 'bbb']),
      StringDomain.substring(context, param, ['dd']),
      StringDomain.empty(context, param)
    );
  });

  test('match and substring intersection fail2', () => {
    intersection(
      StringDomain.match(context, param, ['aaa', 'bbb']),
      StringDomain.substring(context, param, ['aaa', 'bbbb']),
      StringDomain.empty(context, param)
    );
  });

  test('match and any intersection', () => {
    intersection(
      StringDomain.match(context, param, ['aaa', 'bbb']),
      StringDomain.any(context, param),
      StringDomain.match(context, param, ['aaa', 'bbb'])
    );
  });

  test('mismatch any intersection', () => {
    intersection(
      StringDomain.mismatch(context, param, ['aaa', 'bbb']),
      StringDomain.any(context, param),
      StringDomain.mismatch(context, param, ['aaa', 'bbb'])
    );
  });

  test('mismatch intersection', () => {
    intersection(
      StringDomain.mismatch(context, param, ['aaa', 'bbb']),
      StringDomain.mismatch(context, param, ['aaa', 'ccc']),
      StringDomain.mismatch(context, param, ['aaa', 'bbb', 'ccc'])
    );
  });

  test('mismatch and match intersection', () => {
    intersection(
      StringDomain.match(context, param, ['aaa']),
      StringDomain.mismatch(context, param, ['ccc']),
      StringDomain.match(context, param, ['aaa'])
    );

    intersection(
      StringDomain.match(context, param, ['aaa']),
      StringDomain.mismatch(context, param, ['aaa']),
      StringDomain.empty(context, param)
    );
  });

  test('substring and substring intersection1', () => {
    intersection(
      StringDomain.substring(context, param, ['aaa', 'bbb']),
      StringDomain.substring(context, param, ['ccc', 'ddd']),
      StringDomain.substring(context, param, ['aaa', 'bbb', 'ccc', 'ddd'])
    );
  });

  test('substring and substring intersection2', () => {
    intersection(
      StringDomain.substring(context, param, ['aaa', 'bbb']),
      StringDomain.substring(context, param, ['aaa', 'ccc']),
      StringDomain.substring(context, param, ['aaa', 'bbb', 'ccc'])
    );
  });

  test('validate not empty value', () => {
    validation(StringDomain.empty(context, param), val('aaa', 'bbb'), 'Пустая область значений');
    validation(
      StringDomain.match(context, param, StringDomain.EMPTY),
      val('aaa', 'bbb'),
      'Значение должно отсутствовать'
    );
    validation(StringDomain.match(context, param, ['aaa', 'bbb']), val('aaa', 'bbb'), '');
    validation(
      StringDomain.match(context, param, ['aaa', 'bbb']),
      val('aaa'),
      'Значение должно быть эквивалентно: aaa; bbb'
    );
    validation(StringDomain.substring(context, param, ['aaa', 'bbb']), val('aaab', 'bbbb'), '');
    validation(
      StringDomain.substring(context, param, ['aaa', 'bbb']),
      val('aadd', 'bbb'),
      'Значение должно содержать подстроки: aaa; bbb'
    );
  });

  test('get values', () => {
    expect(StringDomain.empty(context, param).getValues()).toBeUndefined();
    expect(StringDomain.substring(context, param, ['aaa', 'bbb']).getValues()).toBeUndefined();
    expect(
      commonAssertion(param, StringDomain.match(context, param, StringDomain.EMPTY).getValues()!, ValueType.STRING).map(
        value => getRuLocalizedString(value.stringValue)?.value
      )
    ).toEqual([]);
    expect(
      commonAssertion(param, StringDomain.match(context, param, ['aaa', 'bbb']).getValues()!, ValueType.STRING).map(
        value => getRuLocalizedString(value.stringValue)?.value
      )
    ).toEqual(['aaa', 'bbb']);
  });
});
