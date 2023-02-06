import { DndProvider } from 'react-dnd';
import { Router } from 'react-router-dom';
import { Provider } from 'react-redux';
import { createMemoryHistory } from 'history';
import store from 'store';
import { IntlProvider } from 'react-intl';
import React from 'react';
import { ModalContainer } from 'lego/utils/modalForm';
import messages from 'localization/ru';
import HTML5Backend from 'components/ReactDndModifiedBackend';

const history = createMemoryHistory();

export const TestBed = ({ children }) => (
  <DndProvider backend={HTML5Backend}>
    <IntlProvider textComponent="span" locale="en" messages={messages}>
      <Router history={history}>
        <Provider store={store}>
          <>
            {children}
            <ModalContainer />
          </>
        </Provider>
      </Router>
    </IntlProvider>
  </DndProvider>
);
