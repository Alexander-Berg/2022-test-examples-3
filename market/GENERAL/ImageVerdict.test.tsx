import React from 'react';
import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import selectEvent from 'react-select-event';

import { ImageVerdict } from './ImageVerdict';
import { renderWithProvider } from 'src/test/setupTestProvider';
import { ImageVerdictRequest, RequestMode } from 'src/java/definitions';
import { api } from 'src/test/singletons/apiSingleton';

const imgUrl = 'https://img.best-kitchen.ru/images/products/1/7430/77602054/3.jpg';

const getRequestParams = () => {
  const controllerRequests = (api.allActiveRequests as any).partnerContentServiceController[0].requests;

  return controllerRequests[0][0];
};

const typeImgUrl = (url: string) => {
  const urlInput = screen.getAllByRole('textbox')[1];
  expect(urlInput).toBeTruthy();
  userEvent.type(urlInput, url);
};

const saveChanges = () => {
  const saveBtn = screen.getByText(/Изменить вердикты/);
  userEvent.click(saveBtn);
  expect((api.allActiveRequests as any).partnerContentServiceController).toBeTruthy();
};

describe('ImageVerdict', () => {
  test.concurrent('with intim mode', async () => {
    renderWithProvider(<ImageVerdict />);

    typeImgUrl(imgUrl);

    await selectEvent.select(screen.getByRole('textbox', { name: 'select' }), 'INTIM');

    saveChanges();

    const requestParams: ImageVerdictRequest[] = getRequestParams();

    await waitFor(() => {
      expect(requestParams[0].requestMode).toBe(RequestMode.INTIM);
    });
    await waitFor(() => {
      expect(requestParams[0].urls[0]).toEqual(imgUrl);
    });

    await act(async () => {
      await api.partnerContentServiceController.acceptImageVerdict.next().resolve([]);
    });
  });
});
