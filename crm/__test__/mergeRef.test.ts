import { createRef } from 'react';
import { MergeRef } from '../mergeRef';

describe('mergerRef', () => {
  it('should be null by default', () => {
    const ref = new MergeRef();

    expect(ref.current).toBe(null);
  });

  it('should store correct ref', () => {
    const ref = new MergeRef();

    const obj = {};
    ref.current = obj;

    expect(ref.current).toBe(obj);
  });

  it('should support ref for refs', () => {
    const ref = new MergeRef();
    const reactRef = createRef();

    const obj = {};
    ref.refs = [reactRef];
    ref.current = obj;

    expect(reactRef.current).toBe(obj);
  });

  it('should support function for refs', () => {
    const ref = new MergeRef();

    const obj = {};

    let node;
    const funcRef = (n) => {
      node = n;
    };

    ref.refs = [funcRef];
    ref.current = obj;

    expect(node).toBe(obj);
  });

  it('should support mergeRef for refs', () => {
    const ref1 = new MergeRef();
    const ref2 = new MergeRef();

    const obj = {};
    ref1.refs = [ref2];
    ref1.current = obj;

    expect(ref2.current).toBe(obj);
  });

  it('should pass as function', () => {
    const ref1 = new MergeRef();
    const ref2 = new MergeRef();

    const obj = {};
    ref1.refs = [ref2.asCallback];
    ref1.current = obj;

    expect(ref2.current).toBe(obj);
  });

  it('should correct with undefined ref', () => {
    const ref = new MergeRef();

    const obj = {};
    ref.refs = [undefined];
    ref.current = obj;

    expect(ref.current).toBe(obj);
  });
});
