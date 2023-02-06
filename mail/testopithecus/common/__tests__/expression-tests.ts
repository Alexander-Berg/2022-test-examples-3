import * as assert from 'assert';
import { AvailableType } from '../utils/expression';
import { ExpressionBuilder } from '../utils/expression-builder';

describe('Expression unit tests', () => {
  it('should check not equals number', (done) => {
    const expression = ExpressionBuilder.build('2 != 1');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>()), true);
    done()
  });
  it('should check equals number', (done) => {
    const expression = ExpressionBuilder.build('13 == 13');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>()), true);
    done()
  });
  it('shouldn\'t check equals number', (done) => {
    const expression = ExpressionBuilder.build('234 == 234.3');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>()), false);
    done()
  });
  it('shouldn\'t check not equals number', (done) => {
    const expression = ExpressionBuilder.build('3.14159 != 3.14159');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>()), false);
    done()
  });

  it('should check not equals string', (done) => {
    const expression = ExpressionBuilder.build('\'yes\' != \'no\'');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>()), true);
    done()
  });
  it('should check equals string', (done) => {
    const expression = ExpressionBuilder.build('\'yes\' == "yes"');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>()), true);
    done()
  });

  it('should check equals variables', (done) => {
    const expression = ExpressionBuilder.build('a == b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 239]])), true);
    done()
  });
  it('should check not equals variables', (done) => {
    const expression = ExpressionBuilder.build('abacaba == bakabaka');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['abacaba', 239], ['bakabaka', 231]])), false);
    done()
  });

  it('should check and', (done) => {
    const expression = ExpressionBuilder.build('2 == 2 && a != b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 231]])), true);
    done()
  });

  it('should check or', (done) => {
    const expression = ExpressionBuilder.build('2 == 22 || a != b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 231]])), true);
    done()
  });

  it('should check || and &&', (done) => {
    const expression = ExpressionBuilder.build('2 == 2 || a != b && b == c');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 231], ['c', 0]])), true);
    done()
  });

  it('should check || and &&', (done) => {
    const expression = ExpressionBuilder.build('(2 == 2 || a != b) && b == c');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 231], ['c', 0]])), false);
    done()
  });

  it('should check not', (done) => {
    const expression = ExpressionBuilder.build('!(2 == 2) || a != b && b == c');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 231], ['c', 0]])), false);
    done()
  });
  it('should check double not', (done) => {
    const expression = ExpressionBuilder.build('5 != 5 || !(!(2 == 2))');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>()), true);
    done()
  });

  it('shouldn\'t check different types', (done) => {
    const expression = ExpressionBuilder.build('a == \'b\'');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 239]])), undefined);
    done()
  });
  it('shouldn\'t check absent variable', (done) => {
    const expression = ExpressionBuilder.build('a == \'b\'');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['b', 239]])), undefined);
    done()
  });

  it('should check < comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a < b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 238], ['b', 239]])), true);
    done()
  });
  it('should check <= comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a <= b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 238], ['b', 239]])), true);
    done()
  });
  it('should check > comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a > b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 238], ['b', 239]])), false);
    done()
  });
  it('should check >= comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a >= b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 238], ['b', 239]])), false);
    done()
  });
  it('should check comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a < b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 238], ['b', 237]])), false);
    done()
  });
  it('should check comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a <= b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 238], ['b', 237]])), false);
    done()
  });
  it('should check comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a > b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 238], ['b', 237]])), true);
    done()
  });
  it('should check comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a >= b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 238], ['b', 237]])), true);
    done()
  });
  it('should check comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a < b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 239]])), false);
    done()
  });
  it('should check comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a <= b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 239]])), true);
    done()
  });
  it('should check comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a > b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 239]])), false);
    done()
  });
  it('should check comparing variables', (done) => {
    const expression = ExpressionBuilder.build('a >= b');
    assert.strictEqual(expression.execute(new Map<string, AvailableType>([['a', 239], ['b', 239]])), true);
    done()
  });
});
