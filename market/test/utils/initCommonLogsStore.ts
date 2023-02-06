import clone from 'ramda/es/clone';
import { AnyAction } from 'typescript-fsa';
import { createApiMock } from '@yandex-market/mbo-test-utils';

import { InputData } from 'src/shared/common-logs/helpers/types';
import { AliasMaker } from 'src/shared/services';
import StorageManager, { Mappings } from 'src/shared/common-logs/services/StorageManager';
import configureStore from 'src/tasks/common-logs/store/configureStore';
import getInitialState from 'src/tasks/common-logs/store/getInitialState';
import { offersActions } from 'src/tasks/common-logs/store/offers/actions';
import { testAssignmentId } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';

export const initCommonLogsStore = (init: {
  initialData: InputData;
  mappings?: Record<string, Mappings>;
  isReadOnly?: boolean;
}) => {
  const { initialData, mappings, isReadOnly } = init;
  const data = clone(initialData);
  const aliasMaker = createApiMock<AliasMaker>();
  const assignmentId = testAssignmentId; // Whenever we really need to change it - add to initializeSequence.
  const storageManager = new StorageManager(assignmentId);
  const actions: AnyAction[] = []; // Could be inspected for simple history of actions
  if (mappings) {
    storageManager.setMappings(mappings);
  }

  const store = configureStore(
    {
      initialState: getInitialState(data, assignmentId, isReadOnly),
      dependencies: { aliasMaker, storageManager },
    },
    () => next => action => {
      try {
        actions.push(action);
        next(action);
      } catch (e) {
        // Really helpful to debug errors in reducers.
        // eslint-disable-next-line no-console
        console.error('Error in reducer', e);
        throw e;
      }
    }
  );

  store.dispatch(offersActions.init());

  return { data, store, assignmentId, aliasMaker, storageManager, actions };
};
