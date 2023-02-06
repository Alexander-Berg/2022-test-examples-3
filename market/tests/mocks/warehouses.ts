import { twoTestWarehouses } from 'src/test/data';

const WAREHOUSES_STOCKS = {
  [twoTestWarehouses[0].id]: 123,
  [twoTestWarehouses[1].id]: 321,
};

export const WAREHOUSES_STOCKS_BY_ID = twoTestWarehouses.reduce((acc, warehouse) => {
  acc[warehouse.id] = WAREHOUSES_STOCKS[warehouse.id];
  return acc;
}, {});
