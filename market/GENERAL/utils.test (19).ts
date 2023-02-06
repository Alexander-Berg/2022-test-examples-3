import { EnumSortType, ParamOptionDto, ParamOptionName, VendorInfo } from 'src/java/definitions';
import { ParameterValuesOrder } from 'src/pages/ParametersPage/widgets/ValuesOrder/types';
import { checkParameterValues, checkParameterValuesOrder } from './utils';

describe('GlobalParameterEdit utils', () => {
  const defaultValue: ParamOptionDto = {
    active: true,
    paramOptionName: {} as ParamOptionName,
    tagCode: {
      code: 'test',
      tag: 'test',
    },
    vendorInfo: {} as VendorInfo,
    valueId: 0,
    displayName: 'name',
    aliases: {
      paramAliases: [],
      parentAliases: [],
    },
    published: false,
    comment: '',
    manufacturerVendors: [],
  };

  it('check empty values', () => {
    expect(checkParameterValues(undefined).length).toBe(0);
  });

  it('check empty name', () => {
    expect(checkParameterValues([{ ...defaultValue, displayName: '' }]).length).toBeGreaterThan(0);
  });

  it('check non-uniq values', () => {
    const values = [
      {
        ...defaultValue,
        displayName: 'testik',
      },
      {
        ...defaultValue,
        displayName: 'testik',
      },
      {
        ...defaultValue,
        displayName: 'testikovich',
      },
    ];
    expect(checkParameterValues(values).length).toBeGreaterThan(0);
  });

  it('check non-uniq alias names', () => {
    const values = [
      {
        ...defaultValue,
        aliases: [
          { displayName: 'alias1', morph: true },
          { displayName: 'alias1', morph: false },
        ],
      },
    ];
    expect(checkParameterValues(values).length).toBeGreaterThan(0);
  });

  it('check empty alias names', () => {
    const values = [
      {
        ...defaultValue,
        aliases: [{ name: '', morph: false }],
      },
    ];
    expect(checkParameterValues(values).length).toBeGreaterThan(0);
  });

  it('check different values count and selected values', () => {
    const values = [
      { ...defaultValue, id: 0 },
      { ...defaultValue, id: 1 },
      { ...defaultValue, id: 2 },
      { ...defaultValue, id: 3 },
    ];

    const selectedValues = [0, 1, 2, 3];

    const valuesOrder: ParameterValuesOrder = {
      values: selectedValues,
      valuesCount: 5,
      sortType: EnumSortType.MANUAL,
    };

    expect(checkParameterValuesOrder(valuesOrder, values).length).toBeGreaterThan(0);
  });

  it('check different values count and selected existing values', () => {
    const values = [
      { ...defaultValue, id: 0 },
      { ...defaultValue, id: 1 },
      { ...defaultValue, id: 2 },
      { ...defaultValue, id: 3 },
    ];

    const selectedValues = [0, 2, 3, 4];

    const valuesOrder: ParameterValuesOrder = {
      values: selectedValues,
      valuesCount: 4,
      sortType: EnumSortType.MANUAL,
    };

    expect(checkParameterValuesOrder(valuesOrder, values).length).toBeGreaterThan(0);
  });
});
