import React from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupTestProvider } from 'test/setupApp';
import { QueueDataPage } from './QueueDataPage';
import { AccessRoles } from '../../context/ConfigContext';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import Api from 'src/Api';

const UPLOADED_FILE = new File(['There can be only one...'], 'Highlander.csv', { type: 'text/csv' });
const MOCK_QUEUE_LIST = ['Qwerty Qwertievna'];

describe('<QueueDataPage />', () => {
  it('load and render', async () => {
    const { Provider, api } = setupTestProvider();
    const app = render(
      <Provider>
        <QueueDataPage />
      </Provider>
    );

    await resolveConfigRequest(api);

    await act(async () => {
      api.mdmManualQueueController.getQueueSet.next().resolve(MOCK_QUEUE_LIST);
    });

    const select = await app.findByText('Выберите очередь');
    userEvent.click(select);

    const queueItem = screen.getByText(MOCK_QUEUE_LIST[0]);
    userEvent.click(queueItem);

    const uploadInput = app.container.querySelector('input[type="file"]') as HTMLInputElement;
    userEvent.upload(uploadInput!, UPLOADED_FILE);

    userEvent.click(app.getByText('С повышенным приоритетом'));

    const submitBtn = app.getByText('Загрузить');
    userEvent.click(submitBtn);

    expect(api.mdmManualQueueController.importCsvForEnqueue.activeRequests()).toHaveLength(1);
    expect(api.mdmManualQueueController.importCsvForEnqueue.activeRequests()[0][2]).toBe(true);
  });

  it('disable submit without selected queue', async () => {
    const { Provider, api } = setupTestProvider();
    const app = render(
      <Provider>
        <QueueDataPage />
      </Provider>
    );
    await resolveConfigRequest(api);
    const uploadInput = app.container.querySelector('input[type="file"]') as HTMLInputElement;
    userEvent.upload(uploadInput!, UPLOADED_FILE);

    const submitBtn = app.getByText('Загрузить');
    userEvent.click(submitBtn);

    expect(api.mdmManualQueueController.importCsvForEnqueue.activeRequests()).toHaveLength(0);
  });

  it('failed queues list request', async () => {
    const { Provider, api } = setupTestProvider();
    const app = render(
      <Provider>
        <QueueDataPage />
      </Provider>
    );
    await resolveConfigRequest(api);

    await act(async () => {
      api.mdmManualQueueController.getQueueSet.next().reject(['testik']);
    });

    expect(app.getByText('Загрузить')).toBeInTheDocument();
  });

  it('failed upload request', async () => {
    const { Provider, api } = setupTestProvider();
    const app = render(
      <Provider>
        <QueueDataPage />
      </Provider>
    );
    await resolveConfigRequest(api);
    const loaderElm = app.container.getElementsByClassName('MboFront_Loader-placeholder').item(0);
    expect(loaderElm).toBeInTheDocument();
    await act(async () => {
      api.mdmManualQueueController.getQueueSet.next().resolve(MOCK_QUEUE_LIST);
    });

    const select = app.getByText('Выберите очередь');
    userEvent.click(select);

    const queueItem = screen.getByText(MOCK_QUEUE_LIST[0]);
    userEvent.click(queueItem);

    const uploadInput = app.container.querySelector('input[type="file"]') as HTMLInputElement;
    userEvent.upload(uploadInput!, UPLOADED_FILE);

    const submitBtn = app.getByText('Загрузить');
    userEvent.click(submitBtn);

    await act(async () => {
      api.mdmManualQueueController.importCsvForEnqueue.next().reject(MOCK_QUEUE_LIST);
    });

    expect(loaderElm).not.toBeInTheDocument();
  });

  it('download template file', async () => {
    const { Provider, api } = setupTestProvider();
    const app = render(
      <Provider>
        <QueueDataPage />
      </Provider>
    );
    await resolveConfigRequest(api);

    expect(() => {
      userEvent.click(app.getByText('Шаблон SSKU'));
    }).not.toThrow();
  });
});

async function resolveConfigRequest(api: MockedApiObject<Api>) {
  await act(async () => {
    api.configController.currentUser.next().resolve({ roles: [AccessRoles.DEVELOPER], login: 'Indiana Jones' });
    api.configController.config.next().resolve({ rolesDescription: {}, idmUrl: '', mboUrl: '' });
  });
}
