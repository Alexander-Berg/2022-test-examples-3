import { complexFiltersDataToPredicates } from './utils';
import { EConditionOperator, EConditionType, IFilterSettings } from 'src/components/ComplexFilter/types';
import { EqualsOperator } from './operators/EqualsOperator';
import { CommonParam, CommonParamValueType } from 'src/java/definitions';
import { NumberComplexFilter } from './NumberComplexFilter';
import { BaseComplexFilter } from './BaseComplexFilter';
import { ContainsOperator } from './operators/ContainsOperator';
import { EnumComplexFilter } from './EnumComplexFilter';
import { IncludesAnyOperator } from './operators/IncludesAnyOperator';
import { IncludesBooleanOperator } from './operators/IncludesBooleanOperator';
import { BooleanComplexFilter } from './BooleanComplexFilter';

describe('complex filters utils', () => {
  it('complexFiltersDataToPredicates', () => {
    const filterSettings: IFilterSettings[] = [
      {
        id: 'param1',
        data: {
          value: 123,
        },
        condition: EConditionType.EQUALS,
      },
      {
        id: 'param2',
        data: {
          value: 456,
        },
        condition: EConditionType.EQUALS,
      },
      {
        id: 'param3',
        data: {
          value: 789,
        },
        condition: EConditionType.EQUALS,
      },
      {
        id: 'param3',
        data: {},
        condition: EConditionType.EQUALS,
      },
      {
        id: 'param4',
        data: {},
        condition: EConditionType.EQUALS,
      },
    ];

    const filters: BaseComplexFilter[] = [1, 2, 3, 5].map(
      i => new NumberComplexFilter(`param${i}`, `param${i}`, [new EqualsOperator()])
    );

    const commonParams = [1, 2, 3].map(i => ({
      commonParamName: `param${i}`,
      commonParamValueType: CommonParamValueType.INT64,
      ruTitle: `param${i}`,
      options: [],
      required: false,
      multivalue: false,
    }));

    const predicates = complexFiltersDataToPredicates(
      filterSettings,
      [EConditionOperator.OR, EConditionOperator.AND, EConditionOperator.AND, EConditionOperator.OR],
      filters,
      commonParams as CommonParam[]
    );

    expect(predicates).toHaveLength(2);
    expect(predicates[0]).toHaveLength(2);

    expect(predicates[0][0].commonParamValue.int64s).toEqual([filterSettings[0].data!.value]);
    expect(predicates[0][1].commonParamValue.int64s).toEqual([filterSettings[1].data!.value]);
    expect(predicates[1][0].commonParamValue.int64s).toEqual([filterSettings[2].data!.value]);
  });

  it('complexFiltersDataToPredicates enum filter', () => {
    const filterSettings: IFilterSettings[] = [
      {
        id: 'param1',
        data: {
          value: [undefined, 123, 456],
        },
        condition: EConditionType.INCLUDES_ANY,
      },
    ];

    const filters: BaseComplexFilter[] = [
      new EnumComplexFilter(`param1`, `param1`, [
        new IncludesAnyOperator([
          {
            value: 123,
            label: 'Opt1',
          },
        ]),
      ]),
    ];

    const commonParams = [
      {
        commonParamName: 'param1',
        commonParamValueType: CommonParamValueType.ENUM,
        ruTitle: 'param1',
        options: [],
        required: false,
        multivalue: false,
      },
    ];

    const predicates = complexFiltersDataToPredicates(filterSettings, [], filters, commonParams as CommonParam[]);

    expect(predicates).toHaveLength(1);
    expect(predicates[0]).toHaveLength(1);

    expect(predicates[0][0].commonParamValue.options).toEqual([{ commonEnumId: 123, commonEnumValue: 'Opt1' }]);
  });

  it('complexFiltersDataToPredicates boolean filter', () => {
    const filterSettings: IFilterSettings[] = [
      {
        id: 'param1',
        data: {
          value: [true],
        },
        condition: EConditionType.INCLUDES_ANY,
      },
    ];

    const filters: BaseComplexFilter[] = [
      new BooleanComplexFilter(`param1`, `param1`, [new IncludesBooleanOperator()]),
    ];

    const commonParams = [
      {
        commonParamName: 'param1',
        commonParamValueType: CommonParamValueType.BOOLEAN,
        ruTitle: 'param1',
        options: [],
        required: false,
        multivalue: false,
      },
    ];

    const predicates = complexFiltersDataToPredicates(filterSettings, [], filters, commonParams as CommonParam[]);

    expect(predicates).toHaveLength(1);
    expect(predicates[0]).toHaveLength(1);

    expect(predicates[0][0].commonParamValue.booleans).toEqual([true]);
  });

  it('complexFiltersDataToPredicates invalid data', () => {
    const filterSettings: IFilterSettings[] = [
      {
        id: 'param0',
        data: {
          value: '', // проверяем, что не будет добавления пустой строки
        },
        condition: EConditionType.CONTAINS,
      },
      {
        id: 'param1',
        data: {
          value: 123,
        },
        condition: EConditionType.CONTAINS, // указываем contains, но в сам фильтр не добавим такой оператор
      },
      {
        id: 'param2',
        data: {
          value: '456,,789',
        },
        condition: EConditionType.EQUALS,
      },
    ];

    const filters: BaseComplexFilter[] = [1, 2, 3]
      .map(i => new NumberComplexFilter(`param${i}`, `param${i}`, [new EqualsOperator()]))
      .concat([new NumberComplexFilter(`param0`, `param0`, [new EqualsOperator(), new ContainsOperator()])]);

    const commonParams = [1, 2, 3].map(i => ({
      commonParamName: `param${i}`,
      commonParamValueType: CommonParamValueType.INT64,
      ruTitle: `param${i}`,
      options: [],
      required: false,
    }));

    const predicates = complexFiltersDataToPredicates(
      filterSettings,
      [EConditionOperator.OR, EConditionOperator.OR],
      filters,
      commonParams as CommonParam[]
    );

    expect(predicates).toHaveLength(1);
    expect(predicates[0]).toHaveLength(2);
    expect(predicates[0][0].commonParamValue.int64s).toStrictEqual([456]);
    expect(predicates[0][1].commonParamValue.int64s).toStrictEqual([789]);
  });
});
