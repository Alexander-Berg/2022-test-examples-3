import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import MappingModerationPartnersApp from 'src/tasks/mapping-moderation-partners/components/App/MappingModerationPartnersApp';
import { configureStore } from 'src/tasks/mapping-moderation-partners/utils/configureStore';
import input from '../../sample-data';

const store = configureStore(input);

describe('MappingModerationPartnersApp', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');
    ReactDOM.render(
      <Provider store={store}>
        <MappingModerationPartnersApp onSubmit={() => undefined} />
      </Provider>,
      div
    );
    ReactDOM.unmountComponentAtNode(div);
  });
});
