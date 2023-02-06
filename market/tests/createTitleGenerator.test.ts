import { ModelType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ModificationSource, SKUParameterMode, ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { Model, Option, Parameter, ParameterValue, SizeMeasureInfo } from '../entities';
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
];

const generateTestModel = (parameterValues: ParameterValue[] = []): Model =>
  getModelMock({
    id: 1,
    sourceType: ModelType.GURU,
    currentType: ModelType.GURU,
    parameterValues,
  });

const generateTestParamValues = (): ParameterValue[] => {
  const parameterValue1: ParameterValue = getParameterValueMock({
    parameterId: 1,
    type: ValueType.NUMERIC,
    numericValue: '10',
  });

  const parameterValue2: ParameterValue = getParameterValueMock({
    parameterId: 2,
    type: ValueType.STRING,
    stringValue: [{ value: 'Model name', isoCode: 'ru' }],
  });

  const parameterValue3: ParameterValue = getParameterValueMock({
    parameterId: 3,
    type: ValueType.ENUM,
    optionId: 11,
  });

  const parameterValue4: ParameterValue = getParameterValueMock({
    parameterId: 4,
    type: ValueType.BOOLEAN,
    booleanValue: true,
  });

  const parameterValue5: ParameterValue = getParameterValueMock({
    parameterId: 5,
    type: ValueType.NUMERIC_ENUM,
    optionId: 14,
  });

  const result = [];
  result.push(parameterValue1);
  result.push(parameterValue2);
  result.push(parameterValue3);
  result.push(parameterValue4);
  result.push(parameterValue5);

  return result;
};

const generateTestSizeData = () => {
  const sizeMeasureInfo: SizeMeasureInfo = {
    scales: [],
    sizeMeasure: {
      id: 650,
      name: 'Размер одежды',
      numericParamId: 0,
      valueParamId: 240,
      unitParamId: 250,
    },
  };

  return {
    sizeMeasureInfos: [sizeMeasureInfo],
    parameters: [
      getParameterMock({
        id: 240,
        xslName: 'size',
        name: 'Size',
        valueType: ValueType.ENUM,
        skuMode: SKUParameterMode.SKU_DEFINING,
        optionIds: [241, 242, 243, 244, 245],
      }),
      getParameterMock({
        id: 250,
        xslName: 'size_UNITS',
        name: 'Size (размерная сетка)',
        valueType: ValueType.ENUM,
        optionIds: [251, 252],
      }),
    ],
    options: [
      getOptionMock({ id: 241, name: 'M' }),
      getOptionMock({ id: 242, name: 'L' }),
      getOptionMock({ id: 243, name: 'XXL' }),
      getOptionMock({ id: 244, name: '46' }),
      getOptionMock({ id: 245, name: '48' }),
      getOptionMock({ id: 251, name: 'INT' }),
      getOptionMock({ id: 252, name: 'RU' }),
    ],
  };
};

const getModelWithVendor = (paramValues: ParameterValue[], vendorValue: ParameterValue, value: string) => {
  return generateTestModel([
    ...paramValues,
    {
      ...vendorValue,
      stringValue: [{ value, isoCode: 'ru' }],
    },
  ]);
};

const generateCategoryData = ({
  options = [],
  parameters = [],
  sizeMeasureInfos,
}: { options?: Option[]; parameters?: Parameter[]; sizeMeasureInfos?: SizeMeasureInfo[] } = {}) => {
  return getCategoryDataMock({
    options: [...commonOptions, ...options],
    parameters: [...commonParameters, ...parameters],
    sizeMeasureInfos,
  });
};

const generateTitle = (guruTitleTemplate: string, parameters: Parameter[], model: Model) => {
  const categoryData = generateCategoryData({ parameters });
  const titleGenerator = createTitleGenerator(categoryData);

  return titleGenerator.createSkuTitle({ guruTitleTemplate, model }).title;
};

describe('create title generator', () => {
  test('create simple title', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(1 ),(v1 )],[(1 ),(v3 )],[(t0 ),(t0 )]]}';

    const model = generateTestModel(generateTestParamValues());
    const titleGenerator = createTitleGenerator(generateCategoryData());
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toEqual('10 AAA Model name');
  });

  test('title generator clear scope', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(1 ),(v1 )],[(1 ),(v3 )],[(t0 ),(t0 )]]}';

    const paramValues = generateTestParamValues();
    const exceptAge = paramValues.filter(pv => pv.parameterId !== 1);

    const model1 = generateTestModel(exceptAge);
    const model2 = generateTestModel(paramValues);
    const model3 = generateTestModel(exceptAge);

    const titleGenerator = createTitleGenerator(generateCategoryData());

    const title1 = titleGenerator.createModelTitle({ titleTemplate, model: model1 }).title;
    const title2 = titleGenerator.createModelTitle({ titleTemplate, model: model2 }).title;
    const title3 = titleGenerator.createModelTitle({ titleTemplate, model: model3 }).title;

    expect(title1).toBe('AAA Model name');
    expect(title2).toBe('10 AAA Model name');
    expect(title3).toBe('AAA Model name');
  });

  test('title generator clear scope multivalues', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(t0 ),(t0 )],[(count(s1) > 0),(join(s1) )],[(v3 ),(v3)]]}';

    const paramValues = generateTestParamValues();
    paramValues.push(
      getParameterValueMock({
        parameterId: 1,
        type: ValueType.NUMERIC,
        numericValue: '14',
      })
    );

    const exceptAge = paramValues.filter(pv => pv.parameterId !== 1);

    const model1 = generateTestModel(exceptAge);
    const model2 = generateTestModel(paramValues);
    const model3 = generateTestModel(exceptAge);

    const titleGenerator = createTitleGenerator(generateCategoryData());

    const title1 = titleGenerator.createModelTitle({ titleTemplate, model: model1 }).title;
    const title2 = titleGenerator.createModelTitle({ titleTemplate, model: model2 }).title;
    const title3 = titleGenerator.createModelTitle({ titleTemplate, model: model3 }).title;

    expect(title1).toBe('Model name AAA');
    expect(title2).toBe('Model name 10, 14 AAA');
    expect(title3).toBe('Model name AAA');
  });

  test('show only last value from multivalues', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(t0 ),(t0 )], [(1 ),(v1 )]]}';

    const paramValues = generateTestParamValues();
    paramValues.push(
      getParameterValueMock({
        parameterId: 1,
        type: ValueType.NUMERIC,
        numericValue: '14',
      })
    );

    const model = generateTestModel(paramValues);

    const titleGenerator = createTitleGenerator(generateCategoryData());
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toEqual('Model name 14');
  });

  test('create simple title with condition', () => {
    const titleTemplate =
      '{"delimiter":" ","values":[[(v1 ),(v1 ),null,(true)], [(v3 ),(v3 ),null,(true)],[(t0 ),(t0 )]]}';

    const paramValues = generateTestParamValues();
    const model = generateTestModel(paramValues);

    const titleGenerator = createTitleGenerator(generateCategoryData());
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toEqual('10 AAA Model name');
  });

  test('create title with enum param', () => {
    const titleTemplate =
      '{"delimiter":" ","values":[[(v130 ),(v130 ),null,(true)],[(v3 ),(v3 ),null,(true)],[(t0 ),(t0 )]]}';

    const parameters: Parameter[] = [
      getParameterMock({
        id: 130,
        xslName: 'type',
        name: 'Type',
        valueType: ValueType.ENUM,
        optionIds: [14, 15],
      }),
    ];
    const options: Option[] = [getOptionMock({ id: 14, name: 'First' }), getOptionMock({ id: 15, name: 'Second' })];

    const paramValues = generateTestParamValues();
    paramValues.push({
      parameterId: 130,
      type: ValueType.ENUM,
      optionId: 15,
    });

    const model = generateTestModel(paramValues);
    const titleGenerator = createTitleGenerator(generateCategoryData({ parameters, options }));

    const { title } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toEqual('Second AAA Model name');
  });

  test('create simple title with boolean param', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(1 ),(v4 )],[(1 ),(v3 )],[(t0 ),(t0 )]]}';

    const paramValues = generateTestParamValues();
    const model = generateTestModel(paramValues);

    const titleGenerator = createTitleGenerator(generateCategoryData());
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toEqual('True AAA Model name');
  });

  test('create title without manadatory param', () => {
    const titleTemplate =
      '{"delimiter":" ","values":[[(v1 ),(v1 ),null,(true)],[(v28 ),(v28 ),null,(true)],[(t0 ),(t0 )]]}';

    const parameters: Parameter[] = [
      getParameterMock({
        id: 28,
        xslName: 'type',
        name: 'Type',
        valueType: ValueType.STRING,
      }),
    ];
    const model = generateTestModel(generateTestParamValues());

    const titleGenerator = createTitleGenerator(generateCategoryData({ parameters }));
    const { title, error } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toBeUndefined();
    expect(error).toBeDefined();
  });

  test('create title without not manadatory param with default value', () => {
    const titleTemplate =
      '{"delimiter":" ","values":[[(v1 ),(v1 ),null,(true)],[(v28 ),(v28 ),("[No value]"),(false)],[(t0 ),(t0 )]]}';

    const parameters: Parameter[] = [
      getParameterMock({
        id: 28,
        xslName: 'type',
        name: 'Type',
        valueType: ValueType.STRING,
      }),
    ];
    const model = generateTestModel(generateTestParamValues());

    const titleGenerator = createTitleGenerator(generateCategoryData({ parameters }));
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toEqual('10 [No value] Model name');
  });

  test('create title without not manadatory param without default value', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(v1 ),(v1 ),null,(true)],[(1 ),(v28 )],[(t0 ),(t0 )]]}';

    const parameters: Parameter[] = [
      getParameterMock({
        id: 28,
        xslName: 'type',
        name: 'Type',
        valueType: ValueType.STRING,
      }),
    ];
    const model = generateTestModel(generateTestParamValues());

    const titleGenerator = createTitleGenerator(generateCategoryData({ parameters }));
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toEqual('10 Model name');
  });

  test('create sku default title', () => {
    const guruTitleTemplate = '{"delimiter":" ","values":[[(1 ),(v3 ),null,(true)],[(1 ),(t0 ),null,(true)]]}';

    const parameters: Parameter[] = [
      getParameterMock({
        id: 130,
        xslName: 'type',
        name: 'Type',
        valueType: ValueType.ENUM,
        skuMode: SKUParameterMode.SKU_DEFINING,
        optionIds: [16, 17],
      }),
      getParameterMock({
        id: 330,
        xslName: 'anotherBooleanParam',
        name: 'BooleanParam',
        valueType: ValueType.BOOLEAN,
        skuMode: SKUParameterMode.SKU_DEFINING,
      }),
      getParameterMock({
        id: 370,
        xslName: 'oneMoreBooleanParam',
        name: 'OneMoreBooleanParam',
        valueType: ValueType.BOOLEAN,
        skuMode: SKUParameterMode.SKU_DEFINING,
      }),
    ];
    const options: Option[] = [getOptionMock({ id: 16, name: 'First' }), getOptionMock({ id: 17, name: 'Second' })];

    const paramValues = [
      ...generateTestParamValues(),
      getParameterValueMock({
        parameterId: 130,
        type: ValueType.ENUM,
        optionId: 17,
      }),
      getParameterValueMock({
        parameterId: 330,
        type: ValueType.BOOLEAN,
        booleanValue: true,
      }),
      getParameterValueMock({
        parameterId: 370,
        type: ValueType.BOOLEAN,
        booleanValue: false,
      }),
    ];

    const model = {
      ...generateTestModel(paramValues),
      currentType: ModelType.SKU,
    };

    const titleGenerator = createTitleGenerator(generateCategoryData({ parameters, options }));
    const { title } = titleGenerator.createSkuTitle({ guruTitleTemplate, model });

    expect(title).toEqual('AAA Model name 10 лет booleanparam universal Second 380 В');
  });

  test('create sku default title with size', () => {
    const guruTitleTemplate = '{"delimiter":" ","values":[[(1 ),(v3 ),null,(true)],[(1 ),(t0 ),null,(true)]]}';

    const { options, parameters, sizeMeasureInfos } = generateTestSizeData();

    const model = {
      ...generateTestModel([
        ...generateTestParamValues(),
        getParameterValueMock({
          parameterId: 240,
          type: ValueType.ENUM,
          optionId: 243,
        }),
        getParameterValueMock({
          parameterId: 250,
          type: ValueType.ENUM,
          optionId: 251,
        }),
      ]),
      currentType: ModelType.SKU,
    };

    const titleGenerator = createTitleGenerator(generateCategoryData({ parameters, options, sizeMeasureInfos }));
    const { title } = titleGenerator.createSkuTitle({ guruTitleTemplate, model });

    expect(title).toEqual('AAA Model name 10 лет universal XXL (INT) 380 В');
  });

  test('create sku default title with missing sku param values', () => {
    const guruTitleTemplate = '{"delimiter":" ","values":[[(1 ),(v3 ),null,(true)],[(1 ),(t0 ),null,(true)]]}';

    const { options, parameters, sizeMeasureInfos } = generateTestSizeData();

    const paramValues = generateTestParamValues().filter(({ parameterId }) => parameterId !== 5 && parameterId !== 1);

    const model = {
      ...generateTestModel(paramValues),
      currentType: ModelType.SKU,
    };

    const titleGenerator = createTitleGenerator(generateCategoryData({ parameters, options, sizeMeasureInfos }));
    const { title } = titleGenerator.createSkuTitle({ guruTitleTemplate, model });

    expect(title).toEqual('AAA Model name universal');
  });

  test('create sku title with custom title', () => {
    const titleTemplate =
      '{"delimiter":" ","values":[[(1 ),(v3 ),null,(true)],[(1 ),(t0 ),null,(true)], [(1), (v1)], [(1), (u0)]]}';

    const model = {
      ...generateTestModel(generateTestParamValues()),
      currentType: ModelType.SKU,
    };

    const titleGenerator = createTitleGenerator(generateCategoryData());
    const { title } = titleGenerator.createSkuTitle({ titleTemplate, model });

    expect(title).toEqual('AAA Model name 10 лет');
  });

  test('create sku title with quoted vendor', () => {
    const guruTemplate = '{"delimiter":" ","values":[[(1 ),(v3 ),null,(true)],[(1 ),(t0 ),null,(true)]]}';

    const params: Parameter[] = [
      getParameterMock({
        id: 130,
        xslName: 'vendor',
        name: 'Производитель',
        valueType: ValueType.STRING,
        skuMode: SKUParameterMode.SKU_DEFINING,
        quotedInTitle: true,
      }),
    ];
    const paramValues = generateTestParamValues();

    const vendorValue: ParameterValue = {
      parameterId: 130,
      type: ValueType.STRING,
    };

    let model: Model;
    let title: string | undefined;

    // Английский вендор
    model = getModelWithVendor(paramValues, vendorValue, 'Samsung');
    title = generateTitle(guruTemplate, params, model);
    expect(title).toEqual('AAA Model name 10 лет universal Samsung 380 В');

    // Русский вендор
    model = getModelWithVendor(paramValues, vendorValue, 'Самсунгъ');
    title = generateTitle(guruTemplate, params, model);
    expect(title).toEqual('AAA Model name 10 лет universal «Самсунгъ» 380 В');

    // Русский уже с кавычками
    model = getModelWithVendor(paramValues, vendorValue, '"Самсунгъ"');
    title = generateTitle(guruTemplate, params, model);
    expect(title).toEqual('AAA Model name 10 лет universal «Самсунгъ» 380 В');

    // Русский с английским вперемешку
    model = getModelWithVendor(paramValues, vendorValue, 'Корпорация Microsoft');
    title = generateTitle(guruTemplate, params, model);
    expect(title).toEqual('AAA Model name 10 лет universal «Корпорация Microsoft» 380 В');

    // Кавычки в начале слова
    model = getModelWithVendor(paramValues, vendorValue, '"Атлант" - студия');
    title = generateTitle(guruTemplate, params, model);
    expect(title).toEqual('AAA Model name 10 лет universal «Атлант» - студия 380 В');

    // Кавычки в конце слова
    model = getModelWithVendor(paramValues, vendorValue, 'Корпорация "Microsoft"');
    title = generateTitle(guruTemplate, params, model);
    expect(title).toEqual('AAA Model name 10 лет universal Корпорация «Microsoft» 380 В');

    // Кавычки посередине
    model = getModelWithVendor(paramValues, vendorValue, 'Игры "АРТ" настольные');
    title = generateTitle(guruTemplate, params, model);
    expect(title).toEqual('AAA Model name 10 лет universal Игры «АРТ» настольные 380 В');
  });

  test('empty mandatory', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(1 ),(v3 ), "false-result", true],[(t0 ),(t0 )]]}';

    const titleGenerator = createTitleGenerator(generateCategoryData());
    const { error } = titleGenerator.createModelTitle({ titleTemplate, model: generateTestModel() });

    expect(error!.message).toEqual('Mandatory field <Vendor> is undefined');
  });

  test('model name override', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(1 ),(v1 )],[(1 ),(v3 )],[(t0 ),(t0 )]]}';

    const model = generateTestModel(generateTestParamValues());
    const titleGenerator = createTitleGenerator(generateCategoryData());
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model, modelNameOverride: 'Custom Model Name' });

    expect(title).toEqual('10 AAA Custom Model Name');
  });

  test('without title template', () => {
    const model = generateTestModel(generateTestParamValues());
    const titleGenerator = createTitleGenerator(generateCategoryData());
    const { title } = titleGenerator.createModelTitle({ model });

    expect(title).toEqual('');
  });

  test('skip non exist parameter', () => {
    const titleTemplate = '[(1 ),(v123 )]';

    const model = generateTestModel([
      ...generateTestParamValues(),
      getParameterValueMock({
        parameterId: 123,
        type: ValueType.BOOLEAN,
        booleanValue: true,
      }),
    ]);

    const titleGenerator = createTitleGenerator(generateCategoryData());
    const { error } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(error!.message).toEqual('v123 is not defined');
  });

  test('skip forbidden sku param names', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(t0 ),(t0 )],[(1 ),(v123 )]]}';

    const parameters: Parameter[] = [
      getParameterMock({
        id: 123,
        xslName: 'skipped',
        valueType: ValueType.BOOLEAN,
      }),
    ];

    const model = generateTestModel([
      ...generateTestParamValues(),
      getParameterValueMock({
        parameterId: 123,
        type: ValueType.BOOLEAN,
        booleanValue: true,
      }),
    ]);

    const titleGenerator = createTitleGenerator(generateCategoryData({ parameters }));
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model, forbiddenSkuParamNames: ['skipped'] });

    expect(title).toEqual('Model name');
  });

  test('skip values injected by SizeMeasureConversionPipePart', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(t0 ),(t0 )],[(1 ),(v123 )]]}';

    const parameters: Parameter[] = [
      getParameterMock({
        id: 123,
        xslName: 'skiped',
        valueType: ValueType.BOOLEAN,
      }),
    ];

    const model = generateTestModel([
      ...generateTestParamValues(),
      getParameterValueMock({
        parameterId: 123,
        type: ValueType.BOOLEAN,
        booleanValue: true,
        modificationSource: ModificationSource.DEPENDENCY_RULE,
      }),
    ]);

    const titleGenerator = createTitleGenerator(generateCategoryData({ parameters }));
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toEqual('Model name');
  });

  /**
   * @issue CONTENTLAB-307
   */
  test('skip invalid option in model', () => {
    const titleTemplate = '{"delimiter":" ","values":[[(t0 ),(t0 )],[(1 ),(v123 )]]}';

    const options = [getOptionMock({ id: 300 })];
    const parameters: Parameter[] = [
      getParameterMock({
        id: 123,
        xslName: 'param123',
        valueType: ValueType.ENUM,
        optionIds: [300],
      }),
    ];

    const model = generateTestModel([
      ...generateTestParamValues(),
      getParameterValueMock({
        parameterId: 123,
        type: ValueType.ENUM,
        optionId: 301,
      }),
    ]);

    const titleGenerator = createTitleGenerator(generateCategoryData({ options, parameters }));
    const { title } = titleGenerator.createModelTitle({ titleTemplate, model });

    expect(title).toEqual('Model name');
  });
});
