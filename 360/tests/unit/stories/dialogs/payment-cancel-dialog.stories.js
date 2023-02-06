import '../../noscript';
import { storiesOf, specs, describe, it, mount } from '../../.storybook/facade';
import React from 'react';
import { PaymentCancelDialog } from '../../../../components/redux/components/dialogs/payment-cancel';
import '../../../../components/redux/components/dialogs/payment-cancel/index.styl';

import { count } from 'helpers/metrika';

jest.mock('helpers/metrika');
jest.mock('helpers/operation', () => ({}));
jest.mock('helpers/page', () => ({
    go: jest.fn()
}));

const defaultProps = {
    visible: true,
    package: {
        expires: 1530121297,
        removes: 1530121297,
        free: false,
        ctime: 1524850901,
        size: 107374182400,
        name: '100gb_1m_2015',
        state: null,
        sid: 'ca348dfbff51b176b7c12ed8a0408487',
        subscription: true,
        order: '1277823287',
        title: 'Подписка 100 ГБ на месяц',
        id: 60
    },
    closeDialog: jest.fn(),
    paymentCancel: jest.fn()
};
const getProps = (props = {}) => Object.assign({}, defaultProps, props);
const getComponent = (props) => (<PaymentCancelDialog {...getProps(props)} />);

export default storiesOf('PaymentCancelDialog', module)
    .add('обычное состояние', ({ kind, story }) => {
        specs(() => describe(kind, () => {
            let props;
            let component;
            let wrapper;

            beforeEach(() => {
                props = getProps();
                component = getComponent(props);
                wrapper = mount(component);
            });

            it(story, () => {
                expect(document.body).toMatchSnapshot();
            });

            it('открытие/закрытие', () => {
                wrapper.setProps({ visible: false });
                wrapper.setProps({ visible: true });
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'popup cancel sub', 'show');
            });

            it('_onDialogSubmit', () => {
                wrapper.instance()._onDialogSubmit();
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'popup cancel sub', 'yes');
                expect(props.closeDialog).toHaveBeenLastCalledWith('paymentCancel');
                expect(props.paymentCancel).toHaveBeenLastCalledWith(defaultProps.package);
            });

            it('_onDialogCancel', () => {
                wrapper.instance()._onDialogCancel();
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'popup cancel sub', 'no');
                expect(props.closeDialog).toHaveBeenLastCalledWith('paymentCancel');
            });

            it('_onDialogClose', () => {
                wrapper.instance()._onDialogClose();
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'popup cancel sub', 'close');
                expect(props.closeDialog).toHaveBeenLastCalledWith('paymentCancel');
            });

            afterEach(() => {
                wrapper.unmount();
            });
        }));

        return getComponent(getProps());
    });
