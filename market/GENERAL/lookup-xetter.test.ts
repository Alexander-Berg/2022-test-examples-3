import {lookupGetter, lookupSetter} from './lookup-xetter';

test('lookup getter - top level', function () {
    const obj = {get value() { return 123; }};
    const getter = lookupGetter(obj, 'value');
    expect(getter()).toBe(123);
});

test('lookup getter - second level', function () {
    const obj = {get value() { return 123; }};
    const child = Object.create(obj);
    const getter = lookupGetter(child, 'value');
    expect(getter()).toBe(123);
});

test('lookup getter - miss', function () {
    const obj = {};
    const getter = lookupGetter(obj, 'value');
    expect(getter).toBe(null);
});

test('lookup setter - top level', function () {
    const obj: any = {set value(a) { this.result = a; }};
    const setter = lookupSetter(obj, 'value');
    setter.call(obj, 123);
    expect(obj.result).toBe(123);
});

test('lookup setter - second level', function () {
    const obj: any = {set value(a) { this.result = a; }};
    const child = Object.create(obj);
    const setter = lookupSetter(child, 'value');
    setter.call(child, 123);
    expect(child.result).toBe(123);
});

test('lookup setter - miss', function () {
    const obj = {};
    const setter = lookupSetter(obj, 'value');
    expect(setter).toBe(null);
});
