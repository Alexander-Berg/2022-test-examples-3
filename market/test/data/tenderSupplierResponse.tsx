import {
  PriceSpecificationAxaptaSplitDto,
  PriceSpecificationDto,
  PriceSpecificationStatus,
  TenderSupplierSummaryDTO,
} from 'src/java/definitions-replenishment';
import { suppliers } from './replenishmentSuppliers';

export const createTenderSupplierResponse = (
  data: Partial<TenderSupplierSummaryDTO> = {}
): TenderSupplierSummaryDTO => ({
  supplierId: suppliers[0].id,
  supplierName: suppliers[0].name,
  rsId: '',
  demand1pId: null,
  priceSpecJson: '',
  priceSpec: {
    id: '123',
    specs: [
      {
        id: 'zks1',
        mdsUrl: 'mds url 1',
        login: 'login',
        fio: 'fio',
      } as PriceSpecificationAxaptaSplitDto,
    ],
    status: PriceSpecificationStatus.PROCESSING,
    sskus: ['ssku1,ssku2'],
    errors: null,
    orderId: null,
  } as PriceSpecificationDto,
  ...data,
});
