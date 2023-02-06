import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { PredicateOperation } from '@yandex-market/market-proto-dts/Market/Mbo/Rules/ModelRulePredicate';

import { createModelRuleTester } from '../mocks/testUtils';
import { getNumValue, getStringValue } from 'src/utils/test-utils';

describe('intersections', () => {
  describe('enum intersections', () => {
    const OPTION_ID3 = 3;
    const OPTION_ID4 = 4;
    const FIFTEEN_THOUSANDS = 15000;
    const NUMERIC2200 = 2200;
    const TEN_THOUSANDS = 10000;
    const TWENTY_THOUSANDS = 20000;

    let tester: ReturnType<typeof createModelRuleTester>;
    beforeEach(() => {
      tester = createModelRuleTester();
      tester
        .param({ xslName: 'fighting', valueType: ValueType.NUMERIC })
        .param({ xslName: 'combat_level', valueType: ValueType.NUMERIC })
        .param({ xslName: 'nation', valueType: ValueType.ENUM }, [
          { id: 1, name: 'USSR' },
          { id: 2, name: 'Germany' },
          { id: OPTION_ID3, name: 'Japan' },
          { id: OPTION_ID4, name: 'USA' },
        ]);
    });

    test('one and two intersection on empty parameter', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation');

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2],
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.modified();
      param.optionIds().toEqual([1]);
    });

    test('two and three intersection on empty parameter', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation');

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2, OPTION_ID3],
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2],
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.shouldUndefined();
    });

    test('clean and list intersection on empty parameter', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation');

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2, OPTION_ID3],
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.shouldUndefined();
    });

    test('clean and list intersection on empty parameter fix conflict1', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation');

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          priority: 1,
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2, OPTION_ID3],
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          priority: 2,
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.maxFailedPriority(1);
      param.isEmpty();
    });

    test('clean and list intersection on empty parameter fix conflict2', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation');

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          priority: 2,
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2, OPTION_ID3],
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          priority: 1,
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.maxFailedPriority(1);
      param.shouldUndefined();
    });

    test('two and three intersection on valid parameter', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation', { optionId: 1 });

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2, OPTION_ID3],
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2],
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.shouldUndefined();
    });

    test('two and three intersection on invalid parameter', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation', { optionId: OPTION_ID3 });

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2, OPTION_ID3],
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2],
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.invalid();
      param.shouldUndefined();
    });

    test('one and two intersection on valid parameter', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation', { optionId: 1 });

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2],
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.optionIds().toEqual([1]);
    });

    test('one and two intersection on invalid parameter', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation', { optionId: OPTION_ID3 });

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1, 2],
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [1],
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.modified();
      param.optionIds().toEqual([1]);
    });

    test('substring intersections1', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation', { optionId: OPTION_ID3 });

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'apa',
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.MATCHES,
              valueIds: [OPTION_ID3, OPTION_ID4],
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.optionIds().toEqual([OPTION_ID3]);
    });

    test('substring intersections2', () => {
      tester
        .value('fighting', getNumValue(FIFTEEN_THOUSANDS))
        .value('combat_level', getNumValue(NUMERIC2200))
        .value('nation', { optionId: 2 });

      tester
        .rule({
          group: 'Test',
          name: 'Nation from fighting',
          ifs: [
            {
              xslName: 'fighting',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: TEN_THOUSANDS,
              maxValue: TWENTY_THOUSANDS,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'an',
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Nation from combat level',
          ifs: [
            {
              xslName: 'combat_level',
              operation: PredicateOperation.NOT_EMPTY,
            },
          ],
          thens: [
            {
              xslName: 'nation',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'Germ',
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('nation');
      param.valid();
      param.notModified();
      param.optionIds().toEqual([2]);
    });
  });

  describe('string intersections', () => {
    let tester: ReturnType<typeof createModelRuleTester>;
    beforeEach(() => {
      tester = createModelRuleTester();
      tester
        .param({ xslName: 'strIn', valueType: ValueType.STRING })
        .param({ xslName: 'strOut', valueType: ValueType.STRING });
    });

    test('substring intersection on empty parameter', () => {
      tester.value('strIn', getStringValue('abc')).value('strOut');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'strIn',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'strOut',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'cc',
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'strIn',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'strOut',
              operation: PredicateOperation.SUBSTRING,
              stringValue: 'ddd',
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('strOut');
      param.valid();
      param.shouldUndefined();
    });
  });

  describe('numeric intersections', () => {
    const NUMERIC50 = 50;
    const NUMERIC150 = 150;
    const NUMERIC70 = 70;
    const NUMERIC180 = 180;
    const NUMERIC90 = 90;
    const NUMERIC110 = 110;
    const NUMERIC200 = 200;
    const NUMERIC10 = 10;
    const NUMERIC1000 = 1000;

    let tester: ReturnType<typeof createModelRuleTester>;
    beforeEach(() => {
      tester = createModelRuleTester();
      tester
        .param({ xslName: 'str', valueType: ValueType.STRING })
        .param({ xslName: 'num', valueType: ValueType.NUMERIC });
    });

    test('two ranges intersection on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC70,
              maxValue: NUMERIC180,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
    });

    test('three ranges intersection on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC70,
              maxValue: NUMERIC180,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Third range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC90,
              maxValue: 100,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
    });

    test('two equal match on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First match',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second match',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.modified();
      param.numerics().toEqual(['100']);
    });

    test('clean and match on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Match',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
      param.conflict();
    });

    test('clean and match on empty parameter fix conflict1', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Match',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.modified();
      param.maxFailedPriority(1);
      param.numerics().toEqual(['100']);
    });

    test('clean and match on empty parameter fix conflict2', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Match',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.maxFailedPriority(1);
      param.isEmpty();
    });

    test('clean and range on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
      param.conflict();
    });

    test('clean and range on empty parameter fix conflict1', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Range',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.maxFailedPriority(1);
      param.isEmpty();
    });

    test('clean and range on empty parameter fix conflict2', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Range',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.maxFailedPriority(1);
      param.shouldUndefined();
    });

    test('clean and match and range on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Match',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
      param.conflict();
    });

    test('clean and match and range on empty parameter fix conflict1', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Range',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Match',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.maxFailedPriority(1);
      param.isEmpty();
    });

    test('clean and match and range on empty parameter fix conflict2', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Range',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Match',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.modified();
      param.maxFailedPriority(1);
      param.numerics().toEqual(['100']);
    });

    test('clean and match and range on empty parameter fix conflict3', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'Range',
          priority: 3,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Match',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Clean',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.modified();
      param.maxFailedPriority(1);
      param.numerics().toEqual(['100']);
    });

    test('two range and one match intersection on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC70,
              maxValue: NUMERIC180,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Third range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.modified();
      param.numerics().toEqual(['100']);
    });

    test('two ranges intersection on valid parameter', () => {
      tester.value('str', getStringValue('abc')).value('num', getNumValue(100));

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC70,
              maxValue: NUMERIC180,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
    });

    test('three ranges intersection on valid parameter', () => {
      tester.value('str', getStringValue('abc')).value('num', getNumValue(100));

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC70,
              maxValue: NUMERIC180,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Third range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC90,
              maxValue: NUMERIC110,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
    });

    test('range and match intersection on valid parameter', () => {
      tester.value('str', getStringValue('abc')).value('num', getNumValue(100));

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.numerics().toEqual(['100']);
    });

    test('two ranges intersection on invalid parameter', () => {
      tester.value('str', getStringValue('abc')).value('num', getNumValue(300));

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC70,
              maxValue: NUMERIC180,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.invalid();
      param.shouldUndefined();
    });

    test('two ranges conflict on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC150,
              maxValue: NUMERIC200,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
      param.conflict();
    });

    test('two ranges conflict on empty parameter fix conflict', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC150,
              maxValue: NUMERIC200,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.maxFailedPriority(1);
      param.shouldUndefined();
    });

    test('two ranges conflict on not empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num', getNumValue(NUMERIC1000));

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC150,
              maxValue: NUMERIC200,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
      param.conflict();
    });

    test('three ranges conflict on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC150,
              maxValue: NUMERIC200,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Third range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC10,
              maxValue: NUMERIC90,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
      param.conflict();
    });

    test('three ranges conflict on empty parameter fix conflict1', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC150,
              maxValue: NUMERIC200,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Third range',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC10,
              maxValue: NUMERIC90,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.maxFailedPriority(1);
      param.shouldUndefined();
    });

    test('three ranges conflict on empty parameter fix conflict2', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC150,
              maxValue: NUMERIC200,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Third range',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC10,
              maxValue: NUMERIC90,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.maxFailedPriority(1);
      param.shouldUndefined();
    });

    test('three ranges conflict on empty parameter fix conflict failed', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          priority: 2,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC150,
              maxValue: NUMERIC200,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Third range',
          priority: 1,
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC10,
              maxValue: NUMERIC90,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.maxFailedPriority(2);
      param.shouldUndefined();
    });

    test('two ranges and clean conflict on empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num');

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC150,
              maxValue: NUMERIC200,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Third range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
      param.conflict();
    });

    test('two ranges and clean conflict on not empty parameter', () => {
      tester.value('str', getStringValue('abc')).value('num', getNumValue(NUMERIC1000));

      tester
        .rule({
          group: 'Test',
          name: 'First range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: 100,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Second range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC150,
              maxValue: NUMERIC200,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Third range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.EMPTY,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.valid();
      param.shouldUndefined();
      param.conflict();
    });

    test.skip('testRangeAndMatchIntersectionOnInvalidParameter', () => {
      tester.value('str', getStringValue('abc')).value('num', getNumValue(NUMERIC10));

      tester
        .rule({
          group: 'Test',
          name: 'Range',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.INSIDE_RANGE,
              minValue: NUMERIC50,
              maxValue: NUMERIC150,
            },
          ],
        })
        .rule({
          group: 'Test',
          name: 'Match',
          ifs: [
            {
              xslName: 'str',
              operation: PredicateOperation.MATCHES,
              stringValue: 'abc',
            },
          ],
          thens: [
            {
              xslName: 'num',
              operation: PredicateOperation.MATCHES,
              minValue: 100,
            },
          ],
        });

      const results = tester.doInference();

      results.count(1);
      results.iterationCount(2);

      const param = results.itemAssertions('num');
      param.invalid();
      param.numerics().toEqual(['100']);
    });
  });
});
