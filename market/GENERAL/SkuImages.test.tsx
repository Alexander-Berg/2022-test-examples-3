import { fireEvent, render, screen } from '@testing-library/react';
import React from 'react';
import { partialWrapper } from '@yandex-market/mbo-test-utils';
import { ImageType, NormalisedModel } from '@yandex-market/mbo-parameter-editor';

import { SkuImages } from './SkuImages';

describe('SkuImages', () => {
  it('renders empty', () => {
    render(<SkuImages sku={partialWrapper<NormalisedModel>({})} selectedUrls={[]} setSelectedUrls={jest.fn()} />);

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
  it('renders with data', () => {
    render(
      <SkuImages
        sku={partialWrapper<NormalisedModel>({
          normalizedPictures: [
            { url: 'test_src1', type: ImageType.REMOTE },
            { url: 'test_src2', type: ImageType.REMOTE },
          ],
        })}
        selectedUrls={[]}
        setSelectedUrls={jest.fn()}
      />
    );

    expect(screen.queryAllByRole('img')).toHaveLength(2);
    expect(screen.queryByRole('button')).toBeInTheDocument();
  });
  it('handles select all', () => {
    const setSelectedUrls = jest.fn();
    render(
      <SkuImages
        sku={partialWrapper<NormalisedModel>({
          normalizedPictures: [
            { url: 'test_src1', type: ImageType.REMOTE },
            { url: 'test_src2', type: ImageType.REMOTE },
          ],
        })}
        selectedUrls={[]}
        setSelectedUrls={setSelectedUrls}
      />
    );

    fireEvent.click(screen.getByRole('button'));

    expect(setSelectedUrls).toBeCalledWith(['test_src1', 'test_src2']);
  });
});
