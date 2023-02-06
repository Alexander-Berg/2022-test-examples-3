import { fireEvent, render } from '@testing-library/react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import React from 'react';

import { ItemTypes } from 'src/types';
import { LoadingImagesContext } from 'src/contexts';
import { PictureEditorImage, PictureEditorImageProps } from './PictureEditorImage';
import { cnPictureEditorImage } from './utils';

const BASE_PROPS: PictureEditorImageProps = { index: 0, image: { url: 'test_src' } as any, type: ItemTypes.IMAGE_SKU };

jest.mock('../ImagePopup', () => ({
  ImagePopup: ({ onClose }: { onClose: () => void }) => {
    // eslint-disable-next-line jsx-a11y/control-has-associated-label
    return <button type="button" className="ImagePopupMock-CloseButton" onClick={onClose} />;
  },
}));

describe('PictureEditorImage', () => {
  it('renders', () => {
    const { container } = renderImage({ ...BASE_PROPS });

    const wrapper = container.getElementsByClassName(cnPictureEditorImage('Wrapper')).item(0);
    expect(wrapper).toBeDefined();
    expect(wrapper!.getAttribute('title')).toEqual(null);
    expect(container.getElementsByClassName(cnPictureEditorImage('Spinner'))).toHaveLength(0);
    expect(container.getElementsByClassName(cnPictureEditorImage('ErrorMessage'))).toHaveLength(0);
    expect(document.body.getElementsByClassName('RkmPicImagePopup')).toHaveLength(0);

    const imageBlock = container.getElementsByClassName(cnPictureEditorImage()).item(0);

    expect(imageBlock).toBeDefined();
    expect(imageBlock!.className).toEqual(cnPictureEditorImage()); // check that block has no flag classes

    const image = container.getElementsByTagName('img').item(0);

    expect(image).toBeDefined();
    expect(image!.getAttribute('src')).toEqual('test_src');
  });
  it('validation error renders', () => {
    const { container } = renderImage({
      ...BASE_PROPS,
      image: { ...BASE_PROPS.image, validationMessage: 'test_error' },
    });

    const error = container.getElementsByClassName(cnPictureEditorImage('ErrorMessage')).item(0);
    expect(error).toBeDefined();
    expect(error!.textContent).toEqual('test_error');
  });
  it('image popup renders', async () => {
    const { container } = renderImage({ ...BASE_PROPS });

    const imageBlock = container.getElementsByClassName(cnPictureEditorImage()).item(0);

    expect(container.getElementsByClassName('ImagePopupMock-CloseButton')).toHaveLength(0);

    fireEvent.click(imageBlock!);

    expect(container.getElementsByClassName('ImagePopupMock-CloseButton')).toHaveLength(1);

    const closeButton = container.getElementsByClassName('ImagePopupMock-CloseButton').item(0);

    expect(closeButton).toBeDefined();

    fireEvent.click(closeButton!);

    expect(container.getElementsByClassName('ImagePopupMock-CloseButton')).toHaveLength(0);
  });
  it('spin renders', () => {
    const { container } = renderImage({ ...BASE_PROPS }, { test_src: true });

    expect(container.getElementsByClassName(cnPictureEditorImage('Spinner'))).toHaveLength(1);
  });
  it('title renders', () => {
    const { container } = renderImage({
      ...BASE_PROPS,
      image: { ...BASE_PROPS.image, link: { imagePicker: { isWhiteBackground: false } } as any },
    });

    const wrapper = container.getElementsByClassName(cnPictureEditorImage('Wrapper')).item(0);
    expect(wrapper!.getAttribute('title')).toEqual('Фон не белый.');
  });
  it('select toggle works', () => {
    const onToggleSelect = jest.fn();
    const { container } = renderImage({ ...BASE_PROPS, onToggleSelect });

    const imageBlock = container.getElementsByClassName(cnPictureEditorImage()).item(0);
    fireEvent.click(imageBlock!, { shiftKey: true });

    expect(onToggleSelect).toBeCalledTimes(1);
    expect(onToggleSelect).toBeCalledWith('test_src');
  });
});

function renderImage(props: PictureEditorImageProps, loadingMap: Record<string, boolean> = {}) {
  return render(
    <DndProvider backend={HTML5Backend}>
      <LoadingImagesContext.Provider value={loadingMap}>
        <PictureEditorImage {...props} />
      </LoadingImagesContext.Provider>
    </DndProvider>
  );
}
