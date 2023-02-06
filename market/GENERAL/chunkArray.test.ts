import { chunkArray } from './chunkArray';

describe('chunkArray', () => {
  test('even', () => {
    const models = [1, 2, 3, 4, 5, 6];
    const chunked = chunkArray(models, 2);
    expect(chunked.length).toEqual(3);
  });

  test('odd', () => {
    const models = [1, 2, 3, 4, 5, 6, 7];
    const chunked = chunkArray(models, 2);
    expect(chunked.length).toEqual(4);
  });

  test('fewer then chunkSize', () => {
    const models = [1];
    const chunked = chunkArray(models, 10);
    expect(chunked.length).toEqual(1);
  });
});
