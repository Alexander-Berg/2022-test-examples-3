/* eslint-disable max-classes-per-file */
import {Context} from './context';
import {BaseModel, Model} from './model';

test('factory', function () {
    const ctx = new Context(null);

    class SomeModel extends BaseModel {}

    class OtherModel extends BaseModel {}

    const original = SomeModel.factory(ctx);

    const clone = SomeModel.factory(ctx);
    const otherContext = SomeModel.factory(new Context(null));
    const otherModel = OtherModel.factory(ctx);

    expect(original).toBe(clone);
    expect(original).not.toBe(otherContext);
    expect(original).not.toBe(otherModel);
});

test('connect', function () {
    class SomeModel extends BaseModel {}

    class OtherModel extends BaseModel {}

    const Connected = SomeModel.connect({other: OtherModel});

    expect(Connected.name).toBe(SomeModel.name);

    const instance = Connected.factory(new Context(null));

    expect(instance).toBeInstanceOf(SomeModel);
    expect(instance.other).toBeInstanceOf(OtherModel);
    expect(instance.other).toBe(instance.other);
    expect(instance.context).toBe(instance.other.context);
});

test('BaseModel has no memoisation', function () {
    const fn = jest.fn();

    class SomeModel extends BaseModel {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        public method(a) { fn(); }
    }

    const instance = SomeModel.factory(new Context(null));

    instance.method(123);
    instance.method(123);

    expect(fn).toHaveBeenCalledTimes(2);
});

test('Model has memoisation', function () {
    const fn = jest.fn();

    class SomeModel extends Model {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        public method(a) { fn(); }
    }

    const instance = SomeModel.factory(new Context(null));

    instance.method(123);
    instance.method(123);

    expect(fn).toHaveBeenCalledTimes(1);
});
