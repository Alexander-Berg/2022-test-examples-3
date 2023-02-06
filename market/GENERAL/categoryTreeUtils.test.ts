import { DisplayCategory } from 'src/java/definitions';
import { makeFullPath, makeTree } from 'src/utils/categoryTreeUtils';

describe('Category tree', () => {
  const categories: DisplayCategory[] = [
    { id: 0, name: 'Category 0', parentId: 5 },
    { id: 1, name: 'Category 1', parentId: 5 },
    { id: 5, name: 'Category 5', parentId: 2 },
    { id: 2, name: 'Category 2', parentId: -1 },
    { id: 6, name: 'Category 6' },
    { id: 9, name: 'Category 9', parentId: 6 },
    { id: 11, name: 'Category 11', parentId: 6 },
  ] as DisplayCategory[];

  const result: DisplayCategory[] = [
    { id: 0, name: 'Category 2\\Category 5\\Category 0', parentId: 5 },
    { id: 1, name: 'Category 2\\Category 5\\Category 1', parentId: 5 },
    { id: 2, name: 'Category 2', parentId: -1 },
    { id: 5, name: 'Category 2\\Category 5', parentId: 2 },
    { id: 6, name: 'Category 6' },
    { id: 9, name: 'Category 6\\Category 9', parentId: 6 },
    { id: 11, name: 'Category 6\\Category 11', parentId: 6 },
  ] as DisplayCategory[];

  const resultNoRoot: DisplayCategory[] = [
    { id: 0, name: 'Category 5\\Category 0', parentId: 5 },
    { id: 1, name: 'Category 5\\Category 1', parentId: 5 },
    { id: 5, name: 'Category 5', parentId: 2 },
    { id: 9, name: 'Category 9', parentId: 6 },
    { id: 11, name: 'Category 11', parentId: 6 },
  ] as DisplayCategory[];

  it('make tree full path labels', () => {
    const fullPath = makeFullPath(categories, false);
    expect(Object.values(fullPath)).toEqual(result);
  });

  it('make tree full path labels without root', () => {
    const fullPath = makeFullPath(categories, true);
    expect(Object.values(fullPath)).toEqual(resultNoRoot);
  });

  it('make tree', () => {
    const tree = makeTree(categories);
    expect(tree).toEqual({ '2': { '5': { '0': {}, '1': {} } }, '6': { '11': {}, '9': {} } });
  });
});
