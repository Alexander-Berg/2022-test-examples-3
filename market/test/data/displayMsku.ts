import { DisplayMsku, MskuSource } from 'src/java/definitions';

let nextId = 1000;
const defaultCategoryId = 3;
const defaultVendorId = 2;

export const testDisplayMsku = (msku: Partial<DisplayMsku> = {}): DisplayMsku => {
  const { id = nextId++, categoryId = defaultCategoryId, vendorId = defaultVendorId } = msku;

  return {
    id,
    categoryId,
    categoryName: `category-${categoryId}`,
    title: `msku-${id}`,
    vendorId,
    vendorName: `vendor-${vendorId}`,
    source: MskuSource.MBO_STUFF,
    cargoTypes: [],
    ...msku,
  };
};
