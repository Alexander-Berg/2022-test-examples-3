import { Model as ProtoModel, ModelType, RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import {
  Category,
  ModificationSource,
  ParameterValue,
  ValueType,
} from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import {
  CategoryData,
  getNormalizedParameter,
  ImageType,
  ModelRelation,
  NormalizedImage,
  Parameter,
  Picture,
} from '@yandex-market/mbo-parameter-editor';
import { removeEmptyFields } from '@yandex-market/mbo-components';

import { IS_SKU_PARAM_ID, RU_ISO_CODE } from 'src/shared/constants';
import { DEFAULT_CATEGORY_ID } from 'src/shared/test-data/test-constants';

export interface TestModelSetup {
  id?: number;
  vendorId?: number;
  modelType?: ModelType;
  relations?: ModelRelation[];
  categoryId?: number;
  category?: Category;
  categoryData?: CategoryData;
  published?: boolean;
  isSku?: boolean;
  isDeleted?: boolean;
  params?: ParameterValue[];
}

let nextId = 1;

export function testModelProto(setup: TestModelSetup = {}): ProtoModel {
  const {
    id = nextId++,
    categoryId,
    relations,
    modelType = ModelType.GURU,
    category,
    categoryData,
    vendorId,
    published,
    isSku,
    isDeleted,
  } = setup;

  if (category && categoryData) {
    throw new Error(`category && categoryData shouldn't be used simultaneously`);
  }

  const params: Parameter[] = categoryData
    ? categoryData.category.parameterIds.map(paramId => categoryData.parameters[paramId])
    : category
    ? (category.parameter || []).map(p => getNormalizedParameter(p).parameter)
    : [];

  const additionalParams: ParameterValue[] = [];
  if (isSku) {
    additionalParams.push({
      value_type: ValueType.BOOLEAN,
      param_id: IS_SKU_PARAM_ID,
      bool_value: true,
      option_id: 123123,
    });
  }

  const paramValues = params.map(generateParamValue).concat(additionalParams);

  return removeEmptyFields({
    id,
    titles: [{ value: `test model #${id}`, isoCode: RU_ISO_CODE }],
    category_id: categoryId || category?.hid || categoryData?.id || DEFAULT_CATEGORY_ID,
    current_type: modelType as any,
    vendor_id: vendorId,
    published_on_market: published,
    relations: relations?.map(r => ({
      type: r.type as any,
      category_id: r.categoryId,
      id: r.id,
    })),
    deleted: Boolean(isDeleted),
    parameter_values: paramValues.length ? paramValues : undefined,
  });
}

export const testModelProtoFactory =
  (common: TestModelSetup) =>
  (setup: TestModelSetup = {}) =>
    testModelProto({ ...common, ...setup });

export function generateParamValue(param: Parameter): ParameterValue {
  return {
    param_id: param.id,
    xsl_name: param.xslName,
    value_type: param.valueType as any,
    numeric_value: param.valueType === ValueType.NUMERIC ? '101' : undefined,
    str_value:
      param.valueType === ValueType.STRING
        ? [{ value: `${param.xslName} #${param.id}`, isoCode: RU_ISO_CODE }]
        : undefined,
    bool_value: param.valueType === ValueType.BOOLEAN ? true : undefined,
    value_source: ModificationSource.OPERATOR_FILLED,
  };
}

export function relation(type: RelationType, id: number, categoryId: number = DEFAULT_CATEGORY_ID): ModelRelation {
  return { type, id, categoryId };
}

export function testPicture(pic: Partial<Picture>): NormalizedImage {
  const { url } = pic;

  return {
    url: url!,
    type: ImageType.REMOTE,
  };
}
