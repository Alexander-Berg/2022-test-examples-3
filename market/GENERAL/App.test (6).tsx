import testStore from 'src/helpers/testStore';
import routes from 'src/pages/routes';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import App from './App';

describe('<App />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');
    const { store, history } = testStore();

    ReactDOM.render(<App history={history} routes={routes} store={store} />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
