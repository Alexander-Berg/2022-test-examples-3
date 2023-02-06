import React from 'react';
import { act, render } from '@testing-library/react';

import { setupTestProvider } from 'test/setupApp';
import { MdmFileHistoryPage } from './FileHistory';
import { FileStatus } from 'src/java/definitions';

describe('<FileHistory />', () => {
  it('load with initial query params', async () => {
    const requestedDate = new Date();
    requestedDate.setFullYear(2022, 4, 13);
    requestedDate.setHours(0, 0, 0, 0);
    const { api, Provider } = setupTestProvider('?page=2&fileType=RSL&uploadedAtStart=2022-05-13');

    const app = render(
      <Provider>
        <MdmFileHistoryPage />
      </Provider>
    );

    expect(api.mdmFileHistoryController.find.activeRequests()).toHaveLength(1);

    await act(async () => {
      api.mdmFileHistoryController.find
        .next(
          (filter, page) =>
            filter.fileType === 'RSL' && page.page === 1 && filter.uploadedAtStart === requestedDate.toISOString()
        )
        .resolve({
          items: [
            {
              fileType: 'RSL',
              fileStatus: FileStatus.OK,
              filename: 'highlander.tsxoxo',
              lastStatusChanged: '',
              s3Path: 'Valdemar',
              userLogin: 'vovchik',
            },
          ],
          totalCount: 2,
        });
    });

    expect(app.getByText('ovchik', { exact: false })).toBeInTheDocument();
  });
});
