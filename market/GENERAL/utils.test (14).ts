import * as R from 'ramda';

import { validate, ValidationRuleType } from './utils';
import {
  createEntryType,
  createContentType,
  Entry,
  EntryInfo,
  getEntryInfo,
  createContentTypeField,
  EntryFieldValue,
} from '../entries';
import { ContentType } from '../content-types';
import { createPropertiesResolver } from '../properties-resolver';
import { createReferenceType, EntityType } from '../common';

function getEntriesInfo(entryList: Entry[], contentTypeList: ContentType[]) {
  const entries = R.indexBy(R.prop('id'), entryList);
  const contentTypes = R.indexBy(R.prop('id'), contentTypeList);
  const resolve = createPropertiesResolver(contentTypes);

  const entriesInfo: Record<string, EntryInfo> = {};
  for (const entry of entryList) {
    entriesInfo[entry.id] = getEntryInfo(entry, entries, contentTypes, resolve);
  }

  return entriesInfo;
}

function createEntryTypeWithValues(
  contentTypeId: string,
  values: Record<string, EntryFieldValue> = {},
  parentId?: string
) {
  const entry = createEntryType(contentTypeId, parentId);

  return R.set(R.lensProp('fields'), values, entry);
}

describe('models/validation/utils', () => {
  describe('validate', () => {
    it('optional field with the empty string value must be valid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName)],
      });
      const entry = createEntryTypeWithValues(contentType.id, { [fieldName]: '' });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toBeUndefined();
    });

    it('required field with the undefined value must be invalid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedValuesMin: 1 })],
      });
      const entry = createEntryTypeWithValues(contentType.id);
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toHaveLength(1);
      expect(fieldResult).toContainEqual(expect.objectContaining({ type: ValidationRuleType.Required }));
    });

    it('required field with the empty string value must be invalid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedValuesMin: 1 })],
      });
      const entry = createEntryTypeWithValues(contentType.id, { [fieldName]: '' });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toHaveLength(1);
      expect(fieldResult).toContainEqual(expect.objectContaining({ type: ValidationRuleType.StringEmpty }));
    });

    it('the field value must not match the regular expression', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { pattern: '^[0-9]+$' })],
      });
      const entry = createEntryTypeWithValues(contentType.id, { [fieldName]: 'abc' });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toHaveLength(1);
      expect(fieldResult).toContainEqual(expect.objectContaining({ type: ValidationRuleType.StringPattern }));
    });

    it('the field value must match the regular expression', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { pattern: '^[0-9]+$' })],
      });
      const entry = createEntryTypeWithValues(contentType.id, { [fieldName]: '10' });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toBeUndefined();
    });

    it('the required field value must be valid if the default value is set', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedValuesMin: 1, defaultValue: 'foo' })],
      });
      const entry = createEntryTypeWithValues(contentType.id);
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toBeUndefined();
    });

    it('the reference field value must be valid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedTypes: ['Bar'] })],
      });
      const entry = createEntryTypeWithValues(contentType.id, {
        [fieldName]: createReferenceType('1', EntityType.Entry),
      });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toBeUndefined();
    });

    it('the reference field value must be invalid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedTypes: ['Bar'] })],
      });
      const entry = createEntryTypeWithValues(contentType.id, {
        // @ts-expect-error
        [fieldName]: {},
      });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toHaveLength(1);
      expect(fieldResult).toContainEqual(expect.objectContaining({ type: ValidationRuleType.EntryReference }));
    });

    it('the reference field value must be required', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedTypes: ['Bar'], allowedValuesMin: 1 })],
      });
      const entry = createEntryTypeWithValues(contentType.id);
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toHaveLength(1);
      expect(fieldResult).toContainEqual(expect.objectContaining({ type: ValidationRuleType.Required }));
    });

    it('the empty value of required reference field with default value must be valid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [
          createContentTypeField(fieldName, {
            allowedTypes: ['Bar'],
            allowedValuesMin: 1,
            defaultValue: 'Bar',
          }),
        ],
      });
      const entry = createEntryTypeWithValues(contentType.id);
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toBeUndefined();
    });

    it('the empty array field must be valid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedValuesMax: 2 })],
      });
      const entry = createEntryTypeWithValues(contentType.id, {
        [fieldName]: [],
      });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toBeUndefined();
    });

    it('the array string field with array string values must be valid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedValuesMax: 2 })],
      });
      const entry = createEntryTypeWithValues(contentType.id, {
        [fieldName]: ['foo', 'bar'],
      });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toBeUndefined();
    });

    it('the array string field with entry reference values must be invalid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedValuesMax: 2 })],
      });
      const entry = createEntryTypeWithValues(contentType.id, {
        [fieldName]: [createReferenceType('1', EntityType.Entry)],
      });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toHaveLength(1);
      expect(fieldResult).toContainEqual(expect.objectContaining({ type: ValidationRuleType.String }));
    });

    it('the array reference field with string values must be invalid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedTypes: ['Bar'], allowedValuesMax: 2 })],
      });
      const entry = createEntryTypeWithValues(contentType.id, {
        [fieldName]: ['Bar'],
      });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toHaveLength(1);
      expect(fieldResult).toContainEqual(expect.objectContaining({ type: ValidationRuleType.EntryReference }));
    });

    it('the array field with incorrect values must be invalid', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedValuesMax: 100 })],
      });
      const entry = createEntryTypeWithValues(contentType.id, {
        // @ts-expect-error
        [fieldName]: [null, false, 100, undefined],
      });
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toHaveLength(4);
      expect(fieldResult).toEqual([
        expect.objectContaining({ type: ValidationRuleType.String }),
        expect.objectContaining({ type: ValidationRuleType.String }),
        expect.objectContaining({ type: ValidationRuleType.String }),
        expect.objectContaining({ type: ValidationRuleType.String }),
      ]);
    });

    it('skip validation for hidden fields', () => {
      const fieldName = 'testField';
      const contentType = createContentType('Foo', {
        fields: [createContentTypeField(fieldName, { allowedValuesMin: 1, hidden: true })],
      });
      const entry = createEntryTypeWithValues(contentType.id);
      const entriesInfo = getEntriesInfo([entry], [contentType]);
      const result = validate(entriesInfo);
      const fieldResult = R.path([entry.id, fieldName], result);

      expect(fieldResult).toBeUndefined();
    });
  });
});
