import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { PredicateOperation } from '@yandex-market/market-proto-dts/Market/Mbo/Rules/ModelRulePredicate';

import { createModelRuleTester } from 'src/mocks/testUtils';
import { getNumValue, getStringValue } from 'src/utils/test-utils';

describe('complex', () => {
  describe('special tests', () => {
    let tester: ReturnType<typeof createModelRuleTester>;
    beforeEach(() => {
      tester = createModelRuleTester();
      tester
        .param({ xslName: 'a', valueType: ValueType.NUMERIC })
        .param({ xslName: 'b', valueType: ValueType.NUMERIC })
        .param({ xslName: 'c', valueType: ValueType.NUMERIC });
    });

    test('clean and range on empty parameter', () => {
      tester
        .value('a', getNumValue(1))
        .value('b', getNumValue(0))
        .value('c', getNumValue(1));
      tester.rule({
        name: 'Range',
        group: 'Test',
        priority: 10,
        ifs: [
          {
            xslName: 'a',
            operation: PredicateOperation.MATCHES,
            minValue: 5,
          },
        ],
        thens: [
          {
            xslName: 'b',
            operation: PredicateOperation.MATCHES,
            minValue: 2,
          },
        ],
      });
      tester.rule({
        name: 'Range',
        group: 'Test',
        priority: 5,
        ifs: [
          {
            xslName: 'a',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
        thens: [
          {
            xslName: 'b',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
      });
      tester.rule({
        name: 'Range',
        group: 'Test',
        priority: 10,
        ifs: [
          {
            xslName: 'c',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
        thens: [
          {
            xslName: 'a',
            operation: PredicateOperation.MATCHES,
            minValue: 5,
          },
        ],
      });
      const results = tester.doInference();
      results.count(2);
      results.iterationCount(3);

      const a = results.itemAssertions('a');
      a.valid();
      a.modified();
      a.numerics().toEqual(['5']);

      const b = results.itemAssertions('b');
      b.valid();
      b.modified();
      b.numerics().toEqual(['2']);
    });
  });

  describe('javascript rules', () => {
    let tester: ReturnType<typeof createModelRuleTester>;
    beforeEach(() => {
      tester = createModelRuleTester();
      tester
        .param({ xslName: 'num1', valueType: ValueType.NUMERIC })
        .param({ xslName: 'num2', valueType: ValueType.NUMERIC })
        .param({ xslName: 'num3', valueType: ValueType.NUMERIC })
        .param({ xslName: 'num4', valueType: ValueType.NUMERIC })
        .param({ xslName: 'str2', valueType: ValueType.STRING, isMultivalue: true })
        .param({ xslName: 'str3', valueType: ValueType.STRING })
        .param({ xslName: 'bool1', valueType: ValueType.BOOLEAN }, [
          { id: 1, name: 'TRUE' },
          { id: 2, name: 'FALSE' },
        ]);
    });

    test('javascript rules', () => {
      tester
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(2))
        .value('num3', getNumValue(3));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return true;`,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: -2,
          },
        ],
      });
      tester.rule({
        name: 'Rule 2',
        group: 'Test',
        ifs: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: -2,
          },
        ],
        thens: [
          {
            xslName: 'num3',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return -3;`,
          },
        ],
      });

      const results = tester.doInference();
      results.count(2);
      results.iterationCount(2);

      const num2 = results.itemAssertions('num2');
      num2.numerics().toEqual(['-2']);

      const num3 = results.itemAssertions('num3');
      num3.numerics().toEqual(['-3']);
    });

    test('java script rules with dependent parameters', () => {
      // тест удаляет из str2 все значения по одному, пока они там есть
      // как только количество значений в str2 становится равным 2, то выставляется флаг bool1 === true
      // как только bool1 === true, str3 = val('str2')[0]
      tester
        .value('bool1', { booleanValue: false, optionId: 2 })
        .value('str2', getStringValue('aaa'))
        .value('str2', getStringValue('bbb'))
        .value('str2', getStringValue('ccc'))
        .value('str3');
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str2',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `var array = val('str2'); return array && array.length > 0 || false;`,
          },
        ],
        thens: [
          {
            xslName: 'str2',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `var array = val('str2'); array.shift(); return array;`,
          },
          {
            xslName: 'bool1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `var array = val('str2'); return array && array.length === 2 || false;`,
          },
        ],
      });
      tester.rule({
        name: 'Rule 2',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return val('bool1');`,
          },
        ],
        thens: [
          {
            xslName: 'str3',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return val('str2')[0];`,
          },
        ],
      });

      const results = tester.doInference();
      results.count(3);
      results.iterationCount(4);

      const str2 = results.itemAssertions('str2');
      str2.isEmpty();

      const str3 = results.itemAssertions('str3');
      str3.strings().toEqual(['bbb']);

      const bool1 = results.itemAssertions('bool1');
      bool1.booleans().toEqual([false]);
    });
  });

  describe('rules chaining', () => {
    let tester: ReturnType<typeof createModelRuleTester>;
    beforeEach(() => {
      tester = createModelRuleTester();
      tester
        .param({ xslName: 'param1', valueType: ValueType.NUMERIC })
        .param({ xslName: 'param2', valueType: ValueType.NUMERIC })
        .param({ xslName: 'param3', valueType: ValueType.NUMERIC })
        .param({ xslName: 'param4', valueType: ValueType.NUMERIC });
    });

    test('two rules chaining', () => {
      tester
        .value('param1', getNumValue(1))
        .value('param2', getNumValue(2))
        .value('param3', getNumValue(3));
      tester.rule({
        name: 'First range',
        group: 'Test',
        ifs: [
          {
            xslName: 'param1',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
        thens: [
          {
            xslName: 'param2',
            operation: PredicateOperation.MATCHES,
            minValue: 4,
          },
        ],
      });
      tester.rule({
        name: 'param2',
        group: 'Test',
        ifs: [
          {
            xslName: 'param2',
            operation: PredicateOperation.MATCHES,
            minValue: 4,
          },
        ],
        thens: [
          {
            xslName: 'param3',
            operation: PredicateOperation.MATCHES,
            minValue: 5,
          },
        ],
      });
      const results = tester.doInference();
      results.count(2);
      results.iterationCount(2);

      const param2 = results.itemAssertions('param2');
      param2.valid();
      param2.modified();
      param2.numerics().toEqual(['4']);

      const param3 = results.itemAssertions('param3');
      param3.valid();
      param3.modified();
      param3.numerics().toEqual(['5']);
    });

    test('two rules chaining back order', () => {
      tester
        .value('param1', getNumValue(1))
        .value('param2', getNumValue(2))
        .value('param3', getNumValue(3));

      tester.rule({
        name: 'param2',
        group: 'Test',
        ifs: [
          {
            xslName: 'param2',
            operation: PredicateOperation.MATCHES,
            minValue: 4,
          },
        ],
        thens: [
          {
            xslName: 'param3',
            operation: PredicateOperation.MATCHES,
            minValue: 5,
          },
        ],
      });
      tester.rule({
        name: 'First range',
        group: 'Test',
        ifs: [
          {
            xslName: 'param1',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
        thens: [
          {
            xslName: 'param2',
            operation: PredicateOperation.MATCHES,
            minValue: 4,
          },
        ],
      });

      const results = tester.doInference();
      results.count(2);
      results.iterationCount(3);

      const param2 = results.itemAssertions('param2');
      param2.valid();
      param2.modified();
      param2.numerics().toEqual(['4']);

      const param3 = results.itemAssertions('param3');
      param3.valid();
      param3.modified();
      param3.numerics().toEqual(['5']);
    });

    test('three rules chaining', () => {
      tester
        .value('param1', getNumValue(1))
        .value('param2', getNumValue(2))
        .value('param3', getNumValue(3))
        .value('param4', getNumValue(4));

      tester.rule({
        name: 'First range',
        group: 'Test',
        ifs: [
          {
            xslName: 'param1',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
        thens: [
          {
            xslName: 'param2',
            operation: PredicateOperation.MATCHES,
            minValue: 4,
          },
        ],
      });
      tester.rule({
        name: 'param2',
        group: 'Test',
        ifs: [
          {
            xslName: 'param2',
            operation: PredicateOperation.MATCHES,
            minValue: 4,
          },
        ],
        thens: [
          {
            xslName: 'param3',
            operation: PredicateOperation.MATCHES,
            minValue: 5,
          },
        ],
      });
      tester.rule({
        name: 'param2',
        group: 'Test',
        ifs: [
          {
            xslName: 'param3',
            operation: PredicateOperation.MATCHES,
            minValue: 5,
          },
        ],
        thens: [
          {
            xslName: 'param4',
            operation: PredicateOperation.MATCHES,
            minValue: 6,
          },
        ],
      });

      const results = tester.doInference();
      results.count(3);
      results.iterationCount(2);

      const param2 = results.itemAssertions('param2');
      param2.valid();
      param2.modified();
      param2.numerics().toEqual(['4']);

      const param3 = results.itemAssertions('param3');
      param3.valid();
      param3.modified();
      param3.numerics().toEqual(['5']);

      const param4 = results.itemAssertions('param4');
      param4.valid();
      param4.modified();
      param4.numerics().toEqual(['6']);
    });

    test('three rules chaining back order', () => {
      tester
        .value('param1', getNumValue(1))
        .value('param2', getNumValue(2))
        .value('param3', getNumValue(3))
        .value('param4', getNumValue(4));

      tester.rule({
        name: 'param2',
        group: 'Test',
        ifs: [
          {
            xslName: 'param3',
            operation: PredicateOperation.MATCHES,
            minValue: 5,
          },
        ],
        thens: [
          {
            xslName: 'param4',
            operation: PredicateOperation.MATCHES,
            minValue: 6,
          },
        ],
      });
      tester.rule({
        name: 'param2',
        group: 'Test',
        ifs: [
          {
            xslName: 'param2',
            operation: PredicateOperation.MATCHES,
            minValue: 4,
          },
        ],
        thens: [
          {
            xslName: 'param3',
            operation: PredicateOperation.MATCHES,
            minValue: 5,
          },
        ],
      });
      tester.rule({
        name: 'First range',
        group: 'Test',
        ifs: [
          {
            xslName: 'param1',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
        thens: [
          {
            xslName: 'param2',
            operation: PredicateOperation.MATCHES,
            minValue: 4,
          },
        ],
      });

      const results = tester.doInference();
      results.count(3);
      results.iterationCount(4);

      const param2 = results.itemAssertions('param2');
      param2.valid();
      param2.modified();
      param2.numerics().toEqual(['4']);

      const param3 = results.itemAssertions('param3');
      param3.valid();
      param3.modified();
      param3.numerics().toEqual(['5']);

      const param4 = results.itemAssertions('param4');
      param4.valid();
      param4.modified();
      param4.numerics().toEqual(['6']);
    });

    test('no npe', () => {
      tester
        .value('param1', getNumValue(1))
        .value('param2', getNumValue(2))
        .value('param3', getNumValue(3));

      tester.rule({
        name: 'First range',
        group: 'Test',
        ifs: [
          {
            xslName: 'param1',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
        thens: [
          {
            xslName: 'param3',
            operation: PredicateOperation.MATCHES,
            minValue: 4,
          },
        ],
      });

      tester.rule({
        name: 'param2',
        group: 'Test',
        ifs: [
          {
            xslName: 'param2',
            operation: PredicateOperation.MATCHES,
            minValue: 2,
          },
        ],
        thens: [
          {
            xslName: 'param1',
            operation: PredicateOperation.MATCHES,
            minValue: 5,
          },
        ],
      });

      const results = tester.doInference();
      results.count(2);
      results.iterationCount(2);

      const param1 = results.itemAssertions('param1');
      param1.valid();
      param1.modified();
      param1.numerics().toEqual(['5']);

      const param3 = results.itemAssertions('param3');
      param3.valid();
      param3.modified();
      param3.numerics().toEqual(['4']);
    });
  });

  describe('model values', () => {
    let tester: ReturnType<typeof createModelRuleTester>;
    beforeEach(() => {
      tester = createModelRuleTester();
      tester
        .param({ xslName: 'str1', valueType: ValueType.STRING })
        .param({ xslName: 'str2', valueType: ValueType.STRING })
        .param({ xslName: 'enum1', valueType: ValueType.ENUM }, [{ id: 100, name: 'Option 100' }])
        .param({ xslName: 'bool1', valueType: ValueType.BOOLEAN }, [
          { id: 200, name: 'TRUE' },
          { id: 300, name: 'FALSE' },
        ]);
    });

    /**
     * @issue CONTENTLAB-307
     */
    test('skip invalid options in parameter values', () => {
      tester
        .value('str1', getStringValue('aaa1'))
        .value('str2', getStringValue('aaa2'))
        .value('enum1', { optionId: 1000 })
        .value('bool1', getNumValue(2000));

      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.NOT_EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'str1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return 'bbb1';`,
          },
        ],
      });
      tester.rule({
        name: 'Rule 2',
        group: 'Test',
        ifs: [
          {
            xslName: 'bool1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'str2',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return 'bbb2';`,
          },
        ],
      });

      const results = tester.doInference();

      results
        .itemAssertions('str1')
        .strings()
        .toEqual(['bbb1']);
      results
        .itemAssertions('str2')
        .strings()
        .toEqual(['bbb2']);
    });
  });
});
