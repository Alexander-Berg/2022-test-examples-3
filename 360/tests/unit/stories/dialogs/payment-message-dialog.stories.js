import '../../noscript';
import { storiesOf, specs, describe, it, mount } from '../../.storybook/facade';
import React from 'react';
import { PaymentMessageDialog } from '../../../../components/redux/components/dialogs/payment-message';
import '../../../../components/redux/components/dialogs/payment-message/index.styl';

jest.mock('helpers/operation', () => ({}));

const defaultProps = {
    visible: true,
    title: 'Заголовок',
    content: 'И немного текста',
    closeDialog: jest.fn()
};
const getProps = (props = {}) => Object.assign({}, defaultProps, props);
const getComponent = (props) => (<PaymentMessageDialog {...getProps(props)} />);

export default storiesOf('PaymentMessageDialog', module)
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

            it('_onDialogCancel', () => {
                wrapper.instance()._onDialogCancel();
                expect(props.closeDialog).toHaveBeenLastCalledWith('paymentMessage');
            });

            afterEach(() => {
                wrapper.unmount();
            });
        }));

        return getComponent(getProps());
    });
