import { Warehouse, WarehouseType, WarehouseUsingType } from 'src/java/definitions';

export const warehouses = [
  {
    id: 145,
    name: 'Cклад #1',
    cargoTypeLmsIds: [960, 0, 320, 450, 100, 485, 230, 40, 200, 970, 490, 780, 150, 470, 310, 440, 220],
    modifiedAt: '2019-08-20T13:04:43.988',
  },
  {
    id: 147,
    name: 'Яндекс.Маркет Ростов',
    cargoTypeLmsIds: [
      320, 960, 0, 450, 770, 520, 200, 970, 780, 910, 20, 150, 470, 790, 220, 100, 485, 230, 40, 490, 300, 750, 310,
      950, 440, 700,
    ],
    modifiedAt: '2019-08-20T13:04:43.995',
  },
  {
    id: 163,
    name: 'Лаборатория Контента',
    cargoTypeLmsIds: null,
    modifiedAt: '2019-08-22T13:47:46.894932',
  },
  {
    id: 171,
    name: 'Яндекс Маркет Томилино',
    cargoTypeLmsIds: [
      960, 0, 320, 770, 450, 100, 485, 230, 40, 520, 200, 970, 490, 780, 300, 750, 910, 20, 470, 310, 950, 440, 220,
    ],
    modifiedAt: '2019-08-20T13:04:43.996',
  },
  {
    id: 172,
    name: 'Яндекс Маркет Софьино',
    cargoTypeLmsIds: [
      320, 960, 0, 450, 770, 520, 200, 970, 780, 910, 20, 150, 470, 790, 220, 100, 485, 230, 40, 490, 300, 750, 310,
      950, 440, 700,
    ],
    modifiedAt: '2019-08-20T13:04:43.996',
  },
  {
    id: 999999999,
    name: 'PEK50 x-dock',
    cargoTypeLmsIds: null,
    modifiedAt: '2019-08-20T13:04:43.996',
  },
];

export const warehouse1 = {
  id: 100,
  name: 'wh100',
  usingType: WarehouseUsingType.USE_FOR_FULFILLMENT,
  calendaringEnabled: false,
  type: WarehouseType.FULFILLMENT,
  modifiedAt: '2019-08-20T13:04:43.996',
};

const warehouse2 = {
  id: 200,
  name: 'wh200',
  usingType: WarehouseUsingType.USE_FOR_FULFILLMENT,
  calendaringEnabled: false,
  type: WarehouseType.FULFILLMENT,
  modifiedAt: '2019-08-20T13:04:43.996',
};

export const twoTestWarehouses = [warehouse1, warehouse2] as Warehouse[];
