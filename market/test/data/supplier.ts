import { MbocSupplierType, Supplier } from 'src/java/definitions';

export const testSupplierIdBase = 69;
let testSupplierId = testSupplierIdBase;

export const testSupplier = (sample: Partial<Supplier> = {}) =>
  ({
    id: ++testSupplierId,
    name: 'Тестовый поставщик',
    domain: '',
    newContentPipeline: true,
    organizationName: '',
    realSupplierId: 'A6RT-DKFN-LE73',
    testSupplier: false,
    type: MbocSupplierType.REAL_SUPPLIER,
    visible: true,
    businessId: 0,
    mbiBusinessId: 0,
    clickAndCollect: false,
    crossdock: false,
    dropship: false,
    dropshipBySeller: false,
    fulfillment: false,
    datacamp: false,
    disableMdm: false,
    ignoreStocks: true,
    yangPriority: 5,
    hideFromToloka: false,
    ...sample,
  } as Supplier);

export const testSuppliers: Supplier[] = [
  testSupplier({ id: testSupplierIdBase }),
  testSupplier({ id: 88, name: '88 Supplier' }),
  testSupplier({ id: 77, name: '-= Поставщик =-' }),
];
