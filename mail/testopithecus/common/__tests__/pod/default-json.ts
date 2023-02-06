import { JSONSerializer } from '../../code/client/json/json-serializer';
import {
  ArrayJSONItem,
  BooleanJSONItem,
  DoubleJSONItem,
  IntegerJSONItem,
  JSONItem, JSONItemKind, MapJSONItem,
  NullJSONItem,
  StringJSONItem,
} from '../../code/mail/logging/json-types'
import { Result } from '../../code/client/result';
import { RuntimeClassInfo } from '../../code/utils/runtime-info';
import { Decodable, Encodable} from '../../ys/ys';
import * as assert from 'assert';

export class DefaultJSONSerializer implements JSONSerializer {

  public deserialize<T>(item: string, materializer: (json: JSONItem) => Result<T>): Result<T> {
    const jsonObj = JSON.parse(item);
    const value = this.parse(jsonObj);
    return materializer(value)
  }

  public deserializeDecodable<T extends Decodable>(runtimeClassInfo: RuntimeClassInfo, item: string): Result<T> {
    return new Result<T>(null, Error('Not implemented'));
  }

  public serialize(item: JSONItem): Result<string> {
    switch (item.kind) {
      case JSONItemKind.nullItem:
        return new Result('null', null);
      case JSONItemKind.integer:
        return new Result((item as IntegerJSONItem).asInt64().toString(), null);
      case JSONItemKind.double:
        return new Result((item as DoubleJSONItem).value.toString(), null);
      case JSONItemKind.boolean:
        return new Result((item as BooleanJSONItem).value.toString(), null);
      case JSONItemKind.string:
        return new Result('\"' + (item as StringJSONItem).value + '\"', null);
      case JSONItemKind.array:
        return this.serializeArray(item as ArrayJSONItem);
      case JSONItemKind.map:
        return this.serializeMap(item as MapJSONItem);
    }
    return new Result<string>(null, null);
  }

  public serializeEncodable<T extends Encodable>(item: T): Result<string> {
    return new Result<string>(null, Error('Not implemented'));
  }

  public parse(element: any): JSONItem {
    switch (typeof (element)) {
      case 'object':
        if (element instanceof Array) {
          return this.parseArray(element as any[]);
        } else if (element === null) {
          return new NullJSONItem();
        } else {
          return this.parseObject(element);
        }
      case 'string':
        return new StringJSONItem(element);
      case 'number':
        if (Number.isInteger(element)) {
          return IntegerJSONItem.fromInt32(element);
        } else {
          return new DoubleJSONItem(element);
        }
      case 'boolean':
        return new BooleanJSONItem(element);
      case 'undefined':
        return new NullJSONItem();
      case 'bigint':
        return IntegerJSONItem.fromInt64(element)
      default:
        return new NullJSONItem();
    }
  }

  private parseArray(arr: any[]): JSONItem {
    const jsonArray = new ArrayJSONItem();
    arr.forEach((element) => jsonArray.add(this.parse(element)));
    return jsonArray;
  }

  private parseObject(obj: object): JSONItem {
    const jsonObject = new MapJSONItem();
    Object.entries(obj).forEach(([key, value]) => jsonObject.put(key, this.parse(value)));
    return jsonObject;
  }

  private serializeArray(arrayItem: ArrayJSONItem): Result<string> {
    const strings = [];
    for (const item of Object.keys(arrayItem.asArray())) {
      const parsed = this.serialize(arrayItem.get(Number.parseInt(item, 10)));
      if (parsed.isError()) {
        return parsed;
      } else {
        strings.push(parsed.getValue())
      }
    }
    return new Result('[' + strings.join(', ') + ']', null);
  }

  private serializeMap(mapItem: MapJSONItem) {
    const strings = [];
    for (const key of mapItem.asMap().keys()) {
      const parsed = this.serialize(mapItem.get(key)!);
      if (parsed.isError()) {
        return parsed;
      } else {
        strings.push('\"' + key + '\"' + ': ' + parsed.getValue())
      }
    }
    return new Result('{' + strings.join(', ') + '}', null);
  }
}

describe('Serialize and deserialize should work correctly', () => {
  it('on empty map', (done) => {
    const expected = new MapJSONItem(new Map<string, JSONItem>());
    const actual = testString('{}');
    assert.deepStrictEqual(actual, expected);
    done()
  });

  it('on empty array', (done) => {
    const expected = new ArrayJSONItem([]);
    const actual = testString('[]');
    assert.deepStrictEqual(actual, expected);
    done()
  });

  it('on string type', (done) => {
    const expected = new StringJSONItem('hello');
    const actual = testString('"hello"');
    assert.deepStrictEqual(actual, expected);
    done()
  });

  it('on integer type', (done) => {
    const expected = IntegerJSONItem.fromInt32(451);
    const actual = testString('451');
    assert.deepStrictEqual(actual, expected);
    done()
  });

  it('on double type', (done) => {
    const expected = new DoubleJSONItem(-18.41);
    const actual = testString('-18.41');
    assert.deepStrictEqual(actual, expected);
    done()
  });

  it('on null type', (done) => {
    const expected = new NullJSONItem();
    const actual = testString('null');
    assert.deepStrictEqual(actual, expected);
    done()
  });

  it('on simple array', (done) => {
    const expected = new ArrayJSONItem()
      .add(IntegerJSONItem.fromInt32(1))
      .add(new StringJSONItem('here'))
      .add(new NullJSONItem());
    const actual = testString('[ 1, \"here\", null ]');
    assert.deepStrictEqual(actual, expected);
    done()
  });

  it('on simple map', (done) => {
    const expected = new MapJSONItem()
      .put('hello', IntegerJSONItem.fromInt32(1))
      .put('world', new StringJSONItem('here'));
    const actual = testString('{ \"hello\" : 1, \"world\" : \"here\" }');
    assert.deepStrictEqual(actual, expected);
    done()
  });

  it('on multi level', (done) => {
    const stringToTest = '{ "array" : [1, 2], "map" : { "key" : "value" }, "emptyMap" : {}, "number" : 1 }';
    const expected = new MapJSONItem()
      .put('array', new ArrayJSONItem()
        .add(IntegerJSONItem.fromInt32(1))
        .add(IntegerJSONItem.fromInt32(2)),
      )
      .put('map', new MapJSONItem()
        .put('key', new StringJSONItem('value')),
      )
      .put('emptyMap', new MapJSONItem())
      .put('number', IntegerJSONItem.fromInt32(1));
    const actual = testString(stringToTest);
    assert.deepStrictEqual(actual, expected);
    done()
  });

  function testString(jsonString: string) {
    const serializer = new DefaultJSONSerializer();
    const jsonItem = serializer.deserialize(jsonString, (item) => new Result(item, null)).getValue();
    const jsonStringFromItem = serializer.serialize(jsonItem).getValue();
    const copyItem = serializer.deserialize(jsonStringFromItem, (item) => new Result(item, null)).getValue();
    const copyJsonStringFromItem = serializer.serialize(copyItem).getValue();
    assert.deepStrictEqual(jsonItem, copyItem);
    assert.strictEqual(jsonStringFromItem, copyJsonStringFromItem);
    return jsonItem;
  }
});
