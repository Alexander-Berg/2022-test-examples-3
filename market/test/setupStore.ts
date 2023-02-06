import { setupApi } from './api/setupApi';
import { initReatomStore } from '../store/reatom/reatomStore';

export const setupStore = () => {
  const api = setupApi();
  const reatomStore = initReatomStore(api);
  return reatomStore;
};
