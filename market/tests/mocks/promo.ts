import * as R from 'ramda';

import { MechanicsType, PromoStatus, SupplierType } from 'src/java/definitions-promo';
import { Promo } from 'src/pages/promo/types/promo';

const PROMO_WARNINGS = ['Ошибка в параметрах участия', 'Изменения не опубликованы'];

interface CategoryMock {
  id: number;
  discount: number;
  name: string;
  parentId: number;
  published: boolean;
}

export const FIRST_CATEGORY_MOCK: CategoryMock = {
  id: 91491,
  discount: 10,
  name: 'First category',
  parentId: 43,
  published: true,
};

export const SECOND_CATEGORY_MOCK: CategoryMock = {
  id: 90639,
  discount: 20,
  name: 'Second category',
  parentId: 42,
  published: true,
};

function convertCategoryToCollectionMock(category: CategoryMock) {
  return R.omit(['discount'], category);
}
export const CATEGORIES_COLLECTION_MOCK = {
  [FIRST_CATEGORY_MOCK.id]: convertCategoryToCollectionMock(FIRST_CATEGORY_MOCK),
  [SECOND_CATEGORY_MOCK.id]: convertCategoryToCollectionMock(SECOND_CATEGORY_MOCK),
};

export const DIRECT_DISCOUNT_PROMO: Promo<MechanicsType.DIRECT_DISCOUNT> = {
  assortmentDeadline: '2020-12-05',
  categoryIds: [FIRST_CATEGORY_MOCK.id, SECOND_CATEGORY_MOCK.id],
  id: 'direct-discount$8376f323-d8a4-e05c-2110-bedcb7bd37fc',
  humanReadableId: '#4242',
  description: '',
  name: 'TEST PROMO MBI ПС1',
  offersCount: 51,
  period: {
    from: '2020-12-08',
    to: '2020-12-15',
  },
  status: PromoStatus.CREATED,
  trade: null,
  updatedAt: '2020-12-02',
  supplierIds: [],
  supplierTypes: [SupplierType._1P, SupplierType._3P],
  warnings: PROMO_WARNINGS,
  system: false,
  mechanics: {
    type: MechanicsType.DIRECT_DISCOUNT,
    minimalDiscountPercentSize: 10,
    categoriesWithDiscounts: [
      {
        categoryId: FIRST_CATEGORY_MOCK.id,
        discount: FIRST_CATEGORY_MOCK.discount,
      },
      {
        categoryId: SECOND_CATEGORY_MOCK.id,
        discount: SECOND_CATEGORY_MOCK.discount,
      },
    ],
  },
};
