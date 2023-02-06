import { ParamMappingRule, ParamMappingType, ParamMapping } from 'src/java/definitions';
import { UiParamMapping } from 'src/utils/types';
import { parameter, stringParameter } from './categoryData';

export const simpleMapping = {
  editable: true,
  hypothesis: false,
  deleted: false,
  id: 123,
  marketParams: [{ parameterId: parameter.id }],
  shopParams: [{ name: 'vendor' }],
  rules: {},
  shopId: 1,
  mappingType: ParamMappingType.MAPPING,
} as ParamMapping;

export const mappingWithStringParam = {
  ...simpleMapping,
  marketParams: [{ parameterId: stringParameter.hid }],
};

export const ruleKey = 'vendor:king';
export const rule: ParamMappingRule = {
  hypothesis: false,
  deleted: false,
  id: 123,
  marketValues: {
    '7893318': [
      {
        hypothesis: 'KING',
        optionId: 16114919,
      },
    ],
  },
  paramMappingId: simpleMapping.id,
  shopValues: {
    vendor: 'king',
  },
};

export const simpleRules = {
  [ruleKey]: rule as ParamMappingRule,
};

export const simpleMappingWithRule: UiParamMapping = {
  ...simpleMapping,
  rules: simpleRules,
  marketParams: [{ parameterId: 7893318 }],
  editable: true,
};
