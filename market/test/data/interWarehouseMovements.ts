import { InterWarehouseMovement } from 'src/java/definitions-replenishment';

export const movements: InterWarehouseMovement[] = [
  {
    warehouseIdFrom: 102,
    warehouseIdTo: 104,
    exportedTs: '2020-02-17',
    authorId: 777,
    transportationTaskId: 1,
    id: 1,
  },
  {
    warehouseIdFrom: 103,
    warehouseIdTo: 104,
    exportedTs: '2020-02-18',
    authorId: 777,
    transportationTaskId: 2,
    id: 2,
  },
];
