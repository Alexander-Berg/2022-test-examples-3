import { MechanicsType, WarningCode } from 'src/java/definitions-promo';
import { Offer } from 'src/pages/promo/types/offer';
import { WAREHOUSES_STOCKS_BY_ID } from './warehouses';

const OFFER_WARNINGS = [
  {
    code: WarningCode.TICK_NOT_SET,
    message: 'Warning text 1',
  },
  {
    code: WarningCode.BASE_PRICE_NOT_EXISTS,
    message: 'Warning text 2',
  },
];

const OFFER_DISABLE_SOURCES = ['First disable source', 'Second disable source'];

const DIRECT_DISCOUNT_OFFER_SSKU = '10281764-sku-tv-sony';

export const DIRECT_DISCOUNT_OFFER_MOCK: Offer<MechanicsType.DIRECT_DISCOUNT> = {
  id: '164$10281764-sku-tv-sony',
  categoryId: 90639,
  msku: 13862494,
  ssku: DIRECT_DISCOUNT_OFFER_SSKU,
  name: 'Телевизор Sony KDL-32WD756 31.5" (2016)',
  participates: false,
  warnings: OFFER_WARNINGS,
  price: 100,
  basePrice: 222,
  disabled: false,
  disabledSources: OFFER_DISABLE_SOURCES,
  multiPromo: false,
  availableCount: 123,
  warehouseId: 164,
  shopId: 10281764,
  mechanics: {
    type: MechanicsType.DIRECT_DISCOUNT,
    minimalDiscountPercentSize: 20,
    fixedBasePrice: 333,
    fixedPrice: 111,
  },
  toThisOfferFilterQueryPart: `sskuIds=${DIRECT_DISCOUNT_OFFER_SSKU}`,
  stocksByWarehouse: WAREHOUSES_STOCKS_BY_ID,
  actualPromos: [],
};
