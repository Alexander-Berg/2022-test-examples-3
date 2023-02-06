import React from 'react';
import { shallow } from 'enzyme';

import AbcResourceView__Field from 'b:abc-resource-view e:field m:type=consumer-tags';

describe('AbcResourceView__Field', () => {
    it('Should render empty field consumer-tags', () => {
        const wrapper = shallow(
            <AbcResourceView__Field
                type="consumer-tags"
                actions={[]}
                tags={[]}
                popupOpen={false}
                onConsumerTagsEditClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
                onResourceTagsEditSubmi={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render field consumer-tags', () => {
        const wrapper = shallow(
            <AbcResourceView__Field
                type="consumer-tags"
                actions={['edit']}
                tags={[
                    {
                        slug: 'foo',
                        name: {
                            ru: 'BAR'
                        }
                    }
                ]}
                popupOpen={false}
                onConsumerTagsEditClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
                onResourceTagsEditSubmi={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
