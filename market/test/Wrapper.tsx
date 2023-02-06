import React, { FC } from 'react';
import { Provider } from 'react-redux';
import { createMemoryHistory } from 'history';

import { RestService } from 'src/services';
import { configureStore } from 'src/store/configureStore';

const api = new RestService();
const history = createMemoryHistory();

const store = configureStore({ dependencies: { history, api } });

export const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};
