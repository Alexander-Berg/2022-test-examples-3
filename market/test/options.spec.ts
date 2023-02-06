import * as assert from 'assert';
import { deepClone } from 'fast-json-patch';
import * as fs from 'fs';
import * as path from 'path';
import {
  validateSchemaCompatibility,
  validateSchemaFiles,
} from '../src/validator';

describe('Options', () => {
  describe('allowNewOneOf', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data_options_allowOneOf.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, { encoding: 'utf-8' }));

    const newSchema = deepClone(originalSchema);
    newSchema.definitions.root.items.anyOf.push({ type: 'number' });
    newSchema.definitions.anotherItem.content.items[0].anyOf.push({ fruit: 'pear' });
    newSchema.definitions.inline_node.anyOf.push(`{ "$ref": "#/definitions/hardBreak_node" }`);

    context('allowNewOneOf is not set', () => {
      it('should return diff if there are new oneOf elements', () => {
        const {length} = validateSchemaCompatibility(originalSchema, newSchema);

        assert.strictEqual(length, 3);
      });
    });

    context('allowNewOneOf = True', () => {
      it('should returns empty diff if there are new oneOf elements', () => {
        const {length} = validateSchemaCompatibility(originalSchema, newSchema, { allowNewOneOf: true });

        assert.strictEqual(length, 0);
      });
    });

    context('allowNewOneOf = False', () => {
      it('should returns diff if there are new oneOf elements', () => {
        const {length} = validateSchemaCompatibility(originalSchema, newSchema, { allowNewOneOf: false });

        assert.strictEqual(length, 3);
      });
    });
  });

  describe('allowNewEnumValue', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data_options_allowNewEnumValue.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, { encoding: 'utf-8' }));

    const newSchema = deepClone(originalSchema);
    newSchema.definitions.root.properties.fruit.type.enum.push('banana');
    newSchema.definitions.anotherItem.properties.tshirt.size.enum.push('large');

    context('allowNewEnumValue is not set', () => {
      it('should returns diff if there are new enum values', () => {
        const {length} = validateSchemaCompatibility(originalSchema, newSchema);

        assert.strictEqual(length, 2);
      });
    });

    context('allowNewEnumValue = True', () => {
      it('should returns empty diff if there are new enum values', () => {
        const {length} = validateSchemaCompatibility(originalSchema, newSchema, { allowNewEnumValue: true });

        assert.strictEqual(length, 0);
      });
    });

    context('allowNewEnumValue = False', () => {
      it('should returns diff if there are new enum values', () => {
        const {length} = validateSchemaCompatibility(originalSchema, newSchema, { allowNewEnumValue: false });

        assert.strictEqual(length, 2);
      });
    });
  });

  describe('allowReorder', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data_options_allowReorder.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, { encoding: 'utf-8' }));

    const newSchema = deepClone(originalSchema);
    newSchema.definitions.status_node = {
      type: 'object',
    };
    const [first, ...others] = newSchema.definitions.inline_node.anyOf;
    newSchema.definitions.inline_node.anyOf = [
      first,
      { $ref: '#/definitions/status_node' },
      ...others,
    ];

    context('allowReorder is not', () => {
      it('should returns diff if items are reordered', () => {
        const {length} = validateSchemaCompatibility(originalSchema, newSchema);

        assert.strictEqual(length, 3);
      });
    });

    context('allowReorder = True', () => {
      it('should returns empty diff if items are reordered', () => {
        const {length} = validateSchemaCompatibility(originalSchema, newSchema, { allowReorder: true });

        assert.strictEqual(length, 0);
      });

      it('should returns diff if items are removed as a result of reordering', () => {
        const invalidSchema = deepClone(newSchema);
        invalidSchema.definitions.inline_node.anyOf = [
          { $ref: '#/definitions/status_node' },
          ...others,
        ];

        const {length} = validateSchemaCompatibility(originalSchema, invalidSchema, { allowReorder: true });

        assert.strictEqual(length, 1);
      });
    });

    context('allowReorder = False', () => {
      it('should returns diff if items are reordered', () => {
        const {length} = validateSchemaCompatibility(originalSchema, newSchema, { allowReorder: false });

        assert.strictEqual(length, 3);
      });
    });

  });

  describe('minItems', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data_minItems.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, { encoding: 'utf-8' }));
    let newSchema: any;

    beforeEach(() => {
      newSchema = deepClone(originalSchema);
    });

    it('should returns empty diff if minItems is removed', () => {
      delete newSchema.definitions.doc.properties.content.minItems;

      const {length} = validateSchemaCompatibility(originalSchema, newSchema);

      assert.strictEqual(length, 0);
    });

    it('should returns empty diff if minItems is replaced with smaller value', () => {
      newSchema.definitions.doc.properties.content.minItems = 0;

      const {length} = validateSchemaCompatibility(originalSchema, newSchema);

      assert.strictEqual(length, 0);
    });

    it('should returns diff if minItems is replaced with greater value', () => {
      newSchema.definitions.doc.properties.content.minItems = 10;

      const {length} = validateSchemaCompatibility(originalSchema, newSchema);

      assert.strictEqual(length, 1);
    });
  });
});
