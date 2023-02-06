import React from 'react';
import { shallow } from 'enzyme';
import __AttributeValueContent from 'b:abc-resource-view e:attribute-value-content';

describe('__AttributeValueContent', () => {
    it('Should render json attribute', () => {
        const wrapper = shallow(
            <__AttributeValueContent
                attribute={{ val: { foo: 'bar' } }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render text attribute', () => {
        const wrapper = shallow(
            <__AttributeValueContent
                attribute={{ val: 'foo' }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render empty text attribute', () => {
        const wrapper = shallow(
            <__AttributeValueContent
                attribute={{ val: '' }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render link attribute', () => {
        const wrapper = shallow(
            <__AttributeValueContent
                attribute={{
                    type: 'link',
                    val: 'test',
                    url: '/foobar'
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should support old value', () => {
        const wrapper = shallow(
            <__AttributeValueContent
                attribute={{ val: 'foo', old: 'bar' }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
