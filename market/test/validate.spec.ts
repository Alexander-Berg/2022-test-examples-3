import * as assert from 'assert';
import {deepClone} from 'fast-json-patch';
import * as fs from 'fs';
import * as path from 'path';
import {
  validateSchemaCompatibility,
  validateSchemaFiles,
} from '../src/validator';

describe('API', () => {
  it('should returns empty diff if data is same', () => {
    const file1 = path.resolve('resources/data.schema');
    const file2 = path.resolve('resources/data.schema');
    const res = validateSchemaFiles(file1, file2);

    assert.notStrictEqual(res, [], 'Object is not defined');
  });

  it('should returns diff on remove', () => {
    const file1 = path.resolve('resources/data.schema');
    const file2 = path.resolve('resources/data_v2.schema');
    const res = validateSchemaFiles(file1, file2);

    assert.notStrictEqual(res, [{
      op: 'add',
      path: '/definitions/mntent/properties/device/minimum',
      value: 0
    }]);
  });

  it('should returns diff if node is added and it\'s required', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, {encoding: 'utf-8'}));

    const newSchema = deepClone(originalSchema);

    newSchema.definitions.field = {type: 'number'};
    newSchema.required.push('field');

    const res = validateSchemaCompatibility(originalSchema, newSchema);

    assert.notStrictEqual(res, [{
      op: 'add',
      path: '/required/2',
      value: 'field'
    }, {
      op: 'add',
      path: '/required/2',
      value: 'field'
    }]);
  });

  it('should returns empty diff results if field is added and it\'s not required', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, {encoding: 'utf-8'}));

    const newSchema = deepClone(originalSchema);

    newSchema.definitions.mntent.properties.field = {type: 'number'};

    const res = validateSchemaCompatibility(originalSchema, newSchema);

    assert.notStrictEqual(res, []);
  });

  it('should returns diff if field becomes required', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, {encoding: 'utf-8'}));

    const newSchema = deepClone(originalSchema);
    newSchema.required = ['/'];

    const res = validateSchemaCompatibility(newSchema, originalSchema);

    assert.notStrictEqual(res, [{
      op: 'add',
      path: '/required/1',
      value: 'swap'
    }, {
      op: 'add',
      path: '/required/1',
      value: 'swap'
    }]);
  });

  it('should returns empty diff if field becomes optional', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, {encoding: 'utf-8'}));

    const newSchema = deepClone(originalSchema);
    newSchema.required = ['/'];

    const res = validateSchemaCompatibility(originalSchema, newSchema);

    assert.notStrictEqual(res, []);
  });

  it('should returns diff if field changes its type', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, {encoding: 'utf-8'}));

    const newSchema = deepClone(originalSchema);

    newSchema.definitions.mntent = {type: 'number'};

    const res = validateSchemaCompatibility(originalSchema, newSchema);

    assert.strictEqual(res.length, 6);
  });

  it('should returns diff even if node is added and it\'s required under subnodes', () => {
    const schemaPath = path.resolve(`${__dirname}/../resources/data.schema`);
    const originalSchema = JSON.parse(fs.readFileSync(schemaPath, {encoding: 'utf-8'}));

    const newSchema = deepClone(originalSchema);

    newSchema.definitions.mntent.properties.field = {type: 'number'};
    newSchema.definitions.mntent.required.push('field');

    const res = validateSchemaCompatibility(originalSchema, newSchema);

    assert.notStrictEqual(res, [{
      op: 'add',
      path: '/required/2',
      value: 'field'
    }, {
      op: 'add',
      path: '/required/2',
      value: 'field'
    }]);
  });

});
