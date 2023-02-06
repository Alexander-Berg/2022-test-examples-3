/* eslint-disable react-hooks/rules-of-hooks */
import React, { useEffect, useRef } from 'react';
import * as R from 'ramda';
import { render } from '@testing-library/react';

export const whyComponentDidUpdate = <Props, State>(
  component: React.Component<Props, State>,
  prevProps: Props,
  prevState: State
) => {
  Object.entries(component.props || {}).forEach(
    ([key, val]) =>
      // eslint-disable-next-line no-console
      (prevProps as any)[key] !== val && console.log(`whyComponentDidUpdate: Prop '${key}' changed to [${val}]`)
  );
  Object.entries(component.state || {}).forEach(
    ([key, val]) =>
      // eslint-disable-next-line no-console
      (prevState as any)[key] !== val && console.log(`whyComponentDidUpdate: State '${key}' changed to [${val}]`)
  );
};

const isObject = R.compose(R.equals('Object'), R.type);
const allAreObjects = R.compose(R.all(isObject), R.values as any);
const hasLeft = R.has('left');
const hasRight = R.has('right');
const hasBoth = R.both(hasLeft, hasRight);
const isEqual = R.both(hasBoth, R.compose(R.apply(R.equals) as any, R.values));

const markAdded = R.compose(R.append(undefined), R.values as any);
const markRemoved = R.compose(R.prepend(undefined), R.values as any);
const isAddition = R.both(hasLeft, R.complement(hasRight));
const isRemoval = R.both(R.complement(hasLeft), hasRight);

const objectDiff = R.curry(_diff);
function _diff(l: any, r: any) {
  return (R.compose(
    R.map(
      R.cond([
        [isAddition, markAdded],
        [isRemoval, markRemoved],
        [hasBoth, R.ifElse(allAreObjects, R.compose(R.apply(objectDiff as any) as any, R.values), R.values)],
      ])
    ),
    R.reject(isEqual) as any,
    R.useWith(R.mergeWith(R.merge), [R.map(R.objOf('left')), R.map(R.objOf('right'))])
  ) as (a: any, b: any) => void)(l, r);
}

// For testing purposes only
export function useWhyDidUpdate<T>(title: string, dep: T) {
  const prev = useRef<T>(dep);
  useEffect(() => {
    if (prev.current !== dep) {
      // eslint-disable-next-line no-console
      console.log(`# WHY => DidUpdate: ${title} [${JSON.stringify(objectDiff(prev.current, dep))}]`);
      prev.current = dep;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dep]);
}

export function generateDummyObject<T>(id: number, sample: Partial<T>): T {
  return Object.entries(sample).reduce((result: T, [key, value]: [string, any]) => {
    // eslint-disable-next-line no-param-reassign
    result[key as keyof T] = typeof value === 'number' ? value + id : (`${value} ${id}` as any);
    return result;
  }, {} as T);
}

export function testDataGen<T>(sample: Partial<T>): { from: (...p: number[]) => T[] } {
  return {
    from: (...ids: number[]) => ids.map(id => generateDummyObject(id, sample)),
  };
}

export const sequence = (length: number) => Array.from(Array(length).keys());

export function renderWithProvider(component: React.ReactElement, wrapper: React.ComponentType) {
  return render(component, {
    wrapper,
  });
}
