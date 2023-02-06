import React from 'react';
import { shallow } from 'enzyme';

import AbcResourceView__Field from 'b:abc-resource-view e:field m:type=supplier-tags';

describe('AbcResourceView__Field', () => {
    it('Should render empty field supplier-tags', () => {
        const wrapper = shallow(
            <AbcResourceView__Field
                type="supplier-tags"
                tags={[]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render field supplier-tags', () => {
        const wrapper = shallow(
            <AbcResourceView__Field
                type="supplier-tags"
                tags={[
                    {
                        slug: 'foo',
                        name: {
                            ru: 'BAR'
                        }
                    }
                ]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
