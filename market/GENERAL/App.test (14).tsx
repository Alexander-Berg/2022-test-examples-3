import React from 'react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { render } from '@testing-library/react';

import { App } from './App';
import { setupApi } from '../test/setupApi';
import { ApiContext } from './context/ApiContext';
import { ThemeContextProvider } from './context/ThemeContext';
import { createAppStore } from './store/createStore';
import { createMemoryHistory } from 'history';
import { RootModelContext, RootModel } from 'src/models/root.model';
import { stdApiErrorHandler } from './utils/utils';

describe('<App />', () => {
  const api = setupApi();
  const history = createMemoryHistory();
  const store = createAppStore({ api, history });
  const apiErrorHandler = stdApiErrorHandler();

  const rootModel = new RootModel(api, apiErrorHandler);

  it('render without errors', () => {
    const { container } = render(
      <MemoryRouter>
        <Provider store={store}>
          <ApiContext.Provider value={api}>
            <RootModelContext.Provider value={rootModel}>
              <ThemeContextProvider>
                <App />
              </ThemeContextProvider>
            </RootModelContext.Provider>
          </ApiContext.Provider>
        </Provider>
      </MemoryRouter>
    );
    expect(container.firstChild).toBeInTheDocument();
  });
});
