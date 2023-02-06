import { fireEvent, render, screen } from '@testing-library/react';
import React from 'react';
import { partialWrapper } from '@yandex-market/mbo-test-utils';
import { ImageType, NormalisedModel } from '@yandex-market/mbo-parameter-editor';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { DndProvider } from 'react-dnd';

import { SkuImagesBlock } from 'src/components';
import { SLOT_CLASS_NAME } from 'src/components/Slot';

const CLOSE_CLASS = 'SkuImageCopyModalMock-CloseButton';

const SkuImageCopyModalMock = jest.fn();

jest.mock('../SkuImageCopyModal', () => ({
  SkuImageCopyModal: (props: { onClose: () => void }) => {
    SkuImageCopyModalMock(props);

    // eslint-disable-next-line jsx-a11y/control-has-associated-label
    return <button type="button" data-testid={CLOSE_CLASS} className={CLOSE_CLASS} onClick={props.onClose} />;
  },
}));

describe('SkuImagesBlock', () => {
  it('renders empty', () => {
    const { container } = render(
      <DndProvider backend={HTML5Backend}>
        <SkuImagesBlock
          sku={partialWrapper<NormalisedModel>({
            normalizedPictures: [],
          })}
        />
      </DndProvider>
    );

    expect(container.getElementsByClassName(SLOT_CLASS_NAME)).toHaveLength(1);
  });
  it('renders images', () => {
    render(
      <DndProvider backend={HTML5Backend}>
        <SkuImagesBlock
          sku={partialWrapper<NormalisedModel>({
            normalizedPictures: [
              { url: 'test_src1', type: ImageType.REMOTE },
              { url: 'test_src2', type: ImageType.REMOTE },
            ],
          })}
        />
      </DndProvider>
    );

    const images = screen.getAllByRole('img');

    expect(images).toHaveLength(2);
    expect(images[0]).toHaveAttribute('src', 'test_src1');
    expect(images[1]).toHaveAttribute('src', 'test_src2');
  });
  it('shows modal', () => {
    const { container } = render(
      <DndProvider backend={HTML5Backend}>
        <SkuImagesBlock
          sku={partialWrapper<NormalisedModel>({
            normalizedPictures: [],
          })}
        />
      </DndProvider>
    );

    fireEvent.click(container.getElementsByClassName(SLOT_CLASS_NAME).item(0)!);

    expect(screen.queryByTestId(CLOSE_CLASS)).toBeInTheDocument();

    fireEvent.click(screen.getByTestId(CLOSE_CLASS));

    expect(screen.queryByTestId(CLOSE_CLASS)).not.toBeInTheDocument();
  });
});
