import { Offer } from '@yandex-market/market-proto-dts/Market/AliasMaker';

import { MarkupOffer } from 'src/shared/common-logs/helpers/types';
import { DEFAULT_CATEGORY_ID } from 'src/shared/test-data/test-constants';
import { GskuInput } from 'src/tasks/mapping-moderation/helpers/input-output';

interface TestOfferSetup {
  id?: string;
  supplierId?: string;
  supplierSkuId?: number;
}

let nextId = 1;

export function testOffer(setup: TestOfferSetup & Partial<MarkupOffer> = {}): Offer {
  const { id = `${nextId++}`, supplierId = '42', supplierSkuId, ...rest } = setup;

  return {
    offer_id: id,
    title: `Test offer ${id}`,
    shop_category_name: 'shop category',
    shop_id: supplierId,
    shop_name: `Supplier #${supplierId}`,
    supplier_name: `Supplier #${supplierId}`,
    category_id: DEFAULT_CATEGORY_ID,
    category_name: 'Test category',
    processing_status_ts: 1550345534041,
    manufacturer: 'Дисней Герман',
    ...(supplierSkuId ? { supplier_mapping_info: { sku_id: supplierSkuId } } : {}),
    ...rest,
  } as any;
}

interface GskuSetup {
  supplierSkuId: number;
}

export function testGskuInput(gsku: GskuSetup & Partial<GskuInput> & Pick<GskuInput, 'generated_sku_id'>): GskuInput {
  const { id = `${nextId}`, category_id = `${DEFAULT_CATEGORY_ID}`, supplierSkuId, ...rest } = gsku;

  return {
    id,
    category_id,
    category_name: `Category ${category_id}`,
    supplier_mapping_info: { sku_id: `${supplierSkuId}` },
    ...rest,
  };
}
