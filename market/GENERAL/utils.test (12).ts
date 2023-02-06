import * as R from 'ramda';

import { createContentType } from '../entries';
import { getAllowedContentTypes } from './utils';

describe('models/contentTypes/utils', () => {
  describe('getAllowedContentTypes', () => {
    it('should return all children of root', () => {
      const root = createContentType('ROOT', {
        fields: [{ name: 'ROOT_FIELD_1', properties: { allowedTypes: ['LEVEL_1'] } }],
      });
      const level1 = createContentType('LEVEL_1', {
        fields: [
          { name: 'LEVEL_1_FIELD_1', properties: { allowedTypes: ['LEVEL_2_1'] } },
          { name: 'LEVEL_1_FIELD_2', properties: { allowedTypes: ['LEVEL_2_2'] } },
        ],
      });
      const level21 = createContentType('LEVEL_2_1', { fields: [{ name: 'LEVEL_2_1_FIELD_1', properties: {} }] });
      const level22 = createContentType('LEVEL_2_2', { fields: [{ name: 'LEVEL_2_2_FIELD_1', properties: {} }] });

      const contentTypes = R.indexBy(R.prop('id'), [root, level1, level21, level22]);
      const result = getAllowedContentTypes('ROOT', contentTypes);

      expect(result).toHaveLength(4);
      expect(result).toEqual(expect.arrayContaining(['ROOT', 'LEVEL_1', 'LEVEL_2_1', 'LEVEL_2_2']));
    });

    it('should return subtree ids', () => {
      const root = createContentType('ROOT', {
        fields: [{ name: 'ROOT_FIELD_1', properties: { allowedTypes: ['LEVEL_1'] } }],
      });
      const level1 = createContentType('LEVEL_1', {
        fields: [
          { name: 'LEVEL_1_FIELD_1', properties: { allowedTypes: ['LEVEL_2_1'] } },
          { name: 'LEVEL_1_FIELD_2', properties: { allowedTypes: ['LEVEL_2_2'] } },
        ],
      });
      const level21 = createContentType('LEVEL_2_1', { fields: [{ name: 'LEVEL_2_1_FIELD_1', properties: {} }] });
      const level22 = createContentType('LEVEL_2_2', { fields: [{ name: 'LEVEL_2_2_FIELD_1', properties: {} }] });

      const contentTypes = R.indexBy(R.prop('id'), [root, level1, level21, level22]);

      const result = getAllowedContentTypes('LEVEL_1', contentTypes);

      expect(result).toHaveLength(3);
      expect(result).toEqual(expect.arrayContaining(['LEVEL_1', 'LEVEL_2_1', 'LEVEL_2_2']));
    });

    it('should skip same ids', () => {
      const root = createContentType('ROOT', {
        fields: [
          { name: 'ROOT_FIELD_1', properties: { allowedTypes: ['LEVEL_1'] } },
          { name: 'ROOT_FIELD_2', properties: { allowedTypes: ['LEVEL_1'] } },
        ],
      });
      const level1 = createContentType('LEVEL_1', { fields: [] });

      const contentTypes = R.indexBy(R.prop('id'), [root, level1]);
      const result = getAllowedContentTypes('ROOT', contentTypes);

      expect(result).toHaveLength(2);
      expect(result).toEqual(expect.arrayContaining(['ROOT', 'LEVEL_1']));
    });

    it('should skip invalid id', () => {
      const root = createContentType('ROOT', {
        fields: [{ name: 'ROOT_FIELD_1', properties: { allowedTypes: ['LEVEL_1'] } }],
      });

      const contentTypes = R.indexBy(R.prop('id'), [root]);
      const result = getAllowedContentTypes('ROOT', contentTypes);

      expect(result).toHaveLength(1);
      expect(result).toEqual(expect.arrayContaining(['ROOT']));
    });

    it('should collect ids from selectors', () => {
      const root = createContentType('ROOT', {
        fields: [{ name: 'ROOT_FIELD_1', properties: {} }],
        selectors: [
          { selector: 'TEST_1', properties: { allowedTypes: ['CHILD_1'] } },
          { selector: 'TEST_2', properties: { allowedTypes: ['CHILD_2'] } },
          { selector: 'TEST_3', properties: {} },
        ],
      });
      const child1 = createContentType('CHILD_1', { fields: [] });
      const child2 = createContentType('CHILD_2', { fields: [] });

      const contentTypes = R.indexBy(R.prop('id'), [root, child1, child2]);
      const result = getAllowedContentTypes('ROOT', contentTypes);

      expect(result).toHaveLength(3);
      expect(result).toEqual(expect.arrayContaining(['ROOT', 'CHILD_1', 'CHILD_2']));
    });
  });
});
