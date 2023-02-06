import { PUBLISHING_OFFERS, SAVING_OFFERS } from './offers';

export const PROMO_TYPES = {
  DIRECT_DISCOUNT: 'DIRECT_DISCOUNT',
  CHEAPEST_AS_GIFT: 'CHEAPEST_AS_GIFT',
}

export const PUBLISHING_PROMOS = {
  cheapestAsGift910Hidden: {
    anaplanId: '#6305',
    type: PROMO_TYPES.CHEAPEST_AS_GIFT,
    promoId: 'cheapest-as-gift$3976868b07b3232ce3189bca83fb2e22-IzYzMDU=',
    files: {
      base: '#6305CheapestAsGift9=10HiddenBase.xlsx',
      changed: '#6305CheapestAsGift9=10HiddenChanged.xlsx',
    },
    offers: [
      PUBLISHING_OFFERS.OFFER1,
    ],
  },
  directDiscount: {
    anaplanId: '#6309',
    promoId: 'direct-discount$7079a2508d0c7b84f7a3b02d803a6799-IzYzMDk=',
    type: PROMO_TYPES.DIRECT_DISCOUNT,
    files: {
      base: '#6309DirectDiscountBase.xlsx',
      changed: '#6309DirectDiscountChanged.xlsx',
    },
    offers: [
      PUBLISHING_OFFERS.OFFER2,
      PUBLISHING_OFFERS.OFFER3,
      PUBLISHING_OFFERS.OFFER4,
      PUBLISHING_OFFERS.OFFER5,
      PUBLISHING_OFFERS.OFFER6,
      PUBLISHING_OFFERS.OFFER7,
      PUBLISHING_OFFERS.OFFER8,
    ],
  },
  cheapestAsGift23: {
    anaplanId: '#6327',
    type: PROMO_TYPES.CHEAPEST_AS_GIFT,
    promoId: 'cheapest-as-gift$8383f5666cbe2be24376e499ae1bf624-IzYzMjc=',
    files: {
      base: '#6327CheapestAsGift2=3Base.xlsx',
      changed: '#6327CheapestAsGift2=3Changed.xlsx'
    },
    offers: [
      PUBLISHING_OFFERS.OFFER9,
      PUBLISHING_OFFERS.OFFER10,
    ],
  },
  cheapestAsGift34: {
    anaplanId: '#6336',
    type: PROMO_TYPES.CHEAPEST_AS_GIFT,
    promoId: 'cheapest-as-gift$839b1449f3eb8b5eddb1d1a9a9c82239-IzYzMzY=',
    files: {
      base: '#6336CheapestAsGift4=5Base.xlsx',
      changed: '#6336CheapestAsGift4=5Changed.xlsx'
    },
    offers: [
      PUBLISHING_OFFERS.OFFER11,
      PUBLISHING_OFFERS.OFFER12,
    ]
  },
};

export const SAVING_PROMOS = {
  directDiscount: {
    anaplanId: '#6370',
    promoId: 'direct-discount$b9096448231918b3b8b4b1d42c6e6a20-IzYzNzA=',
    cases: [
      {
        name: 'При проставленной галочке без цен есть ошибка',
        offer: SAVING_OFFERS.REQUIRED_PARAMS_OFFER_1,
      },
      {
        name: 'При проставленной только зачеркнутой цене по акции есть ошибка',
        offer: SAVING_OFFERS.REQUIRED_PARAMS_OFFER_2,
      },
      {
        name: 'При проставленной только цене по акции есть ошибка',
        offer: SAVING_OFFERS.REQUIRED_PARAMS_OFFER_3,
      },
      {
        name: 'Нет ошибок при обеих проставленных ценах',
        offer: SAVING_OFFERS.REQUIRED_PARAMS_OFFER_4,
      },
      {
        name: 'Ошибка при скидке меньше минимальной в категории',
        offer: SAVING_OFFERS.MINIMAL_DISCOUNT_OFFER_1,
      },
      {
        name: 'Нет ошибка при скидке больше минимальной в категории',
        offer: SAVING_OFFERS.MINIMAL_DISCOUNT_OFFER_2,
      },
      {
        name: 'Есть ошибка при цене продажи менее 1р',
        offer: SAVING_OFFERS.MINIMAL_PRICE_OFFER_1,
      },
      {
        name: 'Нет ошибки при цене продажи 1',
        offer: SAVING_OFFERS.MINIMAL_PRICE_OFFER_2,
      },
    ]
  },
};
