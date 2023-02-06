import '../../noscript';
import { storiesOf, specs, describe, it, mount } from '../../.storybook/facade';
import React from 'react';
import { PaymentDialog } from '../../../../components/redux/components/dialogs/payment-dialog';
import { Button } from '@ps-int/ufo-rocks/lib/components/lego-components/Button';
import '../../../../components/redux/components/dialogs/payment-dialog.styl';

import { count } from 'helpers/metrika';

jest.mock('helpers/metrika');
jest.mock('helpers/operation', () => ({}));

const defaultProps = {
    visible: true,
    paymentUrl: 'https://ya.ru',
    orderId: '1',
    wait: false,
    openDialog: jest.fn(),
    closeDialog: jest.fn(),
    setOrderProcessing: jest.fn(),
    termsOfUseUrl: 'https://yandex.ru/legal/disk_termsofuse/'
};
const getProps = (props = {}) => Object.assign({}, defaultProps, props);
const getComponent = (props) => (<PaymentDialog {...getProps(props)} />);

export default storiesOf('PaymentDialog', module)
    .add('обычное состояние', ({ kind, story }) => {
        specs(() => describe(kind, () => {
            let props;
            let component;
            let wrapper;

            beforeEach(() => {
                count.mockClear();
                defaultProps.openDialog.mockClear();
                defaultProps.closeDialog.mockClear();
                defaultProps.setOrderProcessing.mockClear();
                props = getProps();
                component = getComponent(props);
                wrapper = mount(component);
            });

            it(story, () => {
                expect(document.body).toMatchSnapshot();
            });

            it('открытие', () => {
                wrapper.setProps({ visible: false });
                wrapper.setProps({ visible: true });
                expect(count).toHaveBeenCalledWith('interface elements', 'upgrade page', 'popup billing', 'show');
            });

            it('закрытие', () => {
                wrapper.setProps({ visible: false });
                expect(count).toHaveBeenCalledWith('interface elements', 'upgrade page', 'popup billing', 'close');

                wrapper.setProps({ visible: true });
                expect(count).toHaveBeenCalledWith('interface elements', 'upgrade page', 'popup billing', 'show');

                const closeButton = wrapper
                    .find(Button)
                    .findWhere((button) => button.prop('className').toString() === 'dialog__close')
                    .first();

                closeButton.simulate('click');
                expect(props.closeDialog).toHaveBeenCalledWith('payment');
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'popup billing', 'close');

                wrapper.instance()._onDialogClose({}, 'outside');
                expect(props.closeDialog).toHaveBeenCalledTimes(1);
            });

            describe('обработка postMessage от iframe оплаты', () => {
                it('невалидный JSON', () => {
                    const onMessage = wrapper.instance()._onMessage;
                    onMessage({});
                    onMessage({ data: 123 });
                    onMessage({ data: '123' });
                    onMessage({ data: '{"source":"invalid json"}' });
                });

                it('неправильный source', () => {
                    const onMessage = wrapper.instance()._onMessage;
                    onMessage({ data: '{"source": "yo"' });
                    onMessage({ data: '{"source": ""' });
                    onMessage({ data: '{"source": null' });
                    onMessage({ data: '{"source": false' });
                });

                it('перехватываем редирект в диск', () => {
                    const onMessage = wrapper.instance()._onMessage;
                    onMessage({ data: '{"type":"payment","source":"YandexTrustPaymentForm"}' });
                    expect(props.closeDialog).toHaveBeenLastCalledWith('payment', { paymentUrl: '' });
                });

                it('перехватываем таймаут оплаты (он тоже пытается средиректить в Диск)', () => {
                    const onMessage = wrapper.instance()._onMessage;
                    onMessage({ data: '{"type":"payment-timeout","source":"YandexTrustPaymentForm"}' });
                    expect(props.closeDialog).toHaveBeenLastCalledWith('payment', { paymentUrl: '' });
                });

                it('обработка успешного статуса операции', () => {
                    const onMessage = wrapper.instance()._onMessage;
                    onMessage({ data: '{"type":"payment-status","source":"YandexTrustPaymentForm","data":{"value":"success"}}' });
                    expect(props.setOrderProcessing).toHaveBeenCalledWith(defaultProps.orderId);
                    expect(count).toHaveBeenCalledWith('interface elements', 'upgrade page', 'popup billing', 'success');
                });

                it('обработка статуса операции wait_for_notification', () => {
                    const onMessage = wrapper.instance()._onMessage;
                    onMessage({ data: '{"type":"payment-status","source":"YandexTrustPaymentForm","data":{"value":"wait_for_notification"}}' });
                    expect(count).toHaveBeenCalledTimes(1);
                });

                it('обработка статуса операции ready', () => {
                    const onMessage = wrapper.instance()._onMessage;
                    onMessage({ data: '{"type":"payment-status","source":"YandexTrustPaymentForm","data":{"value":"wait_for_notification"}}' });
                    expect(count).toHaveBeenCalledTimes(1);
                });

                it('обработка других статусов операции', () => {
                    const onMessage = wrapper.instance()._onMessage;
                    onMessage({ data: '{"type":"payment-status","source":"YandexTrustPaymentForm","data":{"value":"fail"}}' });
                    expect(count).toHaveBeenCalledWith('interface elements', 'upgrade page', 'popup billing', 'fail');
                });
            });

            afterEach(() => {
                wrapper.unmount();
            });
        }));

        return getComponent(getProps());
    })
    .add('загрузка', ({ kind, story }) => {
        specs(() => describe(kind, () => {
            let props;
            let component;
            let wrapper;

            beforeEach(() => {
                props = getProps({
                    wait: true
                });
                component = getComponent(props);
                wrapper = mount(component);
            });

            it(story, () => {
                expect(document.body).toMatchSnapshot();
            });

            it('загрузка без paymentUrl', () => {
                wrapper.props({
                    paymentUrl: undefined,
                    wait: false
                });
                expect(document.body).toMatchSnapshot();
            });

            afterEach(() => {
                wrapper.unmount();
            });
        }));

        return getComponent(getProps({
            wait: true
        }));
    });
