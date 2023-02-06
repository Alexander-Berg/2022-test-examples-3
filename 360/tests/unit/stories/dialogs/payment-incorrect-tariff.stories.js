import '../../noscript';
import { storiesOf, specs, describe, it, mount } from '../../.storybook/facade';
import React from 'react';
import { selector, PaymentIncorrectTariffDialog } from '../../../../components/redux/components/dialogs/payment-incorrect-tariff';
import { STATE } from '../../../../components/redux/store/actions/dialogs';

const getComponent = (props) => (<PaymentIncorrectTariffDialog {...props} />);
const originalLocationReload = location.reload;

export default storiesOf('PaymentIncorrectTariff', module)
    .add('селектор', ({ kind }) => {
        specs(() => describe(kind, () => {
            it('should return { visible: false } if no dialog info', () => {
                expect(selector({ environment: { agent: { OSFamily: 'MacOS' } } }, {})).toEqual({ visible: false });
            });
            it('should return { visible: false, isIosSafari: false } if no dialog is CLOSED on MacOS', () => {
                expect(selector({
                    environment: {
                        agent: {
                            OSFamily: 'MacOS'
                        }
                    }
                }, {
                    dialog: {
                        state: STATE.CLOSED
                    }
                })).toEqual({ visible: false, isIosSafari: false });
            });
            it('should return { visible: true, isIosSafari: false } if no dialog is OPENED on MacOS', () => {
                expect(selector({
                    environment: {
                        agent: {
                            OSFamily: 'MacOS'
                        }
                    }
                }, {
                    dialog: {
                        state: STATE.OPENED,
                        isIosSafari: false
                    }
                })).toEqual({ visible: true, isIosSafari: false });
            });
            it('should return { visible: true, isIosSafari: false } if no dialog is OPENED on iOS', () => {
                expect(selector({
                    environment: {
                        agent: {
                            OSFamily: 'iOS',
                            BrowserBase: 'Safari'
                        }
                    }
                }, {
                    dialog: {
                        state: STATE.OPENED,
                        isIosSafari: false
                    }
                })).toEqual({ visible: true, isIosSafari: true });
            });
        }));
    })
    .add('PaymentIncorrectTariffDialog', ({ kind, story }) => {
        specs(() => describe(kind, () => {
            let component;
            let wrapper;

            beforeEach(() => {
                component = getComponent({ visible: true });
                wrapper = mount(component);
                global.location.reload = jest.fn();
            });

            it(story, () => {
                expect(document.body).toMatchSnapshot();
            });

            it('_onDialogCancel', () => {
                wrapper.instance()._onDialogCancel();
                expect(global.location.reload).toBeCalled();
            });

            afterEach(() => {
                wrapper.unmount();
                global.location.reload = originalLocationReload;
            });
        }));
    });
