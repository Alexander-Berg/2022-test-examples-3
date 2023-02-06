import React from 'react';
import { shallow } from 'enzyme';

import Message from './Message';

describe('Should render ', () => {
    it('default message', () => {
        const wrapper = shallow(
            <Message>
                message
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('default message with title', () => {
        const wrapper = shallow(
            <Message>
                <Message.Title>title</Message.Title>
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error message with className', () => {
        const wrapper = shallow(
            <Message type="error" className="Block-Element">
                message
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error message', () => {
        const wrapper = shallow(
            <Message type="error">
                message
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('message with Title', () => {
        const wrapper = shallow(
            <Message type="error" className="Block-Element">
                <Message.Title>
                    Title
                </Message.Title>
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error message with Error', () => {
        const wrapper = shallow(
            <Message type="error" className="Block-Element">
                <Message.Error
                    data={{
                        message: {
                            ru: 'Текст ru message',
                            en: 'Текст en message',
                        },
                        title: {
                            ru: 'Текст ru title',
                            en: 'Текст en title',
                        },
                    }}
                />
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error message with description', () => {
        const wrapper = shallow(
            <Message type="error" className="Block-Element">
                <Message.Error
                    data={{
                        title: {
                            ru: 'Текст ru title',
                            en: 'Текст en title',
                        },
                        description: 'Текст description',
                    }}
                    type="dispenser"
                />
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error message with with no description', () => {
        const wrapper = shallow(
            <Message type="error" className="Block-Element">
                <Message.Error
                    data={{
                        title: {
                            ru: 'Текст ru title',
                            en: 'Текст en title',
                        },
                        description: '',
                    }}
                    type="dispenser"
                />
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('info message with new view', () => {
        const wrapper = shallow(
            <Message type="info" view="new">
                message
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error message with new view', () => {
        const wrapper = shallow(
            <Message type="error" view="new">
                message
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('success message with new view', () => {
        const wrapper = shallow(
            <Message type="success" view="new">
                message
            </Message>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
