import { ShopModelValidationType, ShopModelValidationSource } from 'src/java/definitions';

export const errors = [
  {
    message: 'Не белый фон',
    validationType: ShopModelValidationType.AG_INVALID_PICTURE,
  },
  {
    message: 'Не та категория',
    validationType: ShopModelValidationType.CATEGORY,
  },
  {
    message: 'Мультизначение не в мультипараметрее',
    validationType: ShopModelValidationType.AG_TOO_MANY_VALUES_IN_PARAM,
    ref: {
      parameterId: 1,
    },
  },
  {
    message: 'Не заполнено обязательное поле',
    validationType: ShopModelValidationType.REQUIRED_PARAMETER,
    ref: {
      parameterId: 2,
    },
  },
];

export const validationResult = {
  errors,
  shopModelId: 1,
  source: ShopModelValidationSource.MERGED,
  valid: true,
};
