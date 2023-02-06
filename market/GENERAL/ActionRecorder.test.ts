import { applyMiddleware, createStore } from 'redux';
import { reducerWithInitialState } from 'typescript-fsa-reducers';

import { ActionRecorder, createActionRecorderMiddleware } from './ActionRecorder';

describe('ActionRecorder', () => {
  test('record', () => {
    const recorder = new ActionRecorder(1);

    const action = { type: 'test', payload: 'something' };
    recorder.saveAction({ ...action, payload: 'something1' });
    recorder.saveAction(action);

    expect(recorder.firedActions).toHaveLength(1);
    expect(recorder.firedActions).toContainEqual(action);
  });

  test('createActionRecorderMiddleware', () => {
    const recorder = new ActionRecorder(1);

    const store = createStore(
      reducerWithInitialState<any>({}),
      {},
      applyMiddleware(createActionRecorderMiddleware(recorder))
    );

    const action = { type: 'test', payload: 'something' };
    store.dispatch(action);

    expect(recorder.firedActions).toHaveLength(1);
    expect(recorder.firedActions).toContainEqual(action);
  });
});
