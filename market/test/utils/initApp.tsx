import { createApiMock } from '@yandex-market/mbo-test-utils';
import { mount } from 'enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { AnyAction, applyMiddleware, createStore } from 'redux';
import { createEpicMiddleware } from 'redux-observable';

import { AliasMakerContext } from 'src/shared/common-logs/context/AliasMakerContext';
import { initActivityMonitor } from 'src/shared/services/ActivityMonitor';
import { AliasMaker } from 'src/shared/services/AliasMaker';
import StorageManager from 'src/shared/common-logs/services/StorageManager';
import { App } from 'src/tasks/categorial-classification/components/ClassificationApp/ClassificationApp';
import { rootEpic } from 'src/tasks/categorial-classification/helpers/rootEpic';
import setupInitialState from 'src/tasks/categorial-classification/helpers/setupInitialState';
import setupReducer from 'src/tasks/categorial-classification/helpers/setupReducer';
import { EpicDependencies, InputData, TaskState } from 'src/tasks/categorial-classification/helpers/types';
import { testAssignmentId } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';

export const initApp = ({ inputData, isReadOnly }: { inputData: InputData; isReadOnly?: boolean }) => {
  const assignmentId = testAssignmentId; // Whenever we really need to change it - add to initializeSequence.

  const { offers: inputOffers } = inputData;

  const aliasMaker = createApiMock<AliasMaker>();

  if (!isReadOnly) {
    initActivityMonitor(aliasMaker);
  }

  const initialState = setupInitialState(inputOffers);
  const reducer = setupReducer(initialState);
  const storageManager = new StorageManager(assignmentId!);
  const epicMiddleware = createEpicMiddleware<AnyAction, AnyAction, TaskState, EpicDependencies>({
    dependencies: { storageManager, aliasMaker },
  });
  const store = createStore(reducer, applyMiddleware(epicMiddleware));

  epicMiddleware.run(rootEpic);

  const submitHolder: { submit: () => Promise<any> } = {
    submit: () =>
      new Promise<any>(resolve => {
        resolve(undefined);
      }),
  };
  const onSubmit = (handler: () => Promise<any>) => {
    submitHolder.submit = handler;
  };

  const app = mount(
    <Provider store={store}>
      <AliasMakerContext.Provider value={aliasMaker}>
        <App onSubmit={onSubmit} />
      </AliasMakerContext.Provider>
    </Provider>
  );

  return { app, submitHolder, store, storageManager, aliasMaker, data: initialState };
};
