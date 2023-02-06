import { applyComplexFilters } from './utils';
import { CommonEntity } from '../../java/definitions';
import { EConditionOperator, EConditionType, IFilterSettings } from '../../components/ComplexFilter/types';
import { BaseComplexFilter } from '../../utils/filters/complexFilters/BaseComplexFilter';
import { NumberComplexFilter } from '../../utils/filters/complexFilters/NumberComplexFilter';
import { ContainsOperator } from '../../utils/filters/complexFilters/operators/ContainsOperator';
import { StringComplexFilter } from '../../utils/filters/complexFilters/StringComplexFilter';
import { EqualsOperator } from '../../utils/filters/complexFilters/operators/EqualsOperator';
import { DateComplexFilter } from '../../utils/filters/complexFilters/DateComplexFilter';
import { BetweenOperator } from '../../utils/filters/complexFilters/operators/BetweenOperator';

describe('DataGridWidget utils', () => {
  it('applyComplexFilters', () => {
    const entities: CommonEntity[] = [
      {
        entityId: 0,
        commonParamValues: [
          {
            commonParamName: 'param1',
            numerics: [123],
          },
          {
            commonParamName: 'param2',
          },
        ],
      } as CommonEntity,
      {
        entityId: 1,
        commonParamValues: [
          {
            commonParamName: 'param1',
          },
          {
            commonParamName: 'param2',
            strings: ['testik'],
          },
          {
            commonParamName: 'param3',
            timestamps: [new Date(1917, 9, 25, 21, 40).getTime()],
          },
        ],
      } as CommonEntity,
      {
        entityId: 2,
        commonParamValues: [
          {
            commonParamName: 'param1',
          },
          {
            commonParamName: 'param2',
          },
          {
            commonParamName: 'param3',
            timestamps: [new Date(1917, 9, 25, 21, 40).getTime()],
          },
        ],
      } as CommonEntity,
    ];

    const filters: BaseComplexFilter[] = [
      new NumberComplexFilter('param1', 'param1', [new ContainsOperator()]),
      new StringComplexFilter('param2', 'param2', [new EqualsOperator()]),
      new DateComplexFilter('param3', 'param3', [new BetweenOperator()]),
    ];

    const settings: IFilterSettings[] = [
      {
        id: 'param1',
        condition: EConditionType.CONTAINS,
        data: { value: 123 },
      },
      {
        id: 'param2',
        condition: EConditionType.EQUALS,
        data: { value: 'test' },
      },
      {
        id: 'param3',
        condition: EConditionType.BETWEEN,
        data: { from: new Date(1900, 0, 1) },
      },
      {
        id: 'param2',
        condition: EConditionType.EQUALS,
        data: { value: 'testik' },
      },
    ];

    let filteredEntities = applyComplexFilters(entities, filters, settings, [
      EConditionOperator.AND,
      EConditionOperator.OR,
      EConditionOperator.AND,
    ]);
    expect(filteredEntities).toHaveLength(1);

    filteredEntities = applyComplexFilters(entities, filters, settings, [
      EConditionOperator.AND,
      EConditionOperator.AND,
      EConditionOperator.AND,
    ]);
    expect(filteredEntities).toHaveLength(0);
  });
});
