import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { Category } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { GetSizeMeasuresInfoResponse } from '@yandex-market/market-proto-dts/Market/Mbo/SizeMeasures';

import { InputData } from 'src/shared/common-logs/helpers/types';
import { Mappings } from 'src/shared/common-logs/services/StorageManager';
import { App } from 'src/tasks/common-logs/components/App/App';
import { submitActions } from 'src/tasks/common-logs/store/submit/actions';
import { initCommonLogsRequests } from 'src/tasks/common-logs/test/utils/initCommonLogsRequests';
import { initCommonLogsStore } from 'src/tasks/common-logs/test/utils/initCommonLogsStore';
import { errorLogger } from 'src/shared/services/instances';

export const testAssignmentId = '000001b3dd--5ce3be9ea007520105334a63';

export const initCommonLogsApp = (init: {
  initialData: InputData;
  mappings?: Record<string, Mappings>;
  sizeMeasuresInfo?: GetSizeMeasuresInfoResponse;
  isAsyncMatching?: boolean;
  isReadOnly?: boolean;
  categoryData?: Category;
}) => {
  const { data, store, assignmentId, aliasMaker, storageManager, actions } = initCommonLogsStore(init);
  const { mappings, sizeMeasuresInfo, isAsyncMatching, categoryData } = init;

  const submitHolder: { submit: () => Promise<any> } = {
    submit: () =>
      new Promise<any>(resolve => {
        resolve(undefined);
      }),
  };
  const onSubmit = (handler: () => Promise<any>) => {
    submitHolder.submit = handler;
  };

  onSubmit(() => new Promise((resolve, reject) => store.dispatch(submitActions.start({ resolve, reject }))));

  const app: ReactWrapper = mount(
    <App
      input={data}
      assignmentId={assignmentId}
      proxy={jest.fn()}
      onSubmit={onSubmit}
      aliasMaker={aliasMaker}
      store={store}
      errorLogger={errorLogger}
    />
  );

  initCommonLogsRequests({ aliasMaker, app, data, mappings, sizeMeasuresInfo, isAsyncMatching, categoryData });

  return { app, submitHolder, store, storageManager, aliasMaker, data, actions };
};
