import { mount, ReactWrapper } from 'enzyme';
import * as React from 'react';
import { Store } from 'redux';
import { Provider } from 'react-redux';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { AsyncFileDownloader, AsyncFileDownloaderProps } from './AsyncFileDownloader';
import { setupApi, setupTestStore } from 'src/test/setup';
import { RootState } from 'src/store/root/reducer';
import Api from 'src/Api';
import { Button } from 'src/components';
import { ApiContext } from 'src/context';
import { ActionStatus } from 'src/java/definitions';

const setupComponent = (props?: Partial<AsyncFileDownloaderProps>) => {
  const api = setupApi();
  const { store } = setupTestStore(api);
  const defaultProps: AsyncFileDownloaderProps = {
    caption: '',
    onStartDownload: () => Promise.resolve(1),
    hasData: true,
    ...props,
  };
  const wrapper: ReactWrapper<AsyncFileDownloaderProps> = mount(
    <Provider store={store}>
      <ApiContext.Provider value={api}>
        <AsyncFileDownloader {...defaultProps} />
      </ApiContext.Provider>
    </Provider>
  );
  return { api, store, wrapper, props: defaultProps };
};

const resolveDownloadExport = (api: MockedApiObject<Api>) => {
  api.backgroundActionResultController.getActionResult.next().resolve({
    message: 'Done',
    status: ActionStatus.FINISHED,
    url: 'mock_url',
  });
};

const resolveDownloadExportFailed = (api: MockedApiObject<Api>, message: string, status: ActionStatus) => {
  api.backgroundActionResultController.getActionResult.next().resolve({ message, status });
};

const resolveDownloadExportFailure = (api: MockedApiObject<Api>, error: any) => {
  api.backgroundActionResultController.getActionResult.next().reject(error);
};

const expectErrorMessage = (store: Store<RootState>, message?: string) => {
  const errMessage = store.getState().globalMessages;
  expect(errMessage[0].title).toMatch(message || 'Не удалось загрузить файл');
};

describe('<FileDownloader />', () => {
  it('Render component', () => {
    const { wrapper } = setupComponent();
    const fileDownloader = wrapper.find(AsyncFileDownloader);
    expect(fileDownloader).toHaveLength(1);
  });

  it('Start and end upload file', () => {
    const { wrapper, api } = setupComponent();
    const fileDownloader = wrapper.find(AsyncFileDownloader).find(Button).first();
    fileDownloader.simulate('click');

    return new Promise(resolve => {
      setTimeout(resolve, 1000);
    }).then(() => {
      resolveDownloadExport(api);
      expect(api.allActiveRequests).toEqual({});
    });
  });

  it('Success start and failure load file', async () => {
    const { wrapper, store, api } = setupComponent();
    const fileDownloader = wrapper.find(AsyncFileDownloader).find(Button).first();
    fileDownloader.simulate('click');

    const httpError = { status: 500, header: new Set(), text: () => Promise.resolve('server error') };
    await new Promise(resolve => setTimeout(resolve, 1000)).then(() => resolveDownloadExportFailure(api, httpError));

    return new Promise(resolve => setTimeout(resolve, 1000)).then(() => {
      expectErrorMessage(store, 'server error');
    });
  });

  it('Success start and failure result load file', () => {
    const { wrapper, store, api } = setupComponent();
    const fileDownloader = wrapper.find(AsyncFileDownloader).find(Button).first();
    fileDownloader.simulate('click');
    return new Promise(resolve => setTimeout(resolve, 1000)).then(() => {
      resolveDownloadExportFailed(api, 'Ошибка на беке', ActionStatus.FAILED);
      expectErrorMessage(store, 'Ошибка на беке');
    });
  });

  it('Exceeded the request limit', () => {
    const { wrapper, store, api } = setupComponent({ maxRequestCount: 0 });
    const fileDownloader = wrapper.find(AsyncFileDownloader).find(Button).first();
    fileDownloader.simulate('click');
    return new Promise(resolve => setTimeout(resolve, 1000)).then(() => {
      resolveDownloadExportFailed(api, 'In process', ActionStatus.IN_PROGRESS);
      expectErrorMessage(store);
    });
  });

  it('Retry load file', () => {
    const { wrapper, api } = setupComponent();
    const fileDownloader = wrapper.find(AsyncFileDownloader).find(Button).first();
    fileDownloader.simulate('click');
    return new Promise(resolve => setTimeout(resolve, 1000))
      .then(() => {
        resolveDownloadExportFailed(api, 'In process', ActionStatus.IN_PROGRESS);
        return new Promise(resolve => setTimeout(resolve, 1000));
      })
      .then(() => {
        resolveDownloadExport(api);
        expect(api.allActiveRequests).toEqual({});
      });
  });

  it('Disabled button when no data', () => {
    const { wrapper } = setupComponent({ hasData: false });
    const buttons = wrapper.find(Button);
    const buttonProps = buttons.first().getElement().props;
    expect(buttonProps.disabled).toEqual(true);
  });
});
