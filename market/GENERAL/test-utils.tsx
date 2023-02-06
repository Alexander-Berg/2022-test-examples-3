// Правило требует, чтобы enzyme присутствовал в dependencies, а утилиты нужны только для тестов
// eslint-disable-next-line import/no-extraneous-dependencies
import { ReactWrapper } from 'enzyme';
import React from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { Subject } from 'rxjs';
import { StateObservable } from 'redux-observable';
import { TestScheduler } from 'rxjs/testing';
import * as R from 'ramda';

import { rootReducer } from '@/store/root.reducer';
import { RootState } from '@/store';

export const TestApp = ({ children }) => <DndProvider backend={HTML5Backend}>{children}</DndProvider>;

export function findFirstIntrinsicDOMElement(reactWrapper: ReactWrapper) {
  return reactWrapper.findWhere(n => n.exists() && typeof n.type() === 'string').first();
}

export const byKey = (key: string) => (node: ReactWrapper) => node.key() === key;

export const getMockedState = () => rootReducer({} as any as RootState, { type: '' });

export function getTestScheduler(): { testScheduler: TestScheduler; hot: TestScheduler['createHotObservable'] } {
  const testScheduler = new TestScheduler((expected, actual) => {
    expect(expected).toEqual(actual);
  });
  const hot = testScheduler.createHotObservable.bind(testScheduler);
  return { testScheduler, hot };
}

export const createMockStore = (state: Partial<RootState> | ((_: RootState) => RootState) = {}) => {
  const subject = new Subject<RootState>();

  if (typeof state === 'function') {
    return new StateObservable<RootState>(subject, state(getMockedState()));
  }

  return new StateObservable<RootState>(subject, R.mergeDeepRight(getMockedState(), state) as any);
};
