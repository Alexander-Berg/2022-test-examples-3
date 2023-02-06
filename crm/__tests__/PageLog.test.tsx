import React from 'react';
import { createMemoryHistory } from 'history';
import { cleanup, render, screen, waitFor } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import store from 'store/store';
import { Route, Router } from 'react-router-dom';
import { MemoryHistory } from 'history/createMemoryHistory';
import zip from 'lodash/zip';
import PageLog from '../PageLog';

const origin = document.location.origin;

const logUrl = '/admin/system/log';

const locations = ['?mailId=0', '?issue2line=1', '?startrek=2', '?headers=3', '?mail=4'];

const sources = [
  '/space/view/mail/rulelog?mailId=0',
  '/space/view/rule/log/issue2line/issue/1',
  '/space/view/rule/log/startrek/issue/2',
  '/download/mail/headers/3',
  '/download/mail/4/4.msg',
];

const values = ['0', '1', '2', '3', '4'];

let history: MemoryHistory;

const setup = () => {
  history = createMemoryHistory();
  history.push(logUrl);

  render(
    <Provider store={store}>
      <Router history={history}>
        <Route path={`/admin/system/log`} component={PageLog} />
      </Router>
    </Provider>,
  );
};

describe('PageLog', () => {
  it('renders without params', () => {
    setup();
    expect(screen.getByRole('textbox')).toBeVisible();
    expect(document.querySelector('iframe')).toBeNull();
    cleanup();
  });

  describe.each(zip(locations, values, sources))(
    'loads with params %s',
    (location: string, value: string, source: string) => {
      beforeAll(setup);
      afterAll(cleanup);

      beforeEach(() => {
        history.push(logUrl + location);
      });

      it('sets textbox value', async () => {
        await waitFor(() => {
          expect((screen.getByRole('textbox') as HTMLInputElement).value).toBe(value);
        });
      });

      it('sets frame src', () => {
        expect((document.querySelector('iframe') as HTMLIFrameElement).src).toBe(origin + source);
      });
    },
  );

  describe.each(zip(values, locations, sources))(
    'search with %s`th button works',
    (value: string, location: string, source: string) => {
      beforeAll(setup);
      afterAll(cleanup);

      beforeEach(async () => {
        const input = screen.getByRole('textbox');
        userEvent.clear(input);
        userEvent.type(input, value);
        (await screen.findAllByRole('button'))[value].click();
      });

      it('sets query params', () => {
        expect(history.location.search).toBe(location);
      });

      // eslint-disable-next-line mocha/no-identical-title
      it('sets frame src', () => {
        expect((document.querySelector('iframe') as HTMLIFrameElement).src).toBe(origin + source);
      });
    },
  );
});
