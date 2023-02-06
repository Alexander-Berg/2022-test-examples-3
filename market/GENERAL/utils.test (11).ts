import { createReferenceType } from './utils';
import { EntityType } from './types';

describe('models/common/utils', () => {
  describe('createReferenceType', () => {
    it('should return valid reference type', () => {
      const ref = createReferenceType('12345', EntityType.ContentType);

      expect(ref).toEqual({
        type: EntityType.Reference,
        referenceType: EntityType.ContentType,
        id: '12345',
      });
    });
  });
});
