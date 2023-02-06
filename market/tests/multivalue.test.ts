import { ModelType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { SKUParameterMode, ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { Model, Option, Parameter, ParameterValue } from '../entities';
import { createTitleGenerator } from '..';
import {
  getCategoryDataMock,
  getModelMock,
  getOptionMock,
  getParameterMock,
  getParameterValueMock,
} from '../__mocks__';

const commonOptions: Option[] = [
  getOptionMock({ id: 11, name: 'AAA' }),
  getOptionMock({ id: 12, name: 'BBB' }),
  getOptionMock({ id: 13, name: '220' }),
  getOptionMock({ id: 14, name: '380' }),
];

const commonParameters: Parameter[] = [
  getParameterMock({
    id: 1,
    xslName: 'age',
    name: 'Age',
    valueType: ValueType.NUMERIC,
    unit: {
      id: 0,
      name: 'лет',
    },
    skuMode: SKUParameterMode.SKU_DEFINING,
  }),
  getParameterMock({
    id: 2,
    xslName: 'name',
    name: 'Name',
    valueType: ValueType.STRING,
  }),
  getParameterMock({
    id: 3,
    xslName: 'vendor',
    name: 'Vendor',
    valueType: ValueType.ENUM,
    optionIds: [11, 12],
  }),
  getParameterMock({
    id: 4,
    xslName: 'is_universal',
    name: 'Universal',
    valueType: ValueType.BOOLEAN,
    skuMode: SKUParameterMode.SKU_DEFINING,
  }),
  getParameterMock({
    id: 5,
    xslName: 'voltage',
    name: 'Voltage',
    valueType: ValueType.NUMERIC_ENUM,
    unit: {
      id: 2,
      name: 'В',
    },
    optionIds: [13, 14],
    skuMode: SKUParameterMode.SKU_DEFINING,
  }),
  getParameterMock({
    id: 6,
    xslName: 'color',
    name: 'Цвет',
    valueType: ValueType.STRING,
  }),
];

const generateTestModel: typeof getModelMock = data => {
  return getModelMock({
    id: 1,
    sourceType: ModelType.GURU,
    currentType: ModelType.GURU,
    ...data,
  });
};

const modelWithAges = (...ages: number[]) => {
  const parameterValues: ParameterValue[] = [];

  for (const numericValue of ages) {
    parameterValues.push(
      getParameterValueMock({
        parameterId: 1,
        type: ValueType.NUMERIC,
        numericValue: String(numericValue),
      })
    );
  }

  parameterValues.push(
    getParameterValueMock({
      parameterId: 2,
      type: ValueType.STRING,
      stringValue: [{ value: 'Model Name', isoCode: 'ru' }],
    })
  );

  return generateTestModel({ parameterValues });
};

const modelWithColors = (...colors: string[]) => {
  const parameterValues: ParameterValue[] = [];

  for (const value of colors) {
    parameterValues.push(
      getParameterValueMock({
        parameterId: 6,
        type: ValueType.STRING,
        stringValue: [{ value, isoCode: 'ru' }],
      })
    );
  }

  parameterValues.push({
    parameterId: 2,
    type: ValueType.STRING,
    stringValue: [{ value: 'Model Name', isoCode: 'ru' }],
  });

  return generateTestModel({ parameterValues });
};

const createTitleForPattern = (titleTemplate: string, model: Model) => {
  const titleGenerator = createTitleGenerator(
    getCategoryDataMock({ parameters: commonParameters, options: commonOptions })
  );

  return titleGenerator.createModelTitle({ titleTemplate, model });
};

describe('model title generator multivalue', () => {
  test('join default', () => {
    const { title } = createTitleForPattern('[true, join(s1)]', modelWithAges(49, 14, 17));

    expect(title).toEqual('49, 14, 17');
  });

  test('join strings', () => {
    const { title } = createTitleForPattern('[true, join(s6)]', modelWithColors('red', 'green'));

    expect(title).toEqual('Red, green');
  });

  test('join empty', () => {
    expect(createTitleForPattern('[true, join(s1)]', generateTestModel()).title).toEqual('');
    expect(createTitleForPattern('[true, join(s5)]', generateTestModel()).title).toEqual('');
  });

  test('empty mandatory', () => {
    expect(createTitleForPattern('[true, join(s1), "false-result", true]', generateTestModel()).error!.message).toEqual(
      'Mandatory field <Age> is undefined'
    );
    expect(createTitleForPattern('[true, join(s6), "false-result", true]', generateTestModel()).error!.message).toEqual(
      'Mandatory field <Цвет> is undefined'
    );
  });

  test('join with delimiter', () => {
    expect(createTitleForPattern("[true, join(s1, '==')]", modelWithAges(49, 14, 17)).title).toEqual('49==14==17');
  });

  test('join with suffix', () => {
    expect(createTitleForPattern("[true, join(s1, null, ' лет')]", modelWithAges(49, 14, 17)).title).toEqual(
      '49 лет, 14 лет, 17 лет'
    );
  });

  test('join with delimiter suffix', () => {
    expect(createTitleForPattern("[true, join(s1, ' и ', ' лет')]", modelWithAges(49, 14, 17)).title).toEqual(
      '49 лет и 14 лет и 17 лет'
    );
  });

  test('join first', () => {
    expect(createTitleForPattern('[true, joinFirst(s1, 2)]', modelWithAges(49, 14, 17)).title).toEqual('49, 14');
    expect(createTitleForPattern('[true, joinFirst(s1, 99)]', modelWithAges(49, 14, 17)).title).toEqual('49, 14, 17');
  });

  test('join first with delimiter', () => {
    expect(createTitleForPattern("[true, joinFirst(s1, 2, '==')]", modelWithAges(49, 14, 17)).title).toEqual('49==14');
    expect(createTitleForPattern("[true, joinFirst(s1, 99, '==')]", modelWithAges(49, 14, 17)).title).toEqual(
      '49==14==17'
    );
  });

  test('join first with suffix', () => {
    expect(createTitleForPattern("[true, joinFirst(s1, 2, '_', '#')]", modelWithAges(49, 14, 17)).title).toEqual(
      '49#_14#'
    );
  });

  test('join first wrong count', () => {
    expect(createTitleForPattern('[true, joinFirst(s1, 0)]', modelWithAges(49, 14, 17)).error!.message).toEqual(
      'joinFirst got count <= 0'
    );
  });

  test('contains', () => {
    expect(createTitleForPattern("[true, contains(s1, '14')]", modelWithAges(49, 14, 17)).title).toEqual('True');
    expect(createTitleForPattern("[true, contains(s1, '47')]", modelWithAges(49, 14, 17)).title).toEqual('False');
  });

  test('contains string', () => {
    expect(createTitleForPattern("[true, contains(s6, 'blue')]", modelWithColors('red', 'blue')).title).toEqual('True');
    expect(createTitleForPattern("[true, contains(s6, 'green')]", modelWithColors('red', 'blue')).title).toEqual(
      'False'
    );
  });

  test('contains empty', () => {
    expect(createTitleForPattern("[true, contains(s1, '15')]", generateTestModel()).title).toEqual('False');
    expect(createTitleForPattern("[true, contains(s6, 'blue')]", generateTestModel()).title).toEqual('False');
  });

  test('contains null', () => {
    expect(createTitleForPattern('[true, contains(s1, null)]', modelWithAges(49, 14, 17)).title).toEqual('False');
    expect(createTitleForPattern('[true, contains(s6, null)]', modelWithColors('red', 'blue')).title).toEqual('False');
  });

  test('contains converts to string', () => {
    expect(createTitleForPattern('[true, contains(s1, 17)]', modelWithAges(49, 14, 17)).title).toEqual('True');
    expect(createTitleForPattern('[true, contains(s1, 19)]', modelWithAges(49, 14, 17)).title).toEqual('False');
  });

  test('contains multiple options', () => {
    expect(createTitleForPattern('[true, contains(s1, 17, 14)]', modelWithAges(49, 14, 17)).title).toEqual('True');
    expect(createTitleForPattern('[true, contains(s1, 17, 1)]', modelWithAges(49, 14, 17)).title).toEqual('False');
  });

  test('default representation', () => {
    expect(createTitleForPattern('[true, s1]', modelWithAges(49, 14, 17)).title).toEqual('49, 14, 17');
    expect(createTitleForPattern('[true, s1]', generateTestModel()).title).toEqual('');
  });

  test('count', () => {
    expect(createTitleForPattern('[true, count(s1)]', modelWithAges(1)).title).toEqual('1');
    expect(createTitleForPattern('[true, count(s1)]', modelWithAges(1, 4, 5)).title).toEqual('3');
  });

  test('count strings', () => {
    expect(createTitleForPattern('[true, count(s6)]', modelWithColors('red')).title).toEqual('1');
    expect(createTitleForPattern('[true, count(s6)]', modelWithColors('green', 'beige')).title).toEqual('2');
  });

  test('count empty', () => {
    expect(createTitleForPattern('[true, count(s1)]', generateTestModel()).title).toEqual('0');
    expect(createTitleForPattern('[true, count(s6)]', generateTestModel()).title).toEqual('0');
  });

  test('wrong argument error', () => {
    expect(createTitleForPattern('[true, count(125)]', generateTestModel()).error!.message).toEqual(
      'count expects array as first argument, got number'
    );

    expect(createTitleForPattern("[true, join('str')]", generateTestModel()).error!.message).toEqual(
      'join expects array as first argument, got string'
    );

    expect(createTitleForPattern('[true, contains(true)]', generateTestModel()).error!.message).toEqual(
      'contains expects array as first argument, got boolean'
    );

    expect(createTitleForPattern('[true, contains()]', generateTestModel()).error!.message).toEqual(
      'contains expects array as first argument, got undefined'
    );
  });
});
