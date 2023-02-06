import * as React from 'react';
import { mount } from 'enzyme';

import { Form2 } from '@yandex-turbo/components/Form2/Form2';
import { withForm, IWithFormProps } from '../withForm';

// Тест будет полностью переделан с этой задачей TURBOUI-894
describe('withForm', () => {
    // Компонент для принятия пропсов.
    class FakeInput extends React.PureComponent<
        React.InputHTMLAttributes<HTMLInputElement> & IWithFormProps
    > {
        public render() {
            return <input {...this.props} />;
        }
    }

    const Wrapped = withForm<
        React.InputHTMLAttributes<HTMLInputElement> & IWithFormProps
    >()(FakeInput);

    const WrappedCheckbox = withForm<
        React.InputHTMLAttributes<HTMLInputElement> & IWithFormProps
        >({
            type: 'checkbox',

            handleChange: ({ value, checked, required }) => {
                const newControlValue = {};

                if (value) {
                    newControlValue[`${value}`] = Boolean(checked);
                }

                return {
                    required,
                    value: newControlValue,
                };
            },
        })(FakeInput);

    describe('с текстовым полем', () => {
        let wrapper;

        beforeEach(() => {
            wrapper = mount(
                <Form2>
                    <Wrapped name="name" type="text" value="inited-value" />
                </Form2>
            );
        });

        it('должен рендерится без ошибок', () => {
            expect(wrapper.find(Wrapped).length).toBe(1);
            expect(wrapper.find(FakeInput).length).toBe(1);
        });

        it('должен прокидывать пропсы', () => {
            expect(wrapper.find(FakeInput).props()).toEqual({
                name: 'name',
                type: 'text',
                value: 'inited-value',
                onChange: expect.any(Function),
                onBlur: expect.any(Function),
                onFocus: expect.any(Function),
            });
        });

        it('должен обрабатывать изменение занчения', () => {
            const input = wrapper.find('input');
            const fake = wrapper.find(FakeInput);

            input.simulate('change', {
                target: { name: 'name', value: 'Changed' },
            });

            expect(fake.props()).toEqual({
                name: 'name',
                type: 'text',
                value: 'inited-value',
                onChange: expect.any(Function),
                onBlur: expect.any(Function),
                onFocus: expect.any(Function),
            });
        });
    });

    describe('с чекбоксом', () => {
        let wrapper;

        beforeEach(() => {
            wrapper = mount(
                <Form2>
                    <WrappedCheckbox name="name" type="checkbox" checked={false} />
                </Form2>
            );
        });

        it('должен рендерится без ошибок', () => {
            expect(wrapper.find(WrappedCheckbox).length).toBe(1);
            expect(wrapper.find(FakeInput).length).toBe(1);
        });

        it('должен прокидывать пропсы', () => {
            expect(wrapper.find(FakeInput).props()).toEqual({
                name: 'name',
                type: 'checkbox',
                checked: false,
                errors: undefined,
                required: undefined,
                value: {},
                onChange: expect.any(Function),
                onBlur: expect.any(Function),
                onFocus: expect.any(Function),
            });
        });

        it.skip('должен обрабатывать изменение занчения', () => { // eslint-disable-line
            const input = wrapper.find('input');

            input.simulate('change', {
                target: { name: 'name', checked: true },
            });

            expect(wrapper.find(FakeInput).props()).toEqual({
                name: 'name',
                type: 'checkbox',
                checked: true,
                onChange: expect.any(Function),
                onBlur: expect.any(Function),
                onFocus: expect.any(Function),
            });
        });
    });

    it.skip('должен сохранять поля из event', () => { // eslint-disable-line
        const wrapper = mount(
            <Form2>
                <Wrapped name="name" value="" type="checkbox" />
            </Form2>
        );

        expect(wrapper.find(FakeInput).props()).toEqual({
            name: 'name',
            value: '',
            onChange: expect.any(Function),
            onBlur: expect.any(Function),
            onFocus: expect.any(Function),
        });

        wrapper.find('input').simulate('change', {
            target: {
                name: 'new-name',
                checked: 'checked',
                value: 'new-value',
                foo: 'should-skiped',
            },
        });

        expect(wrapper.find(FakeInput).props()).toEqual({
            name: 'new-name',
            checked: 'checked',
            value: 'new-value',
            onChange: expect.any(Function),
            onBlur: expect.any(Function),
            onFocus: expect.any(Function),
        });
    });

    it.skip('дожен работать с одинаковыми props.name', () => { // eslint-disable-line
        const wrapper = mount(
            <Form2>
                <Wrapped name="name" type="checkbox" checked={false} />
                <Wrapped name="name" type="checkbox" checked={false} />
            </Form2>
        );

        expect(wrapper.find(FakeInput).get(0).props).toEqual(
            expect.objectContaining({
                checked: false,
                name: 'name',
            })
        );
        expect(wrapper.find(FakeInput).get(1).props).toEqual(
            expect.objectContaining({
                checked: false,
                name: 'name',
            })
        );
        wrapper
            .find(FakeInput)
            .first()
            .simulate('change', { target: { checked: true } });
        expect(wrapper.find(FakeInput).get(0).props).toEqual(
            expect.objectContaining({
                checked: true,
                name: 'name',
            })
        );
        expect(wrapper.find(FakeInput).get(1).props).toEqual(
            expect.objectContaining({
                checked: false,
                name: 'name',
            })
        );
    });

    it('должен переопределять кастомный handleChange', () => {
        const WrappedComponent = withForm<
            React.InputHTMLAttributes<HTMLInputElement> & IWithFormProps
        >({
            handleChange: props => ({
                ...props,
                checked: props.value !== undefined,
            }),
        })(FakeInput);

        const wrapper = mount(
            <Form2>
                <WrappedComponent name="foo" type="" />
                <WrappedComponent name="bar" value="omg!" type="" />
            </Form2>
        );

        expect(wrapper.find(FakeInput).get(0).props).toEqual(
            expect.objectContaining({
                checked: false,
            })
        );
        expect(wrapper.find(FakeInput).get(1).props).toEqual(
            expect.objectContaining({
                checked: true,
            })
        );
    });
});
