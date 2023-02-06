'use strict';

const PerSecondAccumulator = require('./per-second-accumulator.js');

class Value {
    constructor(v1 = 0, v2 = 0) {
        this.v1 = v1;
        this.v2 = v2;
    }

    add(value) {
        if (value.constructror !== this.constructror) {
            throw new Error('value types mismatch');
        }
        this.v1 += value.v1;
        this.v2 += value.v2;
        return this;
    }
}

test('throws without size', () => {
    expect(() => {
        new PerSecondAccumulator();
    }).toThrow();
});

test('throws with size=1', () => {
    expect(() => {
        new PerSecondAccumulator(1);
    }).toThrow();
});

test('throws without value ctor', () => {
    expect(() => {
        new PerSecondAccumulator(5);
    }).toThrow();
});

test('#add returns value', () => {
    const accumulator = new PerSecondAccumulator(5, Value);
    const value = new Value(3, 1);

    const result = accumulator.add(value);

    expect(result).toEqual(value);
});

test('#add adds values', () => {
    const accumulator = new PerSecondAccumulator(5, Value);
    accumulator.add(new Value(3, 1), 10000);
    accumulator.add(new Value(4, 2), 13000);

    const result = accumulator.values;

    expect(result).toMatchSnapshot();
});

test('#getSum just works', () => {
    const accumulator = new PerSecondAccumulator(5, Value);
    const value1 = new Value(3, 1);
    const value2 = new Value(7, 9);
    const value3 = new Value(4, 2);
    const at = Date.now() - 2000;
    accumulator.add(value1, at);
    accumulator.add(value2, at);
    accumulator.add(value3);

    const result = accumulator.getSum();

    expect(result).toEqual(new Value(10, 10));
});
