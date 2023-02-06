import RestService from 'src/services/RestService';
import configureStore from 'src/store/configureStore';
import { EpicDependencies } from 'src/store/types';
import { createMemoryHistory } from 'history';

export default function testStore(customDeps: any = {}, initialState: any = {}) {
  const history = createMemoryHistory();

  const dependencies: EpicDependencies = {
    api: new RestService(),
    history,
    ...customDeps,
  };

  return {
    store: configureStore({ initialState, dependencies }),
    history,
  };
}
