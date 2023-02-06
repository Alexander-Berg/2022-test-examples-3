import React from 'react';
import { render } from '@testing-library/react';

import { ImagePopup } from './ImagePopup';

describe('ImagePopup', () => {
  it('works', () => {
    const imagePopupProps = {
      src: 'testImageUrlMock',
      onOutsideClick: jest.fn(),
    };

    render(<ImagePopup onClose={jest.fn()} {...imagePopupProps} />);
  });
});
