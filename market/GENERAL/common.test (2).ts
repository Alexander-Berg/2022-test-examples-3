import { isEntryReference, isArrayEntryReference, isValidValue } from './common';
import { EntityType } from '../models';

describe('utils/common', () => {
  describe('isEntryReference', () => {
    it('should return true', () => {
      expect(
        isEntryReference({
          type: EntityType.Reference,
          referenceType: EntityType.Entry,
          id: '1',
        })
      ).toBe(true);
    });

    it('should return false', () => {
      expect(isEntryReference(undefined)).toBe(false);
      expect(isEntryReference({})).toBe(false);
      expect(isEntryReference('')).toBe(false);
      expect(
        isEntryReference({
          type: EntityType.Reference,
          referenceType: EntityType.ContentType,
          id: '1',
        })
      ).toBe(false);
      expect(
        isEntryReference({
          type: EntityType.Reference,
          referenceType: EntityType.Entry,
        })
      ).toBe(false);
      expect(
        isEntryReference({
          type: EntityType.Reference,
          referenceType: EntityType.Entry,
          id: 1,
        })
      ).toBe(false);
    });
  });

  describe('isArrayEntryReference', () => {
    it('should return true', () => {
      expect(isArrayEntryReference([])).toBe(true);
      expect(
        isArrayEntryReference([
          {
            type: EntityType.Reference,
            referenceType: EntityType.Entry,
            id: '1',
          },
        ])
      ).toBe(true);
    });

    it('should return false', () => {
      expect(isArrayEntryReference(undefined)).toBe(false);
      expect(isArrayEntryReference({})).toBe(false);
      expect(isArrayEntryReference('')).toBe(false);
      expect(
        isArrayEntryReference([
          {
            type: EntityType.Reference,
            referenceType: EntityType.Entry,
            id: '1',
          },
          {},
        ])
      ).toBe(false);
    });
  });

  describe('isValidValue', () => {
    it('should return true', () => {
      expect(isValidValue('', {})).toBe(true);
      expect(isValidValue('foo', {})).toBe(true);
      expect(
        isValidValue(
          {
            type: EntityType.Reference,
            referenceType: EntityType.Entry,
            id: '1',
          },
          { allowedTypes: ['ContentType'] }
        )
      ).toBe(true);
      expect(isValidValue([], { allowedValuesMax: 2 })).toBe(true);
      expect(isValidValue(['foo'], { allowedValuesMax: 2 })).toBe(true);
      expect(
        isValidValue(
          [
            {
              type: EntityType.Reference,
              referenceType: EntityType.Entry,
              id: '1',
            },
          ],
          { allowedTypes: ['ContentType'], allowedValuesMax: 2 }
        )
      ).toBe(true);
    });

    it('should return false', () => {
      expect(isValidValue(true, {})).toBe(false);
      expect(isValidValue(1, {})).toBe(false);
      expect(isValidValue([undefined], { allowedValuesMax: 2 })).toBe(false);
      expect(
        isValidValue(
          {
            type: EntityType.Reference,
            referenceType: EntityType.Entry,
            id: '1',
          },
          {}
        )
      ).toBe(false);
      expect(
        isValidValue(
          {
            type: EntityType.Reference,
            referenceType: EntityType.Entry,
            id: '1',
          },
          { allowedValuesMax: 2 }
        )
      ).toBe(false);
    });
  });
});
