import { makeAutoObservable } from 'mobx';
import {
  createObservableFromMobxReaction,
  IReactionRxJSOptions,
} from '../createObservableFromMobxReaction';

class Atom {
  private _value = 1;

  constructor() {
    makeAutoObservable(this);
  }

  set value(value: number) {
    this._value = value;
  }

  get value() {
    return this._value;
  }
}

describe('createObservableFromMobxReaction', () => {
  describe('without error', () => {
    const createTestData = (options?: IReactionRxJSOptions) => {
      const atom = new Atom();
      const observable = createObservableFromMobxReaction(() => atom.value, options);
      const next = jest.fn();
      const subscription = observable.subscribe(next);

      return { atom, next, subscription };
    };

    it('calls next without init value by default', () => {
      const { atom, next, subscription } = createTestData();

      atom.value = 2;
      atom.value = 3;
      subscription.unsubscribe();

      expect(next).toBeCalledTimes(2);
      expect(next.mock.calls[0][0]).toBe(2);
      expect(next.mock.calls[1][0]).toBe(3);
    });

    it('calls next with init value by options', () => {
      const { atom, next, subscription } = createTestData({ fireImmediately: true });

      atom.value = 2;
      subscription.unsubscribe();

      expect(next).toBeCalledTimes(2);
      expect(next.mock.calls[0][0]).toBe(1);
      expect(next.mock.calls[1][0]).toBe(2);
    });

    it('supports unsubscribe', () => {
      const { atom, next, subscription } = createTestData();

      atom.value = 2;
      subscription.unsubscribe();
      atom.value = 3;

      expect(next).toBeCalledTimes(1);
      expect(next.mock.calls[0][0]).toBe(2);
    });
  });

  describe('with error', () => {
    it('does not call error', () => {
      const atom = new Atom();
      const error = jest.fn();

      const observable = createObservableFromMobxReaction(() => {
        if (atom.value === 2) {
          throw new Error();
        }

        return atom.value;
      });

      const subscription = observable.subscribe({ error });
      atom.value = 2;

      subscription.unsubscribe();

      expect(error).not.toBeCalled();
    });

    it('calls error', () => {
      const atom = new Atom();

      const observable = createObservableFromMobxReaction(
        () => {
          if (atom.value === 2) {
            throw new Error();
          }

          return atom.value;
        },
        { isThrowError: true },
      );

      const error = jest.fn();
      const subscription = observable.subscribe({ error });
      atom.value = 2;
      subscription.unsubscribe();

      expect(error).toBeCalled();
    });
  });
});
