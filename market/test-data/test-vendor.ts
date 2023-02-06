import { Vendor } from '@yandex-market/market-proto-dts/Market/AliasMaker';

import { DEFAULT_CATEGORY_ID } from 'src/shared/test-data/test-constants';

let nextId = 1000;

export const testVendor = (vendor?: Vendor): Vendor => {
  const {
    vendor_id = nextId++,
    local_vendor_id = nextId++,
    category_id = DEFAULT_CATEGORY_ID,
    name = `Vendor ${vendor_id}`,
    ...rest
  } = (vendor || {}) as Vendor;

  return {
    vendor_id,
    local_vendor_id,
    category_id,
    name,
    ...rest,
  };
};
