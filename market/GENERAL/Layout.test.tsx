import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { MemoryRouter } from 'react-router';
import { Layout } from './Layout';

describe('<Layout />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');
    const user = { id: 0, login: '', roles: [] };

    ReactDOM.render(
      <MemoryRouter>
        <Layout
          getCurrentUser={jest.fn()}
          authenticating={false}
          notifications={[]}
          hideNotification={jest.fn()}
          removeNotification={jest.fn()}
          user={user}
          route={{}}
        />
      </MemoryRouter>,
      div
    );
    ReactDOM.unmountComponentAtNode(div);
  });
});
