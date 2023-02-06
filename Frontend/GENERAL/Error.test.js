import React from 'react';
import { mount } from 'enzyme';

import { Error } from './Error';

describe('Should render', () => {
    it('error 1', () => {
        const wrapper = mount(
            <Error
                data={{
                    message: {
                        ru: 'Текст ru message',
                        en: 'Текст en message',
                    },
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 2', () => {
        const wrapper = mount(
            <Error
                data={{ detail: 'Текст detail' }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 3', () => {
        const wrapper = mount(
            <Error
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
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 4', () => {
        const wrapper = mount(
            <Error
                data={{
                    detail: 'Текст detail',
                    title: {
                        ru: 'Текст ru title',
                        en: 'Текст en title',
                    },
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 5', () => {
        const wrapper = mount(
            <Error
                data={{
                    code: 'validation_error',
                    detail: 'Текст detail',
                    extra: {
                        __all__: ['Такое себе'],
                        foo: ['Раз', 'Два'],
                    },
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 6', () => {
        const wrapper = mount(
            <Error
                data={{
                    code: 'validation_error',
                    detail: 'Текст detail',
                    extra: {
                        foo: ['Раз', 'Два'],
                    },
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 7', () => {
        const wrapper = mount(
            <Error
                data={{
                    code: 'validation_error',
                    detail: 'Текст detail',
                    extra: {
                        foo: {
                            ru: ['Раз', 'Два'],
                        },
                    },
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 8', () => {
        const wrapper = mount(
            <Error
                data={{
                    message: 'None',
                    code: 'YAWF_VALIDATION_ERROR',
                    params: {
                        contacts: [
                            {
                                non_field_errors: ['Неверное значение контакта 0'],
                            },
                            {},
                            {
                                non_field_errors: ['Неверное значение контакта 2'],
                            },
                        ],
                    },
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 9', () => {
        const wrapper = mount(
            <Error
                data={{
                    message: 'String message',
                    title: 'String title',
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 10', () => {
        const wrapper = mount(
            <Error
                data={{
                    extra: {
                        path: [
                            'Узел в дереве ролей не найден',
                        ],
                    },
                    message: 'Invalid data sent',
                    code: 'error',
                    detail: 'Произошла ошибка сервера.',
                    title: 'Ошибка IDM',
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 11', () => {
        const wrapper = mount(
            <Error
                data={{
                    message: '',
                    code: 'integration-error',
                    detail: 'IDM did not fulfil the request.',
                    extra: {
                        raw: '{"error_code": "READONLY_STATE", "message": "IDM is in a read-only state, cannot handle write intending request"}',
                        message: 'IDM is in a read-only state, cannot handle write intending request',
                        errors: null,
                    },
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 12: dispenser error with data object', () => {
        const wrapper = mount(
            <Error
                data={{
                    message: '',
                    code: 'integration-error',
                    detail: 'Dispenser did not fulfil the request.',
                    description: 'Dispenser is in a read-only state, cannot handle write intending request',
                }}
                type="dispenser"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 13: dispenser error with string data', () => {
        const wrapper = mount(
            <Error
                data="Dispenser is in a read-only state, cannot handle write intending request"
                type="dispenser"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('error 14', () => {
        const wrapper = mount(
            <Error
                data={{
                    message: '',
                    code: 'integration-error',
                    detail: 'IDM did not fulfil the request.',
                    description: 'IDM is in a read-only state, cannot handle write intending request',
                }}
                type="dispenser"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
