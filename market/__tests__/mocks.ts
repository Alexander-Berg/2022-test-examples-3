import {MenuBody} from '../Menu'

export const PLACE_ID = 162

export const REVISION = 'MS4xNjA5NDU5MjAwMDAwLmRkMzJ5a0dqaDhZbl9VSzRXa1dtSVE'

export const CATEGORIES: MenuBody['categories'] = [
  {
    available: true,
    id: '103263',
    name: 'Завтрак',
    sortOrder: 130,
    originalAvailable: true,
    originalReactivatedAt: null,
    reactivatedAt: null,
    scheduleDescription: null
  },
  {
    available: true,
    id: '103265',
    name: 'Закуски',
    sortOrder: 160,
    originalAvailable: true,
    originalReactivatedAt: null,
    reactivatedAt: null,
    scheduleDescription: null
  },
  {
    available: true,
    id: '103281',
    name: 'Напитки',
    sortOrder: 310,
    originalAvailable: true,
    originalReactivatedAt: null,
    reactivatedAt: null,
    scheduleDescription: null
  }
]

export const DEFAULT_CATEGORY = ['Завтрак', 'Закуски']

export const ITEMS: MenuBody['items'] = [
  {
    available: false,
    categoryId: '103263',
    description: 'Сухофрукты',
    id: '1234583',
    images: [{url: 'https://testing.eda.tst.yandex.net/images/1370147/36ca994761eb1fd00066ac634c96e0d9.jpeg'}],
    thumbnails: [{url: 'https://testing.eda.tst.yandex.net/images/1368744/e7368825a608169ae33ae8ce67952f14.jpeg'}],
    measure: 35,
    measureUnit: 'г',
    name: 'Сухофрукты',
    price: 115,
    sortOrder: 100,
    vat: 20
  },
  {
    available: true,
    categoryId: '103265',
    description:
      'Слабосоленый лосось, сливочный сыр с зеленью, малосольные огурцы, каперсы, красный лук, шнитт-лук, укроп, оливковое масло. Подается с миксом зелени',
    id: 'kbjnh7uv-vppe5701y6-8woqyfh7ic9',
    images: [{url: 'https://testing.eda.tst.yandex.net/images/1368744/e7368825a608169ae33ae8ce67952f14.jpeg'}],
    thumbnails: [{url: 'https://testing.eda.tst.yandex.net/images/1368744/e7368825a608169ae33ae8ce67952f14.jpeg'}],
    measure: 130,
    measureUnit: 'г',
    name: 'Смерребред с лососем',
    price: 590,
    sortOrder: 0,
    vat: 10
  },
  {
    available: true,
    categoryId: '103281',
    description: 'Классический латте от Кофемании',
    id: 'kgibo90y-sebvmwt8kh-q9pj0wgpz5h',
    images: [{url: 'https://testing.eda.tst.yandex.net/images/3806315/978224124f8da07d0743281899e6e4cb.jpeg'}],
    thumbnails: [{url: 'https://testing.eda.tst.yandex.net/images/1368744/e7368825a608169ae33ae8ce67952f14.jpeg'}],
    measure: 300,
    measureUnit: 'мл',
    name: 'Латте',
    price: 470,
    sortOrder: 100,
    vat: -1
  }
]

export const MENU = {
  menu: {
    categories: CATEGORIES,
    items: ITEMS,
    lastChange: '2021-09-02T14:31:54.146325+00:00'
  },
  previous_result: {revision: '', status: 'not_applicable', status_type: 'fail'},
  revision: REVISION
}

export const UPDATE_RESPONSE = {
  revision: 'MS4xNjMxMDExODg1MzkxLkZlaHhuOFpKbi1PVTJpQ1RoOGNucWc',
  status: 'processing',
  status_type: 'intermediate'
}
