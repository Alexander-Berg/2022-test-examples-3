import * as R from 'ramda';

import {
  collectInvalidEntries,
  composeEntryTitle,
  createContentType,
  createContentTypeField,
  createEntryType,
  getContentTypeProperties,
  getEntriesWithDefaultValue,
  getEntryIds,
  getEntryInfo,
  getEntryPath,
  getSchemaPath,
  removeEntryReferenceFromParent,
  stripHTML,
} from './utils';
import { createReferenceType, EntityType } from '../common';
import { createPropertiesResolver } from '../properties-resolver';
import { InvalidEntryReason } from './types';

describe('models/entries/utils', () => {
  describe('getEntryPath', () => {
    it('should return path for root entry', () => {
      const entry = createEntryType('Some');
      const entries = R.indexBy(R.prop('id'), [entry, createEntryType('Some')]);

      expect(getEntryPath(entry.id, entries)).toEqual([entry.id]);
    });

    it('should return path for child entry', () => {
      const entry = createEntryType('Some');
      const child = createEntryType('Some', entry.id);
      const entries = R.indexBy(R.prop('id'), [entry, child]);

      expect(getEntryPath(child.id, entries)).toEqual([entry.id, child.id]);
    });
  });

  describe('removeEntryReferenceFromParent', () => {
    it('should removed entry reference value from single field', () => {
      const parent = createEntryType('Some');
      parent.fields.some = createReferenceType('1', EntityType.Entry);

      expect(removeEntryReferenceFromParent(parent, 'some', '1')).not.toHaveProperty(['fields', 'some']);
    });

    it('should removed entry reference value from array field', () => {
      const parent = createEntryType('Some');
      const ref1 = createReferenceType('1', EntityType.Entry);
      const ref2 = createReferenceType('2', EntityType.Entry);
      parent.fields.some = [ref1, ref2];
      const result = removeEntryReferenceFromParent(parent, 'some', ref2.id);

      expect(result.fields.some).toContain(ref1);
      expect(result.fields.some).not.toContain(ref2);
    });
  });

  describe('getEntryIds', () => {
    it('should return current id', () => {
      const entry = createEntryType('Some');
      const entries = R.indexBy(R.prop('id'), [entry]);

      const ids = getEntryIds(entry.id, entries);

      expect(ids).toHaveLength(1);
      expect(ids).toContain(entry.id);
    });

    it('should return with children ids', () => {
      const root = createEntryType('Some');
      const entry = createEntryType('Some');
      const child1 = createEntryType('Some');
      const child2 = createEntryType('Some');
      const entries = R.indexBy(R.prop('id'), [root, entry, child1, child2]);
      root.fields.some = createReferenceType(entry.id, EntityType.Entry);
      entry.fields.some = createReferenceType(child1.id, EntityType.Entry);
      child1.fields.some = [createReferenceType(child2.id, EntityType.Entry)];

      const ids = getEntryIds(entry.id, entries);

      expect(ids).toHaveLength(3);
      expect(ids).toContain(entry.id);
      expect(ids).toContain(child1.id);
      expect(ids).toContain(child2.id);
    });
  });

  describe('getSchemaPath', () => {
    it('should return correct schema path', () => {
      const root = createEntryType('Some');
      const entry = createEntryType('Some', root.id);
      const child1 = createEntryType('Some', entry.id);
      const child2 = createEntryType('Some', child1.id);
      const entries = R.indexBy(R.prop('id'), [root, entry, child1, child2]);
      root.fields.field1 = createReferenceType(entry.id, EntityType.Entry);
      entry.fields.field2 = createReferenceType(child1.id, EntityType.Entry);
      child1.fields.field3 = [createReferenceType(child2.id, EntityType.Entry)];

      const entryPath = [root.id, entry.id, child1.id, child2.id];
      const schemaPath = getSchemaPath(entryPath, entries);

      expect(schemaPath).toEqual(['Some', 'field1', 'Some', 'field2', 'Some', 'field3', 'Some']);
    });
  });

  describe('getContentTypeProperties', () => {
    it('should resolve properties', () => {
      const rootContentType = createContentType('Root', {
        selectors: [
          { selector: 'field2[Some]', properties: { label: 'Widget' } },
          { selector: 'field2[Some]/field2', properties: { defaultValue: 'Some' } },
        ],
        fields: [createContentTypeField('field1')],
      });
      const contentType = createContentType('Some', {
        selectors: [{ selector: 'field2[Some]/field2', properties: { hidden: true } }],
        fields: [createContentTypeField('field2', { hidden: false })],
      });
      const contentTypes = R.indexBy(R.prop('id'), [rootContentType, contentType]);
      const resolveProperties = createPropertiesResolver(contentTypes);

      const { properties, fieldProperties } = getContentTypeProperties(
        contentType,
        ['Root', 'field1', 'Some', 'field2', 'Some'],
        resolveProperties
      );

      expect(properties).toEqual({
        label: 'Widget',
      });
      expect(fieldProperties).toEqual({
        field2: {
          defaultValue: 'Some',
          hidden: true,
        },
      });
    });
  });

  describe('stripHTML', () => {
    it('should strip HTML tags', () => {
      expect(stripHTML('<div>text</div>')).toBe('text');
    });
  });

  describe('getEntryInfo', () => {
    it('should exists properties', () => {
      const contentType = createContentType('Some');
      const entry = createEntryType(contentType.id);
      const contentTypes = R.indexBy(R.prop('id'), [contentType]);
      const entries = R.indexBy(R.prop('id'), [entry]);
      const resolveProperties = createPropertiesResolver(contentTypes);

      const info = getEntryInfo(entry, entries, contentTypes, resolveProperties);

      expect(info).toHaveProperty('parent', null);
      expect(info).toHaveProperty('entry', entry);
      expect(info).toHaveProperty('entryPath');
      expect(info).toHaveProperty('schemaPath');
      expect(info).toHaveProperty('properties');
      expect(info).toHaveProperty('fieldProperties');
      expect(info).toHaveProperty('pluginConfigs');
      expect(info).toHaveProperty('widgetTitle');
    });
  });

  describe('getEntriesWithDefaultValue', () => {
    it('should fill default value', () => {
      const rootContentType = createContentType('Root', {
        fields: [createContentTypeField('field1', { defaultValue: 'Some', allowedTypes: ['Some'] })],
      });
      const contentType = createContentType('Some');
      const entry = createEntryType(rootContentType.id);
      const contentTypes = R.indexBy(R.prop('id'), [rootContentType, contentType]);
      const entries = R.indexBy(R.prop('id'), [entry]);
      const resolveProperties = createPropertiesResolver(contentTypes);

      const newEntries = getEntriesWithDefaultValue(entry, entries, contentTypes, resolveProperties);

      expect(newEntries).toHaveProperty([entry.id, 'fields', 'field1', 'type'], EntityType.Reference);
      expect(newEntries).toHaveProperty([entry.id, 'fields', 'field1', 'referenceType'], EntityType.Entry);

      // @ts-expect-error
      const refId = newEntries[entry.id].fields.field1.id;
      expect(newEntries).toHaveProperty([refId, 'sys', 'contentType', 'id'], 'Some');
    });

    it('should fill multiple default values', () => {
      const rootContentType = createContentType('Root', {
        fields: [
          createContentTypeField('field1', {
            allowedValuesMax: 2,
            defaultValue: ['Some', 'Thing'],
            allowedTypes: ['Some', 'Thing'],
          }),
        ],
      });
      const contentType = createContentType('Some');
      const contentType2 = createContentType('Thing');
      const entry = createEntryType(rootContentType.id);
      const contentTypes = R.indexBy(R.prop('id'), [rootContentType, contentType, contentType2]);
      const entries = R.indexBy(R.prop('id'), [entry]);
      const resolveProperties = createPropertiesResolver(contentTypes);

      const newEntries = getEntriesWithDefaultValue(entry, entries, contentTypes, resolveProperties);

      expect(newEntries).toHaveProperty([entry.id, 'fields', 'field1', 0, 'type'], EntityType.Reference);
      expect(newEntries).toHaveProperty([entry.id, 'fields', 'field1', 0, 'referenceType'], EntityType.Entry);

      // @ts-expect-error
      let refId = newEntries[entry.id].fields.field1[0].id;
      expect(newEntries).toHaveProperty([refId, 'sys', 'contentType', 'id'], 'Some');
      // @ts-expect-error
      refId = newEntries[entry.id].fields.field1[1].id;
      expect(newEntries).toHaveProperty([refId, 'sys', 'contentType', 'id'], 'Thing');
    });
  });

  describe('composeEntryTitle', () => {
    it('should resolve placeholder value', () => {
      const contentType = createContentType('Some', {
        properties: { widgetTitle: ['field1'] },
        fields: [createContentTypeField('field1')],
      });
      const entry = createEntryType(contentType.id);
      entry.fields.field1 = 'Foo';
      const contentTypes = R.indexBy(R.prop('id'), [contentType]);
      const entries = R.indexBy(R.prop('id'), [entry]);

      const title = composeEntryTitle(entry, entries, contentTypes);

      expect(title).toBe('Foo');
    });

    it('should resolve node with array value correct', () => {
      const childContentType = createContentType('CHILD', {
        properties: { widgetTitle: [' * ', 'flag'] },
        fields: [createContentTypeField('flag')],
      });
      const parentContentType = createContentType('PARENT', {
        properties: { widgetTitle: ['Parent block', 'exp'] },
        fields: [
          createContentTypeField('exp', {
            allowedTypes: [childContentType.id],
            allowedValuesMax: 100,
            allowedValuesMin: 0,
          }),
        ],
      });

      const childEntry1 = createEntryType(childContentType.id, parentContentType.id);
      childEntry1.fields.flag = 'FLAG_NUMBER_ONE';
      const childEntry2 = createEntryType(childContentType.id, parentContentType.id);
      childEntry2.fields.flag = 'FLAG_NUMBER_TWO';

      const parentEntry = createEntryType(parentContentType.id);
      parentEntry.fields.exp = [
        createReferenceType(childEntry1.id, EntityType.Entry),
        createReferenceType(childEntry2.id, EntityType.Entry),
      ];

      const contentTypes = R.indexBy(R.prop('id'), [childContentType, parentContentType]);
      const entries = R.indexBy(R.prop('id'), [parentEntry, childEntry1, childEntry2]);
      const title = composeEntryTitle(parentEntry, entries, contentTypes);

      expect(title).toBe('Parent block * FLAG_NUMBER_ONE * FLAG_NUMBER_TWO');
    });
  });

  describe('collectInvalidEntries', () => {
    it('should return invalid entries', () => {
      const contentType = createContentType('Some', {
        fields: [createContentTypeField('field1')],
      });
      const entry = createEntryType(contentType.id);
      const invalid1 = createEntryType('INVALID1', entry.id);
      const invalid2 = createEntryType('INVALID2');
      entry.fields.field1 = createReferenceType(invalid1.id, EntityType.Entry);

      const contentTypes = R.indexBy(R.prop('id'), [contentType]);
      const entries = R.indexBy(R.prop('id'), [entry, invalid1, invalid2]);

      const result = collectInvalidEntries(entries, contentTypes);

      expect(result).toHaveLength(2);
      expect(result).toEqual(
        expect.arrayContaining([
          {
            entryId: invalid1.id,
            contentTypeId: 'INVALID1',
            parent: { id: entry.id, fieldName: 'field1' },
            reason: InvalidEntryReason.MISSING_CONTENT_TYPE,
          },
          {
            entryId: invalid2.id,
            contentTypeId: 'INVALID2',
            parent: null,
            reason: InvalidEntryReason.MISSING_CONTENT_TYPE,
          },
        ])
      );
    });
  });
});
