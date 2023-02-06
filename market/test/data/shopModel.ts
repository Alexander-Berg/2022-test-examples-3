import { UiShopModel } from 'src/utils/types';
import {
  ApprovedSkuMapping,
  CategoryRestrictionType,
  MarketParameterValue,
  MigratingStatus,
  Picture,
  ProcessingStatus,
  RatingKind,
  ShopModelAvailability,
  ShopModelProcessingStatus,
  ShopModelValidationSource,
  ShopModelValidationType,
  SkuType,
  UpdateStatus,
  ValueSource,
} from 'src/java/definitions';
import { VENDOR_PARAMETER_ID } from 'src/constants';
import { parameter } from './categoryData';

export const approvedSkuMapping: ApprovedSkuMapping = {
  skuId: 0,
  categoryId: 0,
  timestamp: '1970-01-01T03:00:00',
  skuType: SkuType.TYPE_MARKET,
};

export const description = `Камень точильный водный комбинированный #1000/#6000 King KHKW-065  Зернистость #1000/#6000. Размеры 185*63*25 мм. С подставкой.  Предназначен для заточки и шлифовки угла заточки, для полировки угла заточки режущей кромки. Инструкция Мы рекомендуем регулярно проводить правку ножей на керамическом мусате или точильном камне. Мусат - это стержень, сделанный из очень твердой стали или из обычной стали, но с напылением алмазного покрытия или керамики. Во время правки ведите по мусату режущей кромкой ножа от себя, плавно сдвигая нож от рукоятки к острию. Точильные камни имеют разную степень зернистости:   грубые - зернистость до 1000 единиц - используются для востановления правильного угла заточки и формы режущей кромки; средние - зернистость 1000 - 3000 единиц - используются для заточки как таковой; тонкие - выше 3000 единиц - используются для чистовой правки ножа.  Перед использованием точильного каменя его необходимо полностью погрузить в воду на 10-15 минут. Во время заточки точильный камень должен быть немного влажным.  При заточке японских ножей с односторонней заточкой, сначала необходимо затачивать выступающий спуск, до появления равномерного заусенца, а только потом с небольшим нажимом несколько раз провести по камню вогнутой стороной. При заточке ножей с "двухсторонне- симметричной" заточкой, сначала затачивайте одну сторону, до появления заусенца, потом приступите к заточке другой стороны ножа. При необходимости повторите операции на более мелкозернистом камне. Затачивайте нож так долго, как это требуется для достижения остроты ножа. При работе следите за тем, чтобы сохранялся первоначальный угол заточки в 15-17 градусов, заданный при производстве.`;

export const formalizationValues: MarketParameterValue[] = [
  {
    parameterId: parameter.id,
    ruleId: 0,
    valPos: { src: '#Описание (ассортимент)', s: 183, e: 192, ps: 156, pe: 165, us: -1, ue: -1 },
    value: { optionId: 16114919, hypothesis: 'полировка' },
    valueSource: 'FORMALIZATION' as ValueSource,
  },
  {
    parameterId: parameter.id,
    ruleId: 0,
    valPos: { src: '#Описание (ассортимент)', s: 146, e: 153, ps: -1, pe: -1, us: -1, ue: -1 },
    value: { optionId: 16115322, hypothesis: 'заточка' },
    valueSource: ValueSource.FORMALIZATION,
  },
];

export const vendorRules = {
  parameterId: 7893318,
  ruleId: 123,
  valPos: undefined,
  value: {
    hypothesis: 'king',
    optionId: 161,
  },
  valueSource: ValueSource.RULE,
};

export const manualValues = [
  {
    parameterId: 7893318,
    ruleId: 123,
    valPos: undefined,
    value: {
      hypothesis: 'king',
      optionId: 161,
    },
    valueSource: ValueSource.MANUAL,
  },
];

export const nameFormalizationValue = {
  parameterId: parameter.id,
  ruleId: 0,
  valPos: { src: '#Описание (ассортимент)', s: 183, e: 192, ps: 156, pe: 165, us: -1, ue: -1 },
  value: { optionId: 16114919, hypothesis: 'полировка' },
  valueSource: ValueSource.FORMALIZATION,
};

export const vendorFormalizationValue = {
  ...vendorRules,
  ruleId: 0,
  valueSource: ValueSource.FORMALIZATION,
};

export const ruleHypotheses = {
  ...vendorRules,
  valueSource: ValueSource.FORMALIZATION,
};

export const rules = [vendorRules];

export const marketValues = {
  [parameter.id]: formalizationValues,
  [VENDOR_PARAMETER_ID]: rules,
};

export const pictures: Picture[] = [
  {
    forceFirst: true,
    mappingId: 12538,
    url: 'https://img.best-kitchen.ru/images/products/1/7031/77601655/3.jpg',
    valueSource: ValueSource.RULE,
  },
];

export const shopValues: { [index: string]: string } = {
  SKU: '57837981',
  vendor: 'KING',
  Материал: 'Керамика',
  'Категория*': '(id:9419025)KING - точильные камни',
  'Название*': 'Камень точильный водный комбинированный #1000/#6000 King KHKW-065',
  '#Описание (ассортимент)': description,
  Покрытие: 'антиналип',
  Фото: 'https://img.best-kitchen.ru/images/products/1/7031/77601655/3.jpg',
  '#Название (ассортимент)': 'Ассортиментное название',
};

export const validationResult = {
  blockExport: false,
  shopModelId: 4017363,
  valid: true,
  errors: [],
  source: ShopModelValidationSource.MERGED,
};

export const shopModel: UiShopModel = {
  version: 1,
  marketValues,
  approvedSkuMapping,
  pictures,
  description,
  shopValues,
  validationResult,
  id: 4017363,
  name: 'Камень точильный водный комбинированный #1000/#6000 King KHKW-065',
  availability: ShopModelAvailability.ACTIVE,
  barCode: '',
  created: '2020-10-09T01:34:35.801564',
  fillRating: 20,
  fillRatingKind: RatingKind.MINIMAL,
  manualPictures: false,
  marketCategoryChecked: false,
  marketCategoryConfidence: 0.5,
  marketCategoryId: 14246003,
  processingStatus: ProcessingStatus.IN_WORK,
  shopCategoryName: '(id:9419025)KING - точильные камни',
  shopId: 10451772,
  shopModelCreationAllowed: true,
  shopModelProcessingStatus: ShopModelProcessingStatus.NOT_SENT,
  shopSku: '57837981',
  shopUrl: 'https://img.best-kitchen.ru/images/products/1/7031/77601655/3.jpg',
  shopVendor: 'KING',
  updatedFromCT: '2020-10-12T19:32:41.647968',

  marketCategoryName: 'Очистители и увлажнители воздуха #734595',
  marketFullCategoryName: 'Очистители и увлажнители воздуха #734595',
  marketParameterNames: [],
  marketParameterCount: 1,
  marketImportantParameterCount: 1,
  marketFilterParameterCount: 1,
  marketImportantParameterTotal: 1,
  marketFilterParameterTotal: 1,
  categoryConfidenceValue: 'checked',

  categoryRestriction: { type: CategoryRestrictionType.ALLOW_ANY },
};

export const agValidations = {
  shopModelId: shopModel.id,
  source: ShopModelValidationSource.MERGED,
  valid: false,
  errors: [
    {
      message: 'ошибка категории',
      validationType: ShopModelValidationType.AG_CATEGORY_RESTRICTION,
    },
    {
      message: 'ошибка картинки',
      validationType: ShopModelValidationType.AG_FILE_IS_NOT_IMAGE,
    },
  ],
};

export const modelWithFormalization = {
  ...shopModel,
  marketValues: {
    '16114895': formalizationValues,
  },
};

export const modelWithRules = {
  ...shopModel,
  marketValues: {
    [VENDOR_PARAMETER_ID]: rules,
  },
};

export const shops = [
  {
    id: 1,
    name: 'MyShop',
    externalId: 1,
    businessId: 1,
    datacamp: false,
    locked: false,
    migratingStatus: MigratingStatus.NOT_MIGRATED,
    updateStatus: UpdateStatus.ENABLED,
    updateTemporallyDisabled: false,
    updatedByFeed: true,
  },
];
