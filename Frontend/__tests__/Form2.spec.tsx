import * as React from 'react';
import { mount, shallow } from 'enzyme';
import { withForm, IWithFormProps } from '@yandex-turbo/components/withForm/withForm';
import { Form2, Form2Presenter } from '../Form2';

const { prepareToSubmit } = Form2Presenter;

describe('Form2', () => {
    class FakeInput extends React.PureComponent<
        React.InputHTMLAttributes<HTMLInputElement> & IWithFormProps
    > {
        public render() {
            return <input {...this.props} />;
        }
    }
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
    const Wrapped = withForm<
        React.InputHTMLAttributes<HTMLInputElement> & IWithFormProps
    >()(FakeInput);

    describe('События', () => {
        let form;
        let handleChange;
        let handleSubmit;
        let handleError;

        beforeEach(() => {
            handleChange = jest.fn();
            handleSubmit = jest.fn(({ baz }) =>
                baz === 'myValue' ?
                    Promise.resolve() :
                    Promise.reject({
                        code: 300,
                        field: {
                            baz: ['error1', 'error2'],
                        },
                    })
            );
            handleError = jest.fn();
            form = mount(
                <Form2
                    onChange={handleChange}
                    onSubmit={handleSubmit}
                    onError={handleError}
                >
                    <WrappedCheckbox
                        name="foo"
                        value="foo-1"
                        type="checkbox"
                        checked
                    />
                    <WrappedCheckbox
                        name="foo"
                        value="foo-2"
                        type="checkbox"
                        checked
                    />
                    <WrappedCheckbox name="bar" type="checkbox" checked={false} />
                    <Wrapped name="baz" type="text" value="myValue" />
                </Form2>
            );
        });

        it('должен вызываться onSumbit', () => {
            form.find('form').simulate('submit');

            expect(handleSubmit).toBeCalledWith({
                foo: ['foo-1', 'foo-2'],
                baz: 'myValue',
            }, expect.any(Object));
            expect(handleError).not.toBeCalled();
        });

        it('должен выставляться флаг isSubmiting', () => {
            const preventDefault = jest.fn();
            const promise = new Promise(() => false);
            const cb = () => promise;
            const wrapped = shallow(<Form2Presenter onSubmit={cb} />);

            wrapped.find('form').simulate('submit', { preventDefault });
            expect(preventDefault).toBeCalled();
            expect(wrapped.state('isSubmitting')).toBeTruthy();
        });

        it('должен вызваться onChange при ините и изменение формы', () => {
            expect(handleChange).toHaveBeenCalledTimes(4);
            form.find('input[name="baz"]').simulate('change', {
                target: { name: 'baz', value: 'ops!' },
            });
            form.find('input[name="bar"]').simulate('change', {
                target: { name: 'bar', checked: true },
            });

            expect(handleChange).toHaveBeenCalledTimes(6);
        });
    });

    describe('prepareToSubmit', () => {
        describe('При пустой форме', () => {
            it('корректно возвращает пустой объект', () => {
                expect(prepareToSubmit({})).toEqual({});
            });
        });

        describe('Текстовые значения', () => {
            it('корректно обрабатывает с простым кейсом', () => {
                expect(
                    prepareToSubmit({
                        foo: { value: 'foo-value' },
                        bar: { value: 'bar-value' },
                    })
                ).toEqual({
                    foo: 'foo-value',
                    bar: 'bar-value',
                });
            });

            it('исключает пустые', () => {
                expect(
                    prepareToSubmit({
                        foo: { value: '' },
                        bar: {},
                        control: { value: 'control' },
                        empty: { value: '' },
                    })
                ).toEqual({ control: 'control' });
            });
        });

        describe('Чекбоксы', () => {
            it('корректно определяет значение для поля', () => {
                expect(
                    prepareToSubmit({
                        foo: { value: { foo: true } },
                        foo2: { value: {} },
                        bar: { value: { on: true } },
                        bar2: { value: {} },
                    })
                ).toEqual({
                    foo: ['foo'],
                    bar: ['on'],
                });
            });
        });

        describe('Массив значений', () => {
            it('собирает одинаковые имена в массив', () => {
                expect(
                    prepareToSubmit({
                        foo: { value: { 'foo-1': true, 'foo-2': true, 'foo-checkbox': true, on: true } },
                        bar: { value: { single: true, double: false } },
                    })
                ).toEqual({
                    foo: ['foo-1', 'foo-2', 'foo-checkbox', 'on'],
                    bar: ['single'],
                });
            });
        });
    });
});
