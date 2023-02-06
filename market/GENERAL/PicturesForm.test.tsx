import React from 'react';
import { shopModel } from 'src/test/data/shopModel';
import { PicturesForm, infoAboutManual, manualTypeText, urlError } from './PicturesForm';
import { setupWithReatom } from 'src/test/withReatom';
import { fireEvent, waitFor } from '@testing-library/react';
import { shopModelsAtom } from 'src/store/shopModels/shopModels.atom';
import { resolveSaveModelsRequest } from 'src/test/api/resolves';
import { ShopModelView } from 'src/java/definitions';
import { Api } from 'src/java/Api';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

const pictureUrl = 'https://www.example.com';

const getModelAfterSave = (api: MockedApiObject<Api>) =>
  (api.allActiveRequests as any).shopModelController[0].requests[0][0][0] as ShopModelView;

const clickSave = (app: any) => {
  const saveButton = app.getByText('Сохранить');
  fireEvent.click(saveButton);
};

const resolveRequest = (api: MockedApiObject<Api>) => {
  resolveSaveModelsRequest(api, []);
  expect(api.allActiveRequests).toEqual({});
};

export const resolveLoadPicturesRequest = (api: MockedApiObject<Api>) => {
  api.imageController.upload.next().resolve({ path: pictureUrl, height: 0, width: 0 });
};

const addPictureUrl = (app: any, url: string) => {
  const form = app.getByTestId('pictures-form');
  const input = app.getByPlaceholderText('Введите ссылку на изображение');

  fireEvent.change(input, { target: { value: url } });
  fireEvent.submit(form);
};

test('render PicturesForm manualPictures=false', () => {
  const onClose = jest.fn();
  const { app } = setupWithReatom(<PicturesForm model={shopModel} onClose={onClose} />);

  app.getByText(infoAboutManual);

  const input = app.getByPlaceholderText('Введите ссылку на изображение');
  expect(input).toBeInTheDocument();
});

test('render PicturesForm manualPictures=true', () => {
  const onClose = jest.fn();
  const { app } = setupWithReatom(<PicturesForm model={{ ...shopModel, manualPictures: true }} onClose={onClose} />);

  expect(app.getByText(manualTypeText)).toBeInTheDocument();
});

test('PicturesForm enter invalid picture url', () => {
  const onClose = jest.fn();
  const { app } = setupWithReatom(<PicturesForm model={{ ...shopModel, manualPictures: true }} onClose={onClose} />);

  addPictureUrl(app, '123');

  app.getByText(urlError);
});

test('PicturesForm add/save picture', async () => {
  const onClose = jest.fn();
  const { app, api } = setupWithReatom(
    <PicturesForm model={{ ...shopModel, manualPictures: true }} onClose={onClose} />,
    { shopModelsAtom }
  );

  addPictureUrl(app, pictureUrl);

  expect(app.getByTitle(pictureUrl)).toBeInTheDocument();

  clickSave(app);

  const updatedModel = getModelAfterSave(api);
  expect(updatedModel.pictures.find(el => el.url === pictureUrl)).toBeTruthy();

  resolveRequest(api);

  await waitFor(() => {
    expect(onClose.mock.calls.length).toBe(1);
  });
});

test('PicturesForm remove picture', async () => {
  const onClose = jest.fn();
  const { app, api } = setupWithReatom(
    <PicturesForm model={{ ...shopModel, manualPictures: true }} onClose={onClose} />,
    { shopModelsAtom }
  );
  const picture = app.getByTitle(shopModel.pictures[0].url);
  expect(picture).toBeInTheDocument();

  const removeBtn = app.getByTestId('remove-pictures');
  fireEvent.click(removeBtn);

  const g = app.queryByTitle(shopModel.pictures[0].url);
  expect(g).toBeNull();

  clickSave(app);

  const updatedModel = getModelAfterSave(api);
  expect(updatedModel.pictures.length).toBeFalsy();

  resolveRequest(api);

  await waitFor(() => {
    expect(onClose.mock.calls.length).toBe(1);
  });
});
