/* eslint-disable react/jsx-no-bind */
import React from 'react';
import { act, create } from 'react-test-renderer';

type THookResult<THookCb> = THookCb extends (() => infer TResult) ? TResult : never;

function TestHook<THookCb extends Function>(
  { hookCallback, getHookResult }: {
    hookCallback: THookCb,
    getHookResult: (result: THookResult<THookCb>) => void
  }): null {
  getHookResult(hookCallback());

  return null;
}

export function renderHook<THookCb extends Function>(
  hookCb: THookCb,
): () => THookResult<THookCb> {
  let value: THookResult<THookCb>;

  const getHookResult = (result: THookResult<THookCb>): void => {
    value = result;
  };

  act(() => {
    create(
      <TestHook
        getHookResult={getHookResult}
        hookCallback={hookCb}
      />,
    );
  });

  return (): THookResult<THookCb> => value;
}
