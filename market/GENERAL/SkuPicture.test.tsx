import { fireEvent, render, screen } from '@testing-library/react';
import React from 'react';
import { ImageType } from '@yandex-market/mbo-parameter-editor';

import { SkuPicture } from './SkuPicture';

describe('SkuPicture', () => {
  it('renders', () => {
    render(
      <SkuPicture image={{ url: 'test_src', type: ImageType.REMOTE }} selectedUrls={[]} setSelectedUrls={jest.fn()} />
    );

    expect(screen.queryByRole('img')).toHaveAttribute('src', 'test_src');
  });
  it('handle click unselected', () => {
    const setSelectedUrls = jest.fn();

    render(
      <SkuPicture
        image={{ url: 'test_src', type: ImageType.REMOTE }}
        selectedUrls={[]}
        setSelectedUrls={setSelectedUrls}
      />
    );

    fireEvent.click(screen.getByRole('img'));

    expect(setSelectedUrls).toBeCalledWith(['test_src']);
  });
  it('handle click selected', () => {
    const setSelectedUrls = jest.fn();

    render(
      <SkuPicture
        image={{ url: 'test_src', type: ImageType.REMOTE }}
        selectedUrls={['test_src']}
        setSelectedUrls={setSelectedUrls}
      />
    );

    fireEvent.click(screen.getByRole('img'));

    expect(setSelectedUrls).toBeCalledWith([]);
  });
});
