import '../../noscript';
import invitesToFolders from '../../../../components/redux/store/reducers/invites-to-folders';
import { INVITES_STATUSES } from '../../../../components/redux/store/actions/invites-to-folders';
import {
    UPDATE_INVITES_TO_FOLDERS,
    UPDATE_INVITE_TO_FOLDER,
    REMOVE_INVITE_TO_FOLDER
} from '../../../../components/redux/store/actions/types';
import deepFreeze from 'deep-freeze';

describe('invitesToFolders reducer', () => {
    describe('UPDATE_INVITES_TO_FOLDERS', () => {
        it('должен обновлять список приглашений и признак загруженности и не обновлять состояния приглашений', () => {
            const state = {
                isLoaded: false,
                invites: ['973caab80aa28c03f9c30b25851e668'],
                invitesByHash: {
                    '973caab80aa28c03f9c30b25851e668': {
                        hash: '973caab80aa28c03f9c30b25851e668',
                        hidden: true,
                        status: INVITES_STATUSES.INITIAL
                    }
                }
            };
            deepFreeze(state);
            const newState = invitesToFolders(state, {
                type: UPDATE_INVITES_TO_FOLDERS,
                payload: [{ hash: '85912e77431e183d27a5c2b4410f67b1' }, { hash: '973caab80aa28c03f9c30b25851e668' }]
            });

            expect(newState).toEqual({
                isLoaded: true,
                invites: ['85912e77431e183d27a5c2b4410f67b1', '973caab80aa28c03f9c30b25851e668'],
                invitesByHash: {
                    '85912e77431e183d27a5c2b4410f67b1': {
                        hash: '85912e77431e183d27a5c2b4410f67b1',
                        status: INVITES_STATUSES.INITIAL
                    },
                    '973caab80aa28c03f9c30b25851e668': {
                        hash: '973caab80aa28c03f9c30b25851e668',
                        hidden: true,
                        status: INVITES_STATUSES.INITIAL
                    }
                }
            });
        });
    });

    describe('UPDATE_INVITE_TO_FOLDER', () => {
        it('должен обновлять состяние указанного приглашения', () => {
            const state = {
                isLoaded: true,
                invites: ['973caab80aa28c03f9c30b25851e668'],
                invitesByHash: {
                    '973caab80aa28c03f9c30b25851e668': {
                        hash: '973caab80aa28c03f9c30b25851e668',
                        hidden: true,
                        status: INVITES_STATUSES.INITIAL
                    }
                }
            };
            deepFreeze(state);
            const newState = invitesToFolders(state, {
                type: UPDATE_INVITE_TO_FOLDER,
                payload: {
                    hash: '973caab80aa28c03f9c30b25851e668',
                    hidden: false
                }
            });
            expect(newState.invitesByHash['973caab80aa28c03f9c30b25851e668'].hidden).toEqual(false);
        });
    });

    describe('REMOVE_INVITE_TO_FOLDER', () => {
        it('должен удалить приглашение и его состояние', () => {
            const state = {
                isLoaded: true,
                invites: ['85912e77431e183d27a5c2b4410f67b1', '973caab80aa28c03f9c30b25851e668'],
                invitesByHash: {
                    '85912e77431e183d27a5c2b4410f67b1': {
                        hash: '85912e77431e183d27a5c2b4410f67b1',
                        status: INVITES_STATUSES.INITIAL
                    },
                    '973caab80aa28c03f9c30b25851e668': {
                        hash: '973caab80aa28c03f9c30b25851e668',
                        status: INVITES_STATUSES.INITIAL
                    }
                }
            };
            deepFreeze(state);
            const newState = invitesToFolders(state, {
                type: REMOVE_INVITE_TO_FOLDER,
                payload: {
                    hash: '85912e77431e183d27a5c2b4410f67b1'
                }
            });
            expect(newState.invitesByHash['85912e77431e183d27a5c2b4410f67b1']).toBeUndefined();
            expect(newState.invites).toEqual(['973caab80aa28c03f9c30b25851e668']);
        });
    });
});
