import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { PredicateOperation } from '@yandex-market/market-proto-dts/Market/Mbo/Rules/ModelRulePredicate';

import { createModelRuleTester } from 'src/mocks/testUtils';
import { getNumValue, getStringValue } from 'src/utils/test-utils';

const NUMERIC3 = 3;
const NUMERIC5 = 5;
const NUMERIC8 = 8;
const NUMERIC9 = 9;
const NUMERIC10 = 10;
const NUMERIC11 = 11;
const NUMERIC12 = 12;
const NUMERIC15 = 15;
const OPTION_ID3 = 3;

describe('conditions', () => {
  let tester: ReturnType<typeof createModelRuleTester>;
  beforeEach(() => {
    tester = createModelRuleTester();
    tester
      .param({ xslName: 'num1', valueType: ValueType.NUMERIC })
      .param({ xslName: 'num2', valueType: ValueType.NUMERIC })
      .param({ xslName: 'num3', valueType: ValueType.NUMERIC })
      .param({ xslName: 'num4', valueType: ValueType.NUMERIC })
      .param({ xslName: 'str1', valueType: ValueType.STRING, isMultivalue: true })
      .param({ xslName: 'str2', valueType: ValueType.STRING })
      .param({ xslName: 'str3', valueType: ValueType.STRING })
      .param({ xslName: 'str4', valueType: ValueType.STRING })
      .param({ xslName: 'bool1', valueType: ValueType.BOOLEAN }, [
        { id: 1, name: 'TRUE' },
        { id: 2, name: 'FALSE' },
      ])
      .param({ xslName: 'bool2', valueType: ValueType.BOOLEAN }, [
        { id: 1, name: 'TRUE' },
        { id: 2, name: 'FALSE' },
      ])
      .param({ xslName: 'bool3', valueType: ValueType.BOOLEAN }, [
        { id: 1, name: 'TRUE' },
        { id: 2, name: 'FALSE' },
      ])
      .param({ xslName: 'bool4', valueType: ValueType.BOOLEAN }, [
        { id: 1, name: 'TRUE' },
        { id: 2, name: 'FALSE' },
      ])
      .param({ xslName: 'enum1', valueType: ValueType.ENUM }, [
        { id: 1, name: 'Option1' },
        { id: 2, name: 'Option2' },
        { id: 3, name: 'Option3' },
      ])
      .param({ xslName: 'enum2', valueType: ValueType.ENUM }, [
        { id: 1, name: 'Option1' },
        { id: 2, name: 'Option2' },
      ])
      .param({ xslName: 'enum3', valueType: ValueType.ENUM }, [{ id: 1, name: 'Option1' }])
      .param({ xslName: 'enum4', valueType: ValueType.ENUM });
  });

  describe('empty condition', () => {
    test('empty condition success1', () => {
      tester.value('num1').value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('empty condition success2', () => {
      tester.value('enum1').value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('empty multi condition success3', () => {
      tester
        .value('enum1')
        .value('enum1')
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('empty condition fail', () => {
      tester.value('num1', getNumValue(1)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('empty multi condition fail', () => {
      tester
        .value('num1')
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });
  });

  describe('inside range condition', () => {
    test('inside range condition success1', () => {
      tester.value('num1', getNumValue(NUMERIC10)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: NUMERIC5,
            maxValue: NUMERIC15,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('inside range condition success2', () => {
      tester.value('num1', getNumValue(NUMERIC10)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: NUMERIC10,
            maxValue: NUMERIC15,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('inside range condition success3', () => {
      tester.value('num1', getNumValue(NUMERIC10)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: NUMERIC5,
            maxValue: NUMERIC10,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('inside range multi condition success1', () => {
      tester
        .value('num1', getNumValue(NUMERIC8))
        .value('num1', getNumValue(NUMERIC10))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: NUMERIC5,
            maxValue: NUMERIC15,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('inside range multi condition success2', () => {
      tester
        .value('num1', getNumValue(NUMERIC10))
        .value('num1', getNumValue(NUMERIC15))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: NUMERIC10,
            maxValue: NUMERIC15,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('inside range multi condition success3', () => {
      tester
        .value('num1', getNumValue(NUMERIC8))
        .value('num1', getNumValue(NUMERIC10))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: NUMERIC5,
            maxValue: NUMERIC10,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('inside range condition fail1', () => {
      tester.value('num1', getNumValue(NUMERIC10)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: NUMERIC11,
            maxValue: NUMERIC15,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('inside range multi condition fail1', () => {
      tester
        .value('num1', getNumValue(NUMERIC12))
        .value('num1', getNumValue(NUMERIC10))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: NUMERIC11,
            maxValue: NUMERIC15,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('inside range condition fail2', () => {
      tester.value('num1', getNumValue(NUMERIC10)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: 1,
            maxValue: NUMERIC9,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('inside range multi condition fail2', () => {
      tester
        .value('num1', getNumValue(NUMERIC8))
        .value('num1', getNumValue(NUMERIC10))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: 1,
            maxValue: NUMERIC9,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('inside range condition fail3', () => {
      tester.value('num1').value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.INSIDE_RANGE,
            minValue: 1,
            maxValue: NUMERIC9,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });
  });

  describe('javascript condition', () => {
    test('javascript condition success1', () => {
      tester.value('num1', getNumValue(1)).value('num2', getNumValue(2));
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
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('javascript condition success2', () => {
      tester.value('num1', getNumValue(1)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return 'true';`,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('javascript condition success3', () => {
      tester
        .value('num1', getNumValue(NUMERIC10))
        .value('num2', getNumValue(NUMERIC10))
        .value('num3', getNumValue(1));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return val('num1') === val('num2');`,
          },
        ],
        thens: [
          {
            xslName: 'num3',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num3');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('javascript condition success4', () => {
      tester.value('str1', getStringValue('abcd')).value('num3', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return val('str1')[0] === 'abcd';`,
          },
        ],
        thens: [
          {
            xslName: 'num3',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num3');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('javascript condition success5', () => {
      tester
        .value('str1', getStringValue('ab'))
        .value('str1', getStringValue('cd'))
        .value('num3', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `var str1 = val('str1'); return str1[0] === 'ab' && str1[1] === 'cd';`,
          },
        ],
        thens: [
          {
            xslName: 'num3',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num3');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('javascript condition fail1', () => {
      tester.value('str1', getStringValue('abcd')).value('num3', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return false;`,
          },
        ],
        thens: [
          {
            xslName: 'num3',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('javascript condition fail2', () => {
      tester.value('str1', getStringValue('abcd')).value('num3', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return 'false';`,
          },
        ],
        thens: [
          {
            xslName: 'num3',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('javascript condition fail3', () => {
      tester.value('str1', getStringValue('abcd')).value('num3', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return val('str1')[0] === 'abcde';`,
          },
        ],
        thens: [
          {
            xslName: 'num3',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('javascript condition fail4', () => {
      tester
        .value('num1', getNumValue(1))
        .value('num1', getNumValue(2))
        .value('num2', getNumValue(NUMERIC3));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.JAVASCRIPT,
            stringValue: `return val('num1').length;`,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });
  });

  describe('matches condition', () => {
    test('match numeric condition success', () => {
      tester.value('num1', getNumValue(1)).value('num2', getNumValue(2));
      tester.rule({
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
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('match multi numeric condition success', () => {
      tester
        .value('num1', getNumValue(1))
        .value('num1', getNumValue(2))
        .value('num2', getNumValue(NUMERIC3));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.MATCHES,
            minValue: 2,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: 2,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual(['2']);
    });

    test('match numeric condition fail', () => {
      tester.value('num1', getNumValue(1)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('match string condition success', () => {
      tester.value('str1', getStringValue('1')).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.MATCHES,
            stringValue: '1',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('match multi string condition success', () => {
      tester
        .value('str1', getStringValue('ab'))
        .value('str1', getStringValue('bc'))
        .value('str1', getStringValue('cd'))
        .value('str2', getStringValue('f'));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.MATCHES,
            stringValue: 'cd',
          },
        ],
        thens: [
          {
            xslName: 'str2',
            operation: PredicateOperation.MATCHES,
            stringValue: 'g',
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('str2');
      param.strings().toEqual(['g']);
    });

    test('match string condition fail', () => {
      tester.value('str1', getStringValue('1')).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.MATCHES,
            stringValue: '2',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('match multi string condition fail', () => {
      tester
        .value('str1', getStringValue('ab'))
        .value('str1', getStringValue('bc'))
        .value('str1', getStringValue('cd'))
        .value('str2', getStringValue('f'));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.MATCHES,
            stringValue: 'dd',
          },
        ],
        thens: [
          {
            xslName: 'str2',
            operation: PredicateOperation.MATCHES,
            stringValue: 'g',
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('match option condition success1', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MATCHES,
            valueIds: [1],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('match option condition success2', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MATCHES,
            valueIds: [1, 2],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('match multi option condition success', () => {
      tester
        .value('enum1', { optionId: 1 })
        .value('enum1', { optionId: 2 })
        .value('enum1', { optionId: OPTION_ID3 })
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MATCHES,
            valueIds: [1, 2],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual(['1']);
    });

    test('match option condition current multi success', () => {
      tester
        .value('enum1', { optionId: 1 })
        .value('num2', getNumValue(2))
        .value('num2', getNumValue(NUMERIC3));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MATCHES,
            valueIds: [1],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: 1,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual(['1']);
    });

    test('match option condition fail1', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MATCHES,
            valueIds: [2],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('match option condition fail2', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MATCHES,
            valueIds: [2, OPTION_ID3],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });
  });

  describe('mismatches condition', () => {
    test('mismatch numeric condition success1', () => {
      tester.value('num1', getNumValue(1)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.MISMATCHES,
            minValue: 2,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('mismatch numeric condition success2', () => {
      tester.value('num1').value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.MISMATCHES,
            minValue: 2,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('mismatch multi numeric condition success', () => {
      tester
        .value('num1', getNumValue(1))
        .value('num1', getNumValue(NUMERIC3))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.MISMATCHES,
            minValue: 2,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('mismatch numeric condition fail', () => {
      tester.value('num1', getNumValue(1)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.MISMATCHES,
            minValue: 1,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('mismatch multi numeric condition fail', () => {
      tester
        .value('num1', getNumValue(1))
        .value('num1', getNumValue(NUMERIC3))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.MISMATCHES,
            minValue: 1,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('mismatch string condition success', () => {
      tester.value('str1', getStringValue('1')).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.MISMATCHES,
            stringValue: '2',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('mismatch multi string condition success', () => {
      tester
        .value('str1', getStringValue('ab'))
        .value('str1', getStringValue('bc'))
        .value('str1', getStringValue('cd'))
        .value('str2', getStringValue('f'));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.MISMATCHES,
            stringValue: 'dd',
          },
        ],
        thens: [
          {
            xslName: 'str2',
            operation: PredicateOperation.MATCHES,
            stringValue: 'g',
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('str2');
      param.strings().toEqual(['g']);
    });

    test('mismatch string condition fail', () => {
      tester.value('str1', getStringValue('1')).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.MISMATCHES,
            stringValue: '1',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('mismatch multi string condition fail', () => {
      tester
        .value('str1', getStringValue('1'))
        .value('str1', getStringValue('2'))
        .value('str1', getStringValue('3'))
        .value('str1', getStringValue('4'))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.MISMATCHES,
            stringValue: '1',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('mismatch option condition success1', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MISMATCHES,
            valueIds: [2],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('mismatch multi option condition success1', () => {
      tester
        .value('enum1', { optionId: 1 })
        .value('enum1', { optionId: 2 })
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MISMATCHES,
            valueIds: [OPTION_ID3],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('mismatch option condition success2', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MISMATCHES,
            valueIds: [2, OPTION_ID3],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('mismatch option condition fail1', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MISMATCHES,
            valueIds: [1],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('mismatch multi option condition fail', () => {
      tester
        .value('enum1', { optionId: 1 })
        .value('enum1', { optionId: 2 })
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MISMATCHES,
            valueIds: [1],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('mismatch option condition fail2', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.MISMATCHES,
            valueIds: [1, 2],
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });
  });

  describe('not empty condition', () => {
    test('not empty condition success', () => {
      tester.value('num1', getNumValue(1)).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.NOT_EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('not empty multi condition success', () => {
      tester
        .value('num1')
        .value('num1', getNumValue(1))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.NOT_EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('not empty condition fail', () => {
      tester.value('num1').value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.NOT_EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('not empty multi condition fail', () => {
      tester
        .value('num1')
        .value('num1')
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'num1',
            operation: PredicateOperation.NOT_EMPTY,
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });
  });

  describe('substring condition', () => {
    test('on string success1', () => {
      tester.value('str1', getStringValue('abc')).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: 'ab',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('on string success2', () => {
      tester
        .value('str1', getStringValue('klb'))
        .value('str1', getStringValue('abc'))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: 'bc',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('on string success3', () => {
      tester
        .value('str1', getStringValue('klb'))
        .value('str1', getStringValue('abc'))
        .value('str1', getStringValue('def'))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: 'de',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('on string fail1', () => {
      tester
        .value('str1', getStringValue('klb'))
        .value('str1', getStringValue('abc'))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: 'tyu',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('on string fail2', () => {
      tester.value('str1').value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: 'tyu',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('on string fail3', () => {
      tester
        .value('str1', getStringValue('klb'))
        .value('str1', getStringValue('abc'))
        .value('str1', getStringValue('def'))
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'str1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: 'tyu',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('on enum success1', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
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
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('on enum success2', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: '',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('on enum success3', () => {
      tester
        .value('enum1', { optionId: 1 })
        .value('enum1', { optionId: 2 })
        .value('num2', getNumValue(2));
      tester.rule({
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
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num2');
      param.numerics().toEqual([String(NUMERIC3)]);
    });

    test('on enum fail1', () => {
      tester.value('enum1', { optionId: 1 }).value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: 'vrg',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('on enum fail2', () => {
      tester.value('enum1').value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: 'Opt1',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('on enum fail3', () => {
      tester.value('enum1').value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: '',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });

    test('on enum fail4', () => {
      tester
        .value('enum1', { optionId: 1 })
        .value('enum1', { optionId: 2 })
        .value('num2', getNumValue(2));
      tester.rule({
        name: 'Rule 1',
        group: 'Test',
        ifs: [
          {
            xslName: 'enum1',
            operation: PredicateOperation.SUBSTRING,
            stringValue: 'vrg',
          },
        ],
        thens: [
          {
            xslName: 'num2',
            operation: PredicateOperation.MATCHES,
            minValue: NUMERIC3,
          },
        ],
      });

      const results = tester.doInference();
      results.count(0);
      results.iterationCount(1);
    });
  });
});
