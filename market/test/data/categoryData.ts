import { groupBy } from 'src/utils/groupBy';
import { Parameter, ParameterImportance, ValueType } from 'src/java/definitions';
import { UiCategoryData, UiCategoryInfo } from 'src/utils/types';
import { VENDOR_PARAMETER_ID } from 'src/constants';

export const options = [
  { id: 16114919, name: 'полировка', aliases: [] },
  { id: 16115322, name: 'заточка', aliases: [] },
  { id: 16114896, name: 'шлифовка', aliases: [] },
  { id: 16114897, name: 'правка', aliases: [] },
  { id: 16114939, name: 'доводка', aliases: [] },
];

export const parameter: Parameter = {
  id: 16114895,
  important: true,
  mandatoryForSignature: false,
  multi: true,
  name: 'Назначение',
  options,
  parameterImportance: ParameterImportance.FILTER,
  service: false,
  usedInFilter: true,
  valueType: ValueType.ENUM,
  xslName: 'use',
  comment: 'Параметр назначение',
};

export const vendorOptions = [
  { id: 161, name: 'king', aliases: [] },
  { id: 162, name: 'stake', aliases: [] },
];

export const vendorParameter = {
  id: VENDOR_PARAMETER_ID,
  hid: 7893318,
  important: false,
  mandatoryForSignature: false,
  multi: true,
  name: 'Торговая марка',
  options: vendorOptions,
  parameterImportance: ParameterImportance.FILTER,
  service: false,
  usedInFilter: true,
  valueType: ValueType.ENUM,
  xslName: 'vendor',
};

export const numericParameter = {
  ...vendorParameter,
  hid: 123,
  valueType: ValueType.NUMERIC,
  options: [],
  multi: false,
};

export const stringParameter = {
  ...vendorParameter,
  hid: 124,
  valueType: ValueType.STRING,
  options: [],
  multi: false,
};

export const categoryData = {
  hid: 14246003,
  leaf: true,
  name: 'Мусаты, точилки, точильные камни',
  parameters: [parameter, vendorParameter],
  parentHid: 7330336,
  parametersMap: {
    [parameter.id]: parameter,
    [vendorParameter.id]: vendorParameter,
    [numericParameter.hid]: numericParameter,
    [stringParameter.hid]: stringParameter,
  },
  optionsMap: groupBy([...options, ...vendorOptions], el => el.id),
  visibleParameters: new Set(options.map(el => el.id)),
} as UiCategoryData;

export const categoryInfo = {
  hid: 14246003,
  isLeaf: true,
  name: 'Мусаты, точилки, точильные камни',
  fullName: 'Точильные камни',
  parentHid: 7330336,
  acceptGoodContent: true,
  notUsed: false,
  uniqueName: 'Мусаты, точилки, точильные камни',
  published: true,
  inCategory:
    'мусат, точилка механическая, набор для заточки ножей, точильная система, камень для точильной системы, электрическая точилка для домашнего использования, модуль точильный (сменный), точильный камень (водный камень) и алмазный брусок, карманная точилка, точилка для ножниц, универсальная точилка, подходящая в том числе и для ножей, сумки для точилок и точильных камней, держатель угла заточки, аксессуары для точильных систем, масло для заточки, карта для заточки, паста для заточки',
  outOfCategory:
    'набор ножей с точилкой (мусатом), профессиональные точильные системы и станки, точильные машины, запчасти и точильные круги к ним (критерии "профессиональности": позиционирование производителя, высокая цена, избыточная производительность для бытовых условий), точилка для садовых инструментов (лопат, топоров, кос, секаторов), ремень для заточки, надфили, в том числе для заточки ножей',
} as UiCategoryInfo;

export const categoryStat = {
  allowCreate: 0,
  canExport: 0,
  hid: categoryInfo.hid,
  needContent: 0,
  rating: 0,
  restrictForExport: 0,
  total: 0,
  withErrors: 0,
  withFormalization: 0,
};
