import React from 'react';
import { withRouter } from 'react-router';
import { render } from '@testing-library/react';

import { SignalsPage as Page } from '@/pages/signals/signals.page';
import { getTestProvider } from '@/test-utils';
import { getSignalMessagesAction } from '@/pages/signals/store';
import { SignalWithMessageDto } from '@/dto';

const SignalsPage = withRouter(Page);

describe('<SignalsPage/>', () => {
  it('load and render data', () => {
    const { store, Provider } = getTestProvider();

    store.dispatch(
      getSignalMessagesAction.done({
        params: {},
        result: [
          {
            signal: {
              isAllowDiff: true,
              type: 'type1',
              created: 123456789,
              saved: 987654321,
              data: {
                pageType: 'pageType',
                name: 'Name of signal',
                staffLogin: 'mr.Yandex',
                userId: '012345',
                id: 'docType',
                userName: 'Testik Testovich',
                revId: '123',
              },
            },
            message: { parts: [] },
          },
          {
            signal: {
              type: 'type2',
              created: 123456789,
              saved: 987654321,
              data: {
                pageType: 'pageType',
                name: 'Name of signal2',
                id: 'docType2',
              },
            },
            message: { parts: [] },
          },
        ] as unknown as SignalWithMessageDto[],
      })
    );

    const app = render(
      <Provider>
        <SignalsPage />
      </Provider>
    );

    const link1 = app.getByText('Name of signal');
    expect(link1.getAttribute('href')).toBe('/editor/documents/docType/edit?revision_id=123');

    const link2 = app.getByText('Name of signal2');
    expect(link2.getAttribute('href')).toBe('/editor/documents/docType2/edit');
  });
});
