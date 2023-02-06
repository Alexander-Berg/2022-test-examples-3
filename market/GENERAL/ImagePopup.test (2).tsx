import React from 'react';
import { render, screen } from '@testing-library/react';

import { ImagePopup } from './ImagePopup';

describe('ImagePopup::', () => {
  it('should render an image popup', () => {
    const imagePopupProps = {
      src:
        'https://images.pexels.com/photos/104827/cat-pet-animal-domestic-104827.jpeg?auto=compress&cs=tinysrgb&h=300',
      onOutsideClick: jest.fn(),
    };

    render(<ImagePopup onClose={jest.fn()} {...imagePopupProps} />);
    const image = screen.getByAltText('');
    expect(image).toBeInTheDocument();
    expect(image.getAttribute('src')).toEqual(imagePopupProps.src);
  });
});
