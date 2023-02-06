import { Warehouse, WarehouseType } from 'src/java/definitions';

export const warehouse: Warehouse[] = [
  {
    id: 102,
    name: 'Склад 2',
    modifiedAt: '2019-06-01',
    type: WarehouseType.FULFILLMENT,
  },
  {
    id: 103,
    name: 'Склад вт/',
    modifiedAt: '2019-06-01',
    type: WarehouseType.FULFILLMENT,
  },
  {
    id: 104,
    name: 'Склад',
    modifiedAt: '2019-06-01',
    type: WarehouseType.FULFILLMENT,
  },
  {
    id: 108,
    name: 'Дальний Склад',
    modifiedAt: '2019-06-01',
    type: WarehouseType.FULFILLMENT,
  },
];
