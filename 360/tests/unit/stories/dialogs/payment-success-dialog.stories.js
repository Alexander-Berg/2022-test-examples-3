import '../../noscript';
import { storiesOf, specs, describe, it, mount } from '../../.storybook/facade';
import React from 'react';
import { PaymentSuccessDialog } from '../../../../components/redux/components/dialogs/payment-success';
import '../../../../components/redux/components/dialogs/payment-success/index.styl';

import { count } from 'helpers/metrika';

jest.mock('helpers/metrika');
jest.mock('helpers/operation', () => ({}));
jest.mock('helpers/page', () => ({
    go: jest.fn()
}));

const defaultProps = {
    visible: true,
    spaceLimit: 21838298088448,
    offerSize: 107374182400,
    closeDialog: jest.fn()
};
const getProps = (props = {}) => Object.assign({}, defaultProps, props);
const getComponent = (props) => (<PaymentSuccessDialog {...getProps(props)} />);

export default storiesOf('PaymentSuccessDialog', module)
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
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'popup success pay', 'show');
            });

            it('_onDialogSubmit', () => {
                wrapper.instance()._onDialogSubmit();
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'popup success pay', 'buy more');
                expect(props.closeDialog).toHaveBeenLastCalledWith('paymentSuccess');
            });

            it('_onDialogCancel', () => {
                wrapper.instance()._onDialogCancel();
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'popup success pay', 'go to Disk');
                expect(props.closeDialog).toHaveBeenLastCalledWith('paymentSuccess');
            });

            it('_onDialogClose', () => {
                wrapper.instance()._onDialogClose();
                expect(count).toHaveBeenLastCalledWith('interface elements', 'upgrade page', 'popup success pay', 'close');
                expect(props.closeDialog).toHaveBeenLastCalledWith('paymentSuccess');
            });

            afterEach(() => {
                wrapper.unmount();
            });
        }));

        return getComponent(getProps());
    });
