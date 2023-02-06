import {
    DieHardControlName,
    DieHardCardType,
    DieHardCardSberStatus,
    DieHardOutgoingMessageType,
    DieHardOutgoingMessageFrameReady,
    DieHardOutgoingMessageSelectCard,
    DieHardOutgoingMessageFormStateChanged,
} from '../DieHard';
import {
    DieHardListenerEventName,
    DieHardListener,
} from '.';

describe('DieHardListener', () => {
    test(DieHardListenerEventName.Ready, () => {
        const dhListener = new DieHardListener();

        const onReady = jest.fn(() => {});

        const message: DieHardOutgoingMessageFrameReady = {
            source: 'YandexPciDssPaymentForm',
            type: DieHardOutgoingMessageType.FrameReady,
            data: {
                cvvStatus: {
                    'card-x3232': true,
                    'card-x3333': false,
                },
            },
        };

        dhListener.on(DieHardListenerEventName.Ready, onReady);
        dhListener.onMessage(message);

        expect(onReady.mock.calls.length).toBe(1);
    });

    test(DieHardListenerEventName.MethodSelected, () => {
        const dhListener = new DieHardListener();

        const onMethodSelected = jest.fn((_methodId, _valid) => {});

        const message1: DieHardOutgoingMessageSelectCard = {
            source: 'YandexPciDssPaymentForm',
            type: DieHardOutgoingMessageType.SelectCard,
            data: {
                action: DieHardOutgoingMessageType.SelectCard,
                canSubmit: true,
                cardId: 'card-x3232',
            },
        };
        const message2: DieHardOutgoingMessageSelectCard = {
            source: 'YandexPciDssPaymentForm',
            type: DieHardOutgoingMessageType.SelectCard,
            data: {
                action: DieHardOutgoingMessageType.SelectCard,
                canSubmit: false,
                cardId: 'card-x3333',
            },
        };

        dhListener.on(DieHardListenerEventName.MethodSelected, onMethodSelected);
        dhListener.onMessage(message1);
        dhListener.onMessage(message2);

        expect(onMethodSelected.mock.calls[0]).toEqual(['card-x3232', true]);
        expect(onMethodSelected.mock.calls[1]).toEqual(['card-x3333', false]);
        expect(onMethodSelected.mock.calls.length).toBe(2);
    });

    test(DieHardListenerEventName.FormChanged, () => {
        const dhListener = new DieHardListener();

        const onFormChanged = jest.fn(_data => {});

        const getControlStateOk = () => ({
            validity: {
                status: 'ok',
            },
            required: true,
            filled: true,
            empty: false,
            completed: true,
            fullFilled: true,
        } as const);
        const getControlStateError = () => ({
            validity: {
                status: 'error',
                errorMessage: 'Invalid field.',
            },
            required: true,
            filled: false,
            empty: false,
            completed: false,
            fullFilled: false,
        } as const);

        const message1: DieHardOutgoingMessageFormStateChanged = {
            source: 'YandexPciDssPaymentForm',
            type: DieHardOutgoingMessageType.FormStateChanged,
            data: {
                action: DieHardOutgoingMessageType.FormStateChanged,
                formState: {
                    [DieHardControlName.Number]: getControlStateOk(),
                    [DieHardControlName.Month]: getControlStateOk(),
                    [DieHardControlName.Year]: getControlStateOk(),
                    [DieHardControlName.Cvv]: getControlStateOk(),
                    [DieHardControlName.Date]: getControlStateOk(),
                    [DieHardControlName.Owner]: getControlStateOk(),
                },
                cardType: null,
                cardBin: null,
                cardSberStatus: DieHardCardSberStatus.Unknown,
                canSubmit: true,
            },
        };
        const message2: DieHardOutgoingMessageFormStateChanged = {
            source: 'YandexPciDssPaymentForm',
            type: DieHardOutgoingMessageType.FormStateChanged,
            data: {
                action: DieHardOutgoingMessageType.FormStateChanged,
                formState: {
                    [DieHardControlName.Number]: getControlStateError(),
                    [DieHardControlName.Month]: getControlStateOk(),
                    [DieHardControlName.Year]: getControlStateOk(),
                    [DieHardControlName.Cvv]: getControlStateError(),
                    [DieHardControlName.Date]: getControlStateOk(),
                    [DieHardControlName.Owner]: getControlStateOk(),
                },
                cardType: DieHardCardType.Mastercard,
                cardBin: '134312',
                cardSberStatus: DieHardCardSberStatus.Sber,
                canSubmit: false,
            },
        };
        const message3: DieHardOutgoingMessageFormStateChanged = {
            source: 'YandexPciDssPaymentForm',
            type: DieHardOutgoingMessageType.FormStateChanged,
            data: {
                action: DieHardOutgoingMessageType.FormStateChanged,
                formState: {
                    [DieHardControlName.Number]: getControlStateError(),
                    [DieHardControlName.Month]: getControlStateOk(),
                    [DieHardControlName.Year]: getControlStateOk(),
                    [DieHardControlName.Date]: getControlStateOk(),
                    [DieHardControlName.Owner]: getControlStateOk(),
                },
                cardType: DieHardCardType.Mastercard,
                cardBin: '134312',
                cardSberStatus: DieHardCardSberStatus.Sber,
                canSubmit: false,
            },
        };
        const message4: DieHardOutgoingMessageFormStateChanged = {
            source: 'YandexPciDssPaymentForm',
            type: DieHardOutgoingMessageType.FormStateChanged,
            data: {
                action: DieHardOutgoingMessageType.FormStateChanged,
                formState: {
                    [DieHardControlName.Number]: getControlStateOk(),
                    [DieHardControlName.Month]: getControlStateOk(),
                    [DieHardControlName.Year]: getControlStateOk(),
                    [DieHardControlName.Date]: getControlStateOk(),
                    [DieHardControlName.Owner]: getControlStateOk(),
                },
                cardType: DieHardCardType.Humocard,
                cardBin: '134312',
                cardSberStatus: DieHardCardSberStatus.Sber,
                canSubmit: true,
            },
        };

        dhListener.on(DieHardListenerEventName.FormChanged, onFormChanged);
        dhListener.onMessage(message1);
        dhListener.onMessage(message2);
        dhListener.onMessage(message3);
        dhListener.onMessage(message4);

        expect(onFormChanged.mock.calls[0]).toEqual([{
            controls: {
                number: { name: DieHardControlName.Number, valid: true },
                month: { name: DieHardControlName.Month, valid: true },
                year: { name: DieHardControlName.Year, valid: true },
                cvv: { name: DieHardControlName.Cvv, valid: true },
            },
            valid: true,
        }]);
        expect(onFormChanged.mock.calls[1]).toEqual([{
            controls: {
                number: { name: DieHardControlName.Number, valid: false },
                month: { name: DieHardControlName.Month, valid: true },
                year: { name: DieHardControlName.Year, valid: true },
                cvv: { name: DieHardControlName.Cvv, valid: false },
            },
            valid: false,
            cardType: DieHardCardType.Mastercard,
            cardBin: '134312',
        }]);
        expect(onFormChanged.mock.calls[2]).toEqual([{
            controls: {
                number: { name: DieHardControlName.Number, valid: false },
                month: { name: DieHardControlName.Month, valid: true },
                year: { name: DieHardControlName.Year, valid: true },
                cvv: { name: DieHardControlName.Cvv, valid: false },
            },
            valid: false,
            cardType: DieHardCardType.Mastercard,
            cardBin: '134312',
        }]);
        expect(onFormChanged.mock.calls[3]).toEqual([{
            controls: {
                number: { name: DieHardControlName.Number, valid: true },
                month: { name: DieHardControlName.Month, valid: true },
                year: { name: DieHardControlName.Year, valid: true },
                cvv: { name: DieHardControlName.Cvv, valid: true },
            },
            valid: true,
            cardType: DieHardCardType.Humocard,
            cardBin: '134312',
        }]);
        expect(onFormChanged.mock.calls.length).toBe(4);
    });
});
