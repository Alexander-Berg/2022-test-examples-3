import '../../noscript';

import { paymentPay } from '../../../../components/redux/store/actions/payment';
import { STATE } from '../../../../components/redux/store/actions/dialogs';
import { OPEN_DIALOG, CLOSE_DIALOG } from '../../../../components/redux/store/actions/types';

describe('payment actions', () => {
    describe('paymentPay', () => {
        const originalNsModelGet = ns.Model.get;
        ns.page.current.params = ns.page.current.params || {};
        const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch) : arg);
        const getState = () => ({
            environment: {
                agent: {}
            },
            dialogs: {
                payment: {
                    state: STATE.OPENED
                }
            },
            page: {
                originalNSParams: {
                    from: 'nexsd'
                }
            }
        });
        const getData = jest.fn();
        const always = jest.fn((fn) => fn({
            valueOf: () => ({
                getData
            })
        }));
        const pushPriority = jest.fn(() => ({ always }));

        beforeEach(() => {
            ns.Model.get = (model) => {
                if (model === 'queueRequest') {
                    return {
                        pushPriority
                    };
                }
            };
        });

        afterEach(() => {
            jest.clearAllMocks();
            ns.Model.get = originalNsModelGet;
        });

        it('should call do-order-payment', () => {
            paymentPay('offer-id')(dispatch, getState);
            expect(pushPriority).toBeCalledWith({
                id: 'do-order-payment',
                params: {
                    from: 'nexsd',
                    pid: 'offer-id',
                    method: 'bankcard',
                    repeat: true,
                    template: 'desktop/form'
                }
            });
        });

        it('should close payment dialog and open paymentIncorrectTariff dialog if got 243 error code', () => {
            getData.mockImplementation(() => ({
                error: {
                    body: {
                        code: 243
                    }
                }
            }));
            paymentPay('offer-id')(dispatch, getState);

            const closeDialogActions = dispatch.mock.calls.filter(([action]) => action.type === CLOSE_DIALOG);
            expect(closeDialogActions.length).toEqual(1);
            expect(closeDialogActions[0]).toEqual([{
                type: CLOSE_DIALOG,
                payload: {
                    dialog: 'payment'
                }
            }]);

            const openDialogActions = dispatch.mock.calls.filter(([action]) => action.type === OPEN_DIALOG);
            expect(openDialogActions.length).toEqual(1);
            expect(openDialogActions[0]).toEqual([{
                type: OPEN_DIALOG,
                payload: {
                    dialog: 'paymentIncorrectTariff'
                }
            }]);
        });
    });
});
