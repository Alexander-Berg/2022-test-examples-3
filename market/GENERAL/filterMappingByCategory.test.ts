import { filterMappingByCategory } from './filterMappingByCategory';
import { categoryData, simpleMapping } from 'src/test/data';

const parentCategory = { ...categoryData, leaf: false };

const categoryByParentHid = {
  [parentCategory.hid]: [categoryData.hid],
};

describe('filterMappingByCategory', () => {
  test('Mapping without category', () => {
    const mappings = [{ ...simpleMapping, categoryId: null }].filter(
      filterMappingByCategory(categoryData, categoryByParentHid)
    );
    expect(mappings).toHaveLength(1);
  });

  test('Mapping with other category', () => {
    const mappings = [{ ...simpleMapping, categoryId: 123456 }].filter(
      filterMappingByCategory(categoryData, categoryByParentHid)
    );
    expect(mappings).toHaveLength(0);
  });

  test('Mapping with children category', () => {
    const mappings = [{ ...simpleMapping, categoryId: categoryData.hid }].filter(
      filterMappingByCategory(parentCategory, categoryByParentHid)
    );
    expect(mappings).toHaveLength(1);
  });
});
