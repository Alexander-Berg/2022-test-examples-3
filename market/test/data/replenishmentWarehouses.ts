import { Warehouse, WarehouseType } from 'src/java/definitions-replenishment';

export const warehouses: Warehouse[] = [
  {
    id: 102,
    code: 102,
    name: 'Склад 2',
    type: WarehouseType.XDOC,
    region: {
      id: 145,
      name: 'MSK',
    },
  },
  {
    id: 103,
    code: 103,
    name: 'Склад вт/',
    type: WarehouseType.FULFILLMENT,
    region: {
      id: 145,
      name: 'MSK',
    },
  },
  {
    id: 104,
    code: 104,
    name: 'Склад',
    type: WarehouseType.FULFILLMENT,
    region: {
      id: 145,
      name: 'MSK',
    },
  },
  {
    id: 108,
    code: 108,
    name: 'Дальний Склад',
    type: WarehouseType.FULFILLMENT,
    region: {
      id: 147,
      name: 'Rostov',
      fallbackRegionId: 145,
    },
  },
];
