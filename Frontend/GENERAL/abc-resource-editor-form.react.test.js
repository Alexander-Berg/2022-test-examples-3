import React from 'react';
import { mount } from 'enzyme';

import AbcResourceEditorForm from 'b:abc-resource-editor-form';

describe('AbcResourceEditorForm', () => {
    it('Should render resource editor form', () => {
        const wrapper = mount(
            <AbcResourceEditorForm
                resourceForm={{
                    form_url: 'form_url'
                }}
                height={0}
                iframeId="id1"
                loading={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource editor form with error', () => {
        const error = new Error();

        error.data = {
            message: {
                ru: 'Текст ru message',
                en: 'Текст en message'
            }
        };

        const wrapper = mount(
            <AbcResourceEditorForm
                resourceForm={{
                    form_url: 'form_url'
                }}
                height={0}
                iframeId="id2"
                error={error}
                loading={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource editor form with loading', () => {
        const wrapper = mount(
            <AbcResourceEditorForm
                resourceForm={{
                    form_url: 'form_url'
                }}
                height={0}
                iframeId="id111"
                loading
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render empty resource form', () => {
        const wrapper = mount(
            <AbcResourceEditorForm
                resourceForm={null}
                height={0}
                iframeId="id111"
                loading={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
