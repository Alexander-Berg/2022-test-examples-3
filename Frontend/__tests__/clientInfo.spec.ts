import { getClientInfoItem, getClientInfoFields, getUpdatedClientInfoArray } from '../clientInfo';
import {
    clientInfoState as clientInfoStateInitial,
    clientInfo,
    clientInfoFields,
    crmOrder,
} from './clientInfo.dataset';
const deepcopy = require('deepcopy');

describe('Selectors', () => {
    describe('ClientInfo', () => {
        let clientInfoState: any;

        beforeEach(() => {
            clientInfoState = deepcopy(clientInfoStateInitial);
        });

        describe('getClientInfoItem', () => {
            it('returns correct clientInfo for existing chatId', () => {
                const actual = getClientInfoItem(clientInfoState as any, '0/4/1');
                expect(actual).toEqual({
                    clientInfo,
                    crmOrder,
                });
            });

            it('returns no clientInfo for existing chatId', () => {
                const actual = getClientInfoItem(clientInfoState as any, 'some-chat-id');
                expect(actual).toBeUndefined();
            });
        });

        describe('getClientInfoFields', () => {
            it('returns empty array if no clientInfo is present by given chatId', () => {
                const actual = getClientInfoFields(clientInfoState as any, 'some-chat-id');
                expect(actual).toEqual([]);
            });
        });

        describe('getUpdatedClientInfoArray', () => {
            it('returns modified array in normal circumstances', () => {
                const newValue = 'new_value';
                const actual = getUpdatedClientInfoArray(clientInfoState as any, '0/4/1', 'defaults', {
                    ...clientInfoFields[0],
                    value: newValue,
                });
                expect(actual).toEqual([
                    {
                        ...clientInfo.defaults,
                        fields: [{
                            ...clientInfoFields[0],
                            value: newValue,
                        }, clientInfoFields[1]],
                    },
                    clientInfo.another,
                ]);
            });

            it('returns empty array if no chatId matches', () => {
                const newValue = 'new_value';
                const actual = getUpdatedClientInfoArray(clientInfoState as any, 'X', 'defaults', {
                    ...clientInfoFields[0],
                    value: newValue,
                });
                expect(actual).toEqual([]);
            });

            it('returns unmodified CRMs array if no crmKey matches', () => {
                const newValue = 'new_value';
                const actual = getUpdatedClientInfoArray(clientInfoState as any, '0/4/1', 'X', {
                    ...clientInfoFields[0],
                    value: newValue,
                });
                expect(actual).toEqual([clientInfo.defaults, clientInfo.another]);
            });

            it('returns unmodified CRMs array if no field key matches', () => {
                const newValue = 'new_value';
                const actual = getUpdatedClientInfoArray(clientInfoState as any, '0/4/1', 'defaults', {
                    ...clientInfoFields[0],
                    key: 'search_query', // unused key
                    value: newValue,
                });
                expect(actual).toEqual([clientInfo.defaults, clientInfo.another]);
            });
        });
    });
});
