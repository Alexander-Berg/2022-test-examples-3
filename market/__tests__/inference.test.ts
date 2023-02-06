import { PredicateOperation } from '@yandex-market/market-proto-dts/Market/Mbo/Rules/ModelRulePredicate';

import { getInferenceTestData } from 'src/mocks/testUtils';
import { XslNames } from 'src/XslNames';
import { getNumValue, getStringValue } from 'src/utils/test-utils';

describe('inference', () => {
  describe('add values inference', () => {
    test('add enum', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('enum1', { optionId: 7 })
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.ADD_VALUE,
              valueIds: [8, 9],
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum1 = results.itemAssertions('enum1');
      enum1.valid();
      enum1.optionIds().toEqual([7, 8, 9]);
    });

    test('add string', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('str1', getStringValue('original-1'))
        .value('str1', getStringValue('original-2'))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'str1',
              operation: PredicateOperation.ADD_VALUE,
              stringValue: 'added-1',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str1 = results.itemAssertions('str1');
      str1.valid();
      str1.strings().toEqual(['original-1', 'original-2', 'added-1']);
    });
  });

  describe('empty inference', () => {
    test('on empty numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.EMPTY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.isEmpty();
    });

    test('on not empty numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(2))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.EMPTY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.modified();
      num2.isEmpty();
    });

    test('on empty string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('aaa'))
        .value('str2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'aaa',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.EMPTY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.isEmpty();
    });

    test('on not empty string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('aaa'))
        .value('str2', getStringValue('bbb'))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'aaa',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.EMPTY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.modified();
      str2.isEmpty();
    });

    test('on empty enum', () => {
      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.EMPTY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.isEmpty();
    });

    test('on not empty enum', () => {
      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2', { optionId: 8 })
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.EMPTY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.modified();
      enum2.isEmpty();
    });
  });

  describe('inside range inference', () => {
    const NUMERIC10 = 10;
    const NUMERIC20 = 20;

    test('range on empty numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC10,
              maxValue: NUMERIC20,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.shouldUndefined();
    });

    test('single on empty numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC10,
              maxValue: NUMERIC10,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.modified();
      num2.numerics().toEqual([String(NUMERIC10)]);
    });

    test('range on valid numeric', () => {
      const num2Val = 15;

      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(num2Val))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC10,
              maxValue: NUMERIC20,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.shouldUndefined();
    });

    test('range on invalid numeric', () => {
      const num2Val = 30;

      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(num2Val))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC10,
              maxValue: NUMERIC20,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.invalid();
      num2.shouldUndefined();
    });

    test('single on equal numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(NUMERIC10))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC10,
              maxValue: NUMERIC10,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.numerics().toEqual([String(NUMERIC10)]);
    });
  });

  describe('javascript dependent parameters', () => {
    test('numeric dependency', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(2))
        .value('num3')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num3',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: 'return val("num2");',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num3 = results.itemAssertions('num3');
      num3.valid();
      num3.modified();
      num3.numerics().toEqual(['2']);
    });

    test('single string dependency', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2', getStringValue('def'))
        .value('str3')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str3',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: 'return val("str2");',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str3 = results.itemAssertions('str3');
      str3.valid();
      str3.modified();
      str3.strings().toEqual(['def']);
    });

    test('string array dependency', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2', getStringValue('def'))
        .value('str2', getStringValue('ldc'))
        .value('str2', getStringValue('edc'))
        .value('str3')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str3',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return val('str2');`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str3 = results.itemAssertions('str3');
      str3.valid();
      str3.modified();
      str3.strings().toEqual(['def', 'ldc', 'edc']);
    });

    test('enum dependency', () => {
      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2', { optionId: 7 })
        .value('enum3')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7, 8],
            },
          ],
          thens: [
            {
              xslName: 'enum3',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: 'return val("enum2");',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum3 = results.itemAssertions('enum3');
      enum3.valid();
      enum3.modified();
      enum3.optionIds().toEqual([7]);
    });

    test('bool dependency true', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2', { booleanValue: true, optionId: 1 })
        .value('bool3')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool3',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: 'return val("bool2");',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool3 = results.itemAssertions('bool3');
      bool3.valid();
      bool3.modified();
      bool3.booleans().toEqual([true]);
      bool3.optionIds().toEqual([1]);
    });

    test('bool dependency false', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2', { booleanValue: false, optionId: 2 })
        .value('bool3')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool3',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: 'return val("bool2");',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool3 = results.itemAssertions('bool3');
      bool3.valid();
      bool3.modified();
      bool3.booleans().toEqual([false]);
      bool3.optionIds().toEqual([2]);
    });
  });

  describe('javascript error inference', () => {
    test('bool null inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'bool2',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return null;`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          'Ожидалось: логический тип JavaScript или регистронезависимая строка, содержащая true или false. Пришло: null значение.'
        );
      }
    });

    test('bool string inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'bool2',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return 'abracadabra';`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          `Ожидалось: логический тип JavaScript или регистронезависимая строка, содержащая true или false. Пришло: строка "abracadabra", отличающаяся от true и false.`
        );
      }
    });

    test('bool array inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'bool2',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return ['abracadabra'];`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          `Ожидалось: логический тип JavaScript или регистронезависимая строка, содержащая true или false. Пришло: значение ["abracadabra"].`
        );
      }
    });

    test('numeric null inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return null;`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          `Ожидалось: число или строка, которую можно преобразовать в число. Пришло: null значение.`
        );
      }
    });

    test('numeric string inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return 'abracadabra';`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          `Ожидалось: число или строка, которую можно преобразовать в число. Пришло: строка "abracadabra", которую невозможно преобразовать в число.`
        );
      }
    });

    test('numeric array inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return ['abracadabra'];`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          `Ожидалось: число или строка, которую можно преобразовать в число. Пришло: значение ["abracadabra"].`
        );
      }
    });

    test('string null inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'str1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return null;`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(`Ожидалось: строка или массив не null строк. Пришло: null значение.`);
      }
    });

    test('string array with null values inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'str1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return ['abracadabra', null];`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          `Ожидалось: строка или массив не null строк. Пришло: массив ["abracadabra", null], содержащий не строковое значение значение.`
        );
      }
    });

    test('enum null inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return null;`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(`Ожидалось: непустая строка, содержащая название опции. Пришло: null значение.`);
      }
    });

    test('enum string inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return '';`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          `Ожидалось: непустая строка, содержащая название опции. Пришло: пустая или состоящая из пробелов строка.`
        );
      }
    });

    test('enum whitespace string inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return '  ';`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          `Ожидалось: непустая строка, содержащая название опции. Пришло: пустая или состоящая из пробелов строка.`
        );
      }
    });

    test('enum array inference', () => {
      const testCase = getInferenceTestData().rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return ['abracadabra'];`,
          },
        ],
      });

      try {
        testCase.doInference();
        throw new Error('"Expecting rules applying to fail');
      } catch (e) {
        expect(e.message).toBe(
          `Ожидалось: непустая строка, содержащая название опции. Пришло: значение ["abracadabra"].`
        );
      }
    });
  });

  describe('javascript inference', () => {
    const NUMERIC10 = 10;
    const NUMERIC20 = 20;

    test('on empty numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 10;`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.modified();
      num2.numerics().toEqual([String(NUMERIC10)]);
    });

    test('on valid numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(NUMERIC10))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 10;`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.numerics().toEqual([String(NUMERIC10)]);
    });

    test('on invalid numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(NUMERIC20))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 10;`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.modified();
      num2.numerics().toEqual([String(NUMERIC10)]);
    });

    test('on empty string single string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 'cba'`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.modified();
      str2.strings().toEqual(['cba']);
    });

    test('on empty string string array', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return ['cba', 'cde', 'abc']`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.modified();
      str2.strings().toEqual(['cba', 'cde', 'abc']);
    });

    test('on empty enum and valid ret value', () => {
      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 'Option2'`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.modified();
      enum2.optionIds().toEqual([8]);
    });

    test('on empty enum and empty ret value', () => {
      const testCase = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return ''`,
            },
          ],
        });

      expect(() => testCase.doInference()).toThrow();
    });

    test('on empty enum and illegal ret value', () => {
      const testCase = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 'Not existing option'`,
            },
          ],
        });

      expect(() => testCase.doInference()).toThrow();
    });

    test('on empty boolean and ret value true1', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return true;`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([true]);
      bool2.optionIds().toEqual([3]);
    });

    test('on empty boolean and ret value true2', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 'true';`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([true]);
      bool2.optionIds().toEqual([3]);
    });

    test('on empty boolean and ret value true3', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 'True';`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([true]);
      bool2.optionIds().toEqual([3]);
    });

    test('on empty boolean and ret value false1', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return false;`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([false]);
      bool2.optionIds().toEqual([4]);
    });

    test('on empty boolean and ret value false2', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 'false';`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([false]);
      bool2.optionIds().toEqual([4]);
    });

    test('on empty boolean and ret value false2', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return 'False';`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([false]);
      bool2.optionIds().toEqual([4]);
    });
  });

  describe('javascript inference with special js params', () => {
    test.skip('is modification for model', () => {
      const results = getInferenceTestData()
        .value('bool1')
        .value('bool2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return val('@is_modification');`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([false]);
      bool2.optionIds().toEqual([2]);
    });

    test.skip('is modification for modification', () => {
      const results = getInferenceTestData()
        .value('bool1')
        .value('bool2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.JAVASCRIPT,
              stringValue: `return val('@is_modification');`,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([true]);
      bool2.optionIds().toEqual([1]);
    });
  });

  describe('mandatory inference', () => {
    test('on empty string parameter and not signed model', () => {
      const results = getInferenceTestData()
        .value(XslNames.OPERATOR_SIGN, { booleanValue: false, optionId: 2 })
        .value('str1', getStringValue('abc'))
        .value('str2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.shouldUndefined();
      str2.message('');
    });

    test('on empty string parameter and signed model', () => {
      const results = getInferenceTestData()
        .value(XslNames.OPERATOR_SIGN, { booleanValue: true, optionId: 1 })
        .value('str1', getStringValue('abc'))
        .value('str2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.invalid();
      str2.shouldUndefined();
      str2.message('Значение не должно быть пустым');
    });

    test('on not empty string parameter and signed model', () => {
      const results = getInferenceTestData()
        .value(XslNames.OPERATOR_SIGN, { booleanValue: true, optionId: 1 })
        .value('str1', getStringValue('abc'))
        .value('str2', getStringValue('cde'))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.shouldUndefined();
      str2.message('');
    });

    test('on empty numeric parameter and not signed model', () => {
      const results = getInferenceTestData()
        .value(XslNames.OPERATOR_SIGN, { booleanValue: false, optionId: 2 })
        .value('num1', getNumValue(1))
        .value('num2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.shouldUndefined();
      num2.message('');
    });

    test('on empty numeric parameter and signed model', () => {
      const results = getInferenceTestData()
        .value(XslNames.OPERATOR_SIGN, { booleanValue: true, optionId: 1 })
        .value('num1', getNumValue(1))
        .value('num2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.invalid();
      num2.shouldUndefined();
      num2.message('Значение не должно быть пустым');
    });

    test('on not empty numeric parameter and signed model', () => {
      const results = getInferenceTestData()
        .value(XslNames.OPERATOR_SIGN, { booleanValue: true, optionId: 1 })
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(2))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.shouldUndefined();
      num2.message('');
    });

    test('on empty enum parameter and not signed model', () => {
      const results = getInferenceTestData()
        .value(XslNames.OPERATOR_SIGN, { booleanValue: false, optionId: 2 })
        .value('enum1', { optionId: 7 })
        .value('enum2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.shouldUndefined();
      enum2.message('');
    });

    test('on empty enum parameter and no signed model', () => {
      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.shouldUndefined();
      enum2.message('');
    });

    test('on empty enum parameter and signed model', () => {
      const results = getInferenceTestData()
        .value(XslNames.OPERATOR_SIGN, { booleanValue: true, optionId: 1 })
        .value('enum1', { optionId: 7 })
        .value('enum2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.invalid();
      enum2.shouldUndefined();
      enum2.message('Значение должно быть из списка: Option1; Option2');
    });

    test('on not empty enum parameter and signed model', () => {
      const results = getInferenceTestData()
        .value(XslNames.OPERATOR_SIGN, { booleanValue: true, optionId: 1 })
        .value('enum1', { optionId: 7 })
        .value('enum2', { optionId: 8 })
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.MANDATORY,
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.shouldUndefined();
      enum2.message('');
    });
  });

  describe('match inference', () => {
    test('on empty numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.MATCHES,
              minValue: 2,
            },
          ],
        })
        .doInference();

      results.count(1);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.modified();
      num2.numerics().toEqual(['2']);
    });

    test('on valid numeric', () => {
      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(2))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.MATCHES,
              minValue: 2,
            },
          ],
        })
        .doInference();

      results.count(1);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.numerics().toEqual(['2']);
    });

    test('on invalid numeric', () => {
      const num2Val = 4;

      const results = getInferenceTestData()
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(num2Val))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'num1',
              operation: PredicateOperation.MATCHES,
              minValue: 1,
            },
          ],
          thens: [
            {
              xslName: 'num2',
              operation: PredicateOperation.MATCHES,
              minValue: 2,
            },
          ],
        })
        .doInference();

      results.count(1);

      const num2 = results.itemAssertions('num2');
      num2.valid();
      num2.modified();
      num2.numerics().toEqual(['2']);
    });

    test('on empty string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.MATCHES,
              stringValue: 'cde',
            },
          ],
        })
        .doInference();

      results.count(1);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.modified();
      str2.strings().toEqual(['cde']);
    });

    test('on valid string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2', getStringValue('cde'))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.MATCHES,
              stringValue: 'cde',
            },
          ],
        })
        .doInference();

      results.count(1);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.strings().toEqual(['cde']);
    });

    test('on invalid string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2', getStringValue('rtd'))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.MATCHES,
              stringValue: 'cde',
            },
          ],
        })
        .doInference();

      results.count(1);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.modified();
      str2.strings().toEqual(['cde']);
    });

    test('on empty enum', () => {
      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.MATCHES,
              valueIds: [7, 8],
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.shouldUndefined();
    });

    test('on valid enum', () => {
      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2', { optionId: 8 })
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.MATCHES,
              valueIds: [7, 8],
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.shouldUndefined();
    });

    test('on invalid enum', () => {
      const enum2Val = 9;

      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2', { optionId: enum2Val })
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.MATCHES,
              valueIds: [7],
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.MATCHES,
              valueIds: [7, 8],
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.invalid();
      enum2.shouldUndefined();
    });

    test('on empty boolean', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.MATCHES,
              valueIds: [3],
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([true]);
      bool2.optionIds().toEqual([3]);
    });

    test('on valid boolean', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2', { booleanValue: true, optionId: 3 })
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.MATCHES,
              valueIds: [3],
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.booleans().toEqual([true]);
      bool2.optionIds().toEqual([3]);
    });

    test('on invalid boolean', () => {
      const results = getInferenceTestData()
        .value('bool1', { booleanValue: true, optionId: 1 })
        .value('bool2', { booleanValue: true, optionId: 3 })
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'bool1',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
          thens: [
            {
              xslName: 'bool2',
              operation: PredicateOperation.MATCHES,
              valueIds: [4],
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const bool2 = results.itemAssertions('bool2');
      bool2.valid();
      bool2.modified();
      bool2.booleans().toEqual([false]);
      bool2.optionIds().toEqual([4]);
    });
  });

  describe('substring inference', () => {
    test('on empty string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'cde',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.shouldUndefined();
    });

    test('single substring on valid single string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2', getStringValue('abcde1'))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'cde',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.shouldUndefined();
    });

    test('substring on valid multiple string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2', getStringValue('abcde'))
        .value('str2', getStringValue('fhty'))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'ht',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.valid();
      str2.shouldUndefined();
    });

    test('substring on invalid single string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2', getStringValue('abcde'))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'rtf',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.invalid();
      str2.shouldUndefined();
    });

    test('substring on invalid multiple string', () => {
      const results = getInferenceTestData()
        .value('str1', getStringValue('abc'))
        .value('str2', getStringValue('abcde'))
        .value('str2', getStringValue('fhty'))
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'str1',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'str2',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'rtf',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const str2 = results.itemAssertions('str2');
      str2.invalid();
      str2.shouldUndefined();
    });

    test('on enum1', () => {
      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2')
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'Opt',
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'Opt',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.shouldUndefined();
    });

    test('on enum2', () => {
      const results = getInferenceTestData()
        .value('enum1', { optionId: 7 })
        .value('enum2', { optionId: 8 })
        .rule({
          name: 'Rule 1',
          group: 'Test',
          ifs: [
            {
              xslName: 'enum1',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'Opt',
            },
          ],
          thens: [
            {
              xslName: 'enum2',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'Option2',
            },
          ],
        })
        .doInference();

      results.count(1);
      results.iterationCount(2);

      const enum2 = results.itemAssertions('enum2');
      enum2.valid();
      enum2.optionIds().toEqual([8]);
    });
  });
});
