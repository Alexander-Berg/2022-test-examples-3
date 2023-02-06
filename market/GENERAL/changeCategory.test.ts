import { shopModel } from 'src/test/data';
import { getPreparedByCategoryModels, groupedOriginAndUpdatedModels, mergeModelsByCategory } from './changeCategory';

const liteModel = { ...shopModel, marketCategoryId: 1, shopCategoryName: 'sock', description: '', shopValues: {} };

const originModels = [{ ...liteModel, marketCategoryId: 1 }];
const changedModels = [{ ...liteModel, marketCategoryId: 2 }];

const key = '1sock';
const updKey = '2sock';

const modelByCategoryKey = {
  [key]: originModels,
  [updKey]: [{ ...liteModel, marketCategoryId: 2, id: 333 }],
};

describe('categories utils', () => {
  test('getPreparedByCategoryModels', () => {
    const grouped = groupedOriginAndUpdatedModels(originModels, changedModels);
    const { removedModels, updatedModels, addModels } = getPreparedByCategoryModels(grouped);

    expect(removedModels.get(key)[0]).toHaveProperty('marketCategoryId', 1);
    expect(removedModels.get(key)).toHaveLength(1);

    expect(addModels.get(updKey)[0]).toHaveProperty('marketCategoryId', 2);
    expect(addModels.get(updKey)).toHaveLength(1);

    expect(updatedModels.get(key)).toBeUndefined();
  });

  test('mergeModelsByCategory', () => {
    const merge = mergeModelsByCategory(modelByCategoryKey, originModels, changedModels);

    expect(merge[key]).toHaveLength(0);
    expect(merge[updKey]).toHaveLength(2);
  });
});
