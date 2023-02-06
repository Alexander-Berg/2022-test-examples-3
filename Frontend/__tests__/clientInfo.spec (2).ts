import { receiveClientInfo, clientInfoReducer } from '../clientInfo';

const chatId = '0/4/fe56b797-d995-4ab7-8002-911c63130a39';

const realDateNow = Date.now;
const mockTimestamp = 1111111111111;

const clientCrmInfoKey1 = 'info-1';
const clientCrmInfoKey2 = 'info-2';
const clientCrmInfoKey3 = 'info-3';

const clientCrmInfo1 = {
    key: clientCrmInfoKey1,
    label: 'INFO1',
    editable: true,
    value: 'value',
    type: 'string',
};
const clientCrmInfo2 = {
    key: clientCrmInfoKey2,
    label: 'INFO2',
    editable: false,
    value: 'value',
    type: 'phone',
};
const clientCrmInfo3 = {
    key: clientCrmInfoKey3,
    label: 'INFO3',
    editable: true,
    value: 'value',
    type: 'email',
};

const crmKey1 = 'key-crm-1';
const crmKey2 = 'key-crm-2';

const clientCrm1 = {
    crm: 'crm-1',
    key: crmKey1,
    label: 'CRM1',
    version: 0,
    fields: [clientCrmInfo1, clientCrmInfo2],
};
const clientCrm2 = {
    crm: 'crm-2',
    key: crmKey2,
    label: 'CRM1',
    version: 0,
    fields: [clientCrmInfo3],
};

beforeEach(() => {
    Date.now = jest.fn(() => mockTimestamp);
});

afterEach(() => {
    Date.now = realDateNow;
});

describe('ClientInfo reducer', () => {
    describe('receiveQueue', () => {
        it('sets clientInfo for chatId', () => {
            const initialState = {
                byChatId: {},
                lastSync: {},
            };
            const newState = clientInfoReducer(
                initialState,
                receiveClientInfo(chatId, [clientCrm1, clientCrm2]),
            );

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                byChatId: {
                    [chatId]: {
                        clientInfo: {
                            [crmKey1]: clientCrm1,
                            [crmKey2]: clientCrm2,
                        },
                        crmOrder: [crmKey1, crmKey2],
                        infoItemsByFieldKey: {
                            [clientCrmInfoKey1]: {
                                crmKey: crmKey1,
                                field: clientCrmInfo1,
                            },
                            [clientCrmInfoKey2]: {
                                crmKey: crmKey1,
                                field: clientCrmInfo2,
                            },
                            [clientCrmInfoKey3]: {
                                crmKey: crmKey2,
                                field: clientCrmInfo3,
                            },
                        },
                    },
                },
                lastSync: {
                    [chatId]: mockTimestamp,
                },
            });
        });
    });
});
