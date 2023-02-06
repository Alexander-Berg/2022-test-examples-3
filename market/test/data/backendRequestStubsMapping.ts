import { logisticsParams } from 'src/test/data/logistics';
import { testSuppliers } from 'src/test/data/supplier';
import { warehouse } from 'src/test/data/warehouse';

export const testStubMapping = {
  '/api/logisticsparams/get': logisticsParams,
  '/api/warehouses/get-all': warehouse,
  '/api/supplier': testSuppliers,
  '/api/current-user/favorites': [],
};
