import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { ImagePopup } from '@yandex-market/mbo-picture-editor';

import { Images } from 'src/tasks/mapping-moderation/components/Images/Images';

describe('Images', () => {
  it('given empty urls it should display nothing', () => {
    const images = shallow(<Images urls={[]} />);
    expect(images.children()).toBeEmpty();
  });

  it('should render image and allow selection of other image', () => {
    const images = shallow(<Images urls={['one.jpg', 'two.jpg']} />);
    expect(images.find('.Images-SelectedImage Image').prop('src')).toEqual('one.jpg');
    expect(images.find('.Images-GalleryImage Image')).toHaveLength(2);

    images.find('.Images-GalleryImage Image').at(1).simulate('click');
    expect(images.find('.Images-SelectedImage Image').prop('src')).toEqual('two.jpg');
  });

  it('should show large image on click', () => {
    const images = mount(<Images urls={['one.jpg', 'two.jpg']} />);

    expect(images.find('h2').findWhere(node => node.text() === 'Увеличенное изображение')).toHaveLength(0);
    images.find('.Images-SelectedImage Image').simulate('click');
    expect(images.find(ImagePopup).find('img').filter({ src: 'one.jpg' })).toHaveLength(1);
  });
});
