/* eslint-disable jsx-a11y/control-has-associated-label */
import { fireEvent, getByRole, render } from '@testing-library/react';
import { act } from 'react-dom/test-utils';
import React from 'react';
import { ImageType, NormalizedImage } from '@yandex-market/mbo-parameter-editor';

import { ImageUploadButton } from './ImageUploadButton';
import { ActionsContext, PictureEditorActions } from 'src/contexts';
import { loadImage } from './loadImage';

const MODAL_MOCK_CLASS = 'UrlRequestModalMock';
const MODAL_MOCK_CLOSE_CLASS = 'UrlRequestModalMock-CloseButton';
const MODAL_MOCK_SET_CLASS = 'UrlRequestModalMock-SetUrlButton';

jest.mock('./UrlRequestModal', () => ({
  UrlRequestModal: ({ onClose, setUrl }: { onClose: () => void; setUrl: (url: string) => void }) => {
    return (
      <div className={MODAL_MOCK_CLASS}>
        <button type="button" className={MODAL_MOCK_CLOSE_CLASS} onClick={onClose} />
        <button type="button" className={MODAL_MOCK_SET_CLASS} onClick={() => setUrl('test_url')} />
      </div>
    );
  },
}));

jest.mock('./loadImage');

const useLoadImageMock = loadImage as jest.Mock<Promise<NormalizedImage>>;

const NORMALIZED_IMAGE = { type: ImageType.LOCAL, url: 'test_url' } as NormalizedImage;
const VALID_IMAGE = { type: 'image/jpeg' };

useLoadImageMock.mockReturnValue(Promise.resolve(NORMALIZED_IMAGE));

describe('ImageUploadButton', () => {
  it('renders', () => {
    render(<ImageUploadButton />);
  });

  it('button click passed to hidden input', () => {
    const { container } = render(<ImageUploadButton />);
    const button = getByRole(container, 'button');
    const input = container.getElementsByTagName('input').item(0);

    expect(input).toBeDefined();
    expect(button).toBeDefined();

    input!.onclick = jest.fn();

    fireEvent.click(button);

    expect(input!.onclick).toBeCalledTimes(1);
  });

  it.each([
    [null, 'Для загрузки доступны изображения в форматах: jpeg, png.'],
    [[], 'Для загрузки доступны изображения в форматах: jpeg, png.'],
    [[{ type: 'test' }], 'Для загрузки доступны изображения в форматах: jpeg, png.'],
  ])(`handles input change with files: %j`, (files, result) => {
    const showError = jest.fn();
    const { container } = renderButton({ showError });
    const input = container.getElementsByTagName('input').item(0);

    fireEvent.change(input!, { target: { files } });
    expect(showError).toBeCalledTimes(1);
    expect(showError).toBeCalledWith(result);
  });

  it('handle input change with valid file', () => {
    const showError = jest.fn();
    const { container } = renderButton({ showError });
    const input = container.getElementsByTagName('input').item(0);

    fireEvent.change(input!, { target: { files: [VALID_IMAGE] } });

    expect(container.getElementsByClassName(MODAL_MOCK_CLASS)).toHaveLength(1);
  });

  it('handle modal close', () => {
    const showError = jest.fn();
    const { container } = renderButton({ showError });
    const input = container.getElementsByTagName('input').item(0);

    fireEvent.change(input!, { target: { files: [VALID_IMAGE] } });
    fireEvent.click(container.getElementsByClassName(MODAL_MOCK_CLOSE_CLASS).item(0)!);

    expect(container.getElementsByClassName(MODAL_MOCK_CLASS)).toHaveLength(0);
  });

  it('handle modal set url', async () => {
    const showError = jest.fn();
    const addImageToCart = jest.fn();
    const { container } = renderButton({ addImageToCart, showError });
    const input = container.getElementsByTagName('input').item(0);

    fireEvent.change(input!, { target: { files: [VALID_IMAGE] } });

    await act(async () => {
      fireEvent.click(container.getElementsByClassName(MODAL_MOCK_SET_CLASS).item(0)!);
    });

    expect(showError).toBeCalledTimes(0);
    expect(addImageToCart).toBeCalledTimes(1);
    expect(addImageToCart).toBeCalledWith(NORMALIZED_IMAGE);

    expect(container.getElementsByClassName(MODAL_MOCK_CLASS)).toHaveLength(0);
  });

  it('hide modal while loading', async () => {
    const showError = jest.fn();
    const addImageToCart = jest.fn();
    const { container } = renderButton({ addImageToCart, showError });
    const input = container.getElementsByTagName('input').item(0);

    let resolveCallback: (img: NormalizedImage) => void;

    const promise = new Promise<NormalizedImage>(resolve => {
      resolveCallback = resolve;
    });

    useLoadImageMock.mockReturnValue(promise);

    fireEvent.change(input!, { target: { files: [VALID_IMAGE] } });
    await act(async () => {
      fireEvent.click(container.getElementsByClassName(MODAL_MOCK_SET_CLASS).item(0)!);
    });

    expect(container.getElementsByClassName(MODAL_MOCK_CLASS)).toHaveLength(0);
    expect(container.getElementsByClassName('Button2_progress')).toHaveLength(1);

    await act(async () => {
      resolveCallback(NORMALIZED_IMAGE);
    });

    expect(container.getElementsByClassName(MODAL_MOCK_CLASS)).toHaveLength(0);
  });

  it('handle load error', async () => {
    const showError = jest.fn();
    const addImageToCart = jest.fn();
    const { container } = renderButton({ addImageToCart, showError });
    const input = container.getElementsByTagName('input').item(0);

    fireEvent.change(input!, { target: { files: [VALID_IMAGE] } });

    useLoadImageMock.mockReturnValue(Promise.reject(new Error('test_error')));

    await act(async () => {
      fireEvent.click(container.getElementsByClassName(MODAL_MOCK_SET_CLASS).item(0)!);
    });

    expect(showError).toBeCalledTimes(1);
    expect(addImageToCart).toBeCalledTimes(0);
    expect(showError).toBeCalledWith('Ошибка загрузки изображения: test_error');

    expect(container.getElementsByClassName(MODAL_MOCK_CLASS)).toHaveLength(0);
  });
});

function renderButton(actions?: Partial<PictureEditorActions>) {
  return render(
    <ActionsContext.Provider value={{ ...actions } as PictureEditorActions}>
      <ImageUploadButton />
    </ActionsContext.Provider>
  );
}
