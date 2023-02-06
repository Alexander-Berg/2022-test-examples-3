import React, { FC } from 'react';
import { createStore } from 'redux';
import { render } from '@testing-library/react';
import { createStore as createReatomStore } from '@reatom/core';
import { Provider } from 'react-redux';
import { Provider as ReatomProvider } from '@reatom/react';

import { SettingsActions } from './SettingsActions';
import { rootReducer } from 'src/store/root/reducer';

const store = createStore(rootReducer);
const reatomStore = createReatomStore();
const Wrapper: FC = ({ children }) => {
  return (
    <ReatomProvider value={reatomStore}>
      <Provider store={store}>{children}</Provider>
    </ReatomProvider>
  );
};

describe('SettingsActions', () => {
  it('should be render without errors', () => {
    render(
      <Wrapper>
        <SettingsActions />
      </Wrapper>
    );
  });
});
