import React from 'react';
import { Route, withRouter } from 'react-router';
import { createMemoryHistory } from 'history';

import Page from './material-list.page';

import { getTestProvider, render } from '@/test-utils';
import { listAction } from '@/pages/material-list/actions/filter.action';
import { objToQuery } from '@/utils/url';

const MaterialListPage = withRouter(Page);

describe('<MaterialListPage />', () => {
  it('should be rendered without errors', () => {
    expect(() => {
      render(<MaterialListPage />);
    }).not.toThrow();
  });

  it('should restore filters from url', () => {
    const queryObject = {
      limit: 10,
      search: 'qwerty',
      nodeId: 'asdf',
      author: ['563073048', '372301071'],
      status: ['NEW', 'PUBLISHED_AND_UPDATED'],
      type: ['cms_problems_info', 'Приложение Маркета/ Каталог'],
      sort: {
        key: 'updated',
        value: 'desc',
      },
    };
    const initialSearch = `?${objToQuery(queryObject)}`;
    const history = createMemoryHistory({
      initialEntries: [`/${initialSearch}`],
    });
    const { Provider, store } = getTestProvider();
    const origDispatch = store.dispatch;
    store.dispatch = jest.fn(origDispatch);
    render(
      <Provider>
        <Route location={history.location}>
          <MaterialListPage />
        </Route>
      </Provider>
    );

    expect(store.dispatch).toHaveLastReturnedWith({ payload: queryObject, type: listAction.started.type });
  });
});
