import React from 'react';
import { render, fireEvent } from '@testing-library/react';

import { PictureList } from './PictureList';

const pictures = ['https://www.example.com', 'https://www.example.com/123'];

const renderPictureList = () => {
  const onSelect = jest.fn((url: string) => url);
  const onChangePictures = jest.fn((urls: string[]) => urls);
  const app = render(
    <PictureList
      droppableId="21"
      pictures={pictures}
      selected=""
      onChangePictures={onChangePictures}
      onSelect={onSelect}
      errors={{}}
    />
  );
  return {
    onSelect,
    onChangePictures,
    app,
  };
};

test('render PictureList select pictures', () => {
  const { app, onSelect } = renderPictureList();

  const picture = app.getByTitle(pictures[0]);

  fireEvent.click(picture);

  expect(onSelect.mock.calls.length).toBe(1);
  expect(onSelect.mock.results[0].value).toMatch(pictures[0]);
});

test('render PictureList remove picture', () => {
  const { app, onChangePictures } = renderPictureList();

  const removeBtn = app.getAllByTestId('remove-pictures')[0];
  fireEvent.click(removeBtn);

  expect(onChangePictures.mock.calls.length).toBe(1);
  expect(onChangePictures.mock.results[0].value).toHaveLength(1);
});
