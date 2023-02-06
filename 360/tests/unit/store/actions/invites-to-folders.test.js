import '../../noscript';
import createStore from '../../../../components/redux/store/create-store';
import * as rawFetchModel from '../../../../components/redux/store/lib/raw-fetch-model';
import {
    INVITES_STATUSES,
    acceptInviteToFolder,
    rejectInviteToFolder,
    remoteAcceptInviteToFolder,
    remoteRejectInviteToFolder
} from '../../../../components/redux/store/actions/invites-to-folders';

jest.mock('../../../../components/redux/store/actions/notifications', () => ({
    notify: jest.fn(() => jest.fn())
}));

jest.mock('../../../../components/redux/store/actions/resources', () => ({
    fetchSingleResource: jest.fn(() => jest.fn())
}));

describe('invitesToFolders actions', () => {
    const originalRawFetchModel = rawFetchModel.default;
    afterEach(() => {
        rawFetchModel.default = originalRawFetchModel;
    });

    describe('rejectInviteToFolder', () => {
        it('при успешном отклонении приглашения оно удаляется из списка приглашений', (done) => {
            const HASH = 'b5912e77431e183d27a5c2b4410f67b1';
            const store = createStore({
                invitesToFolders: {
                    isLoaded: true,
                    invites: [HASH],
                    invitesByHash: { [HASH]: { hash: HASH, status: INVITES_STATUSES.INITIAL } }
                }
            });

            rawFetchModel.default = jest.fn(() => Promise.resolve({}));

            store.dispatch(rejectInviteToFolder(HASH)).then(() => {
                const { invitesToFolders } = store.getState();
                expect(invitesToFolders.invites).toEqual([]);
                done();
            });
        });

        it('при ошибке при отклонении приглашения, оно не удаляется из списка приглашений', (done) => {
            const HASH = 'b5912e77431e183d27a5c2b4410f67b1';
            const store = createStore({
                invitesToFolders: {
                    isLoaded: true,
                    invites: [HASH],
                    invitesByHash: { [HASH]: { hash: HASH, status: INVITES_STATUSES.INITIAL } }
                }
            });

            rawFetchModel.default = jest.fn(() => Promise.reject({}));

            store.dispatch(rejectInviteToFolder(HASH)).catch(() => {
                const { invitesToFolders } = store.getState();
                expect(invitesToFolders.invites).toEqual([HASH]);
                expect(invitesToFolders.invitesByHash[HASH].status).toEqual(INVITES_STATUSES.INITIAL);
                done();
            });
        });
    });

    describe('acceptInviteToFolder', () => {
        it('при успешном принятии приглашения оно удаляется из списка приглашений', (done) => {
            const HASH = 'b5912e77431e183d27a5c2b4410f67b1';
            const FOLDER_ID = '/disk/folder';
            const store = createStore({
                invitesToFolders: {
                    isLoaded: true,
                    invites: [HASH],
                    invitesByHash: { [HASH]: { hash: HASH, status: INVITES_STATUSES.INITIAL } }
                }
            });

            rawFetchModel.default = jest.fn((modelName) => {
                if (modelName === 'do-resource-invite-accept') {
                    return Promise.resolve({ id: FOLDER_ID });
                }
                return Promise.resolve({ id: FOLDER_ID });
            });

            store.dispatch(acceptInviteToFolder(HASH)).then(() => {
                const { invitesToFolders } = store.getState();
                expect(invitesToFolders.invites).toEqual([]);
                done();
            });
        });

        it('при ошибке при принятии приглашения оно не удаляется из списка приглашений', (done) => {
            const HASH = 'b5912e77431e183d27a5c2b4410f67b1';
            const store = createStore({
                invitesToFolders: {
                    isLoaded: true,
                    invites: [HASH],
                    invitesByHash: { [HASH]: { hash: HASH, status: INVITES_STATUSES.INITIAL } }
                }
            });

            rawFetchModel.default = jest.fn(() => {
                return Promise.reject({});
            });

            store.dispatch(acceptInviteToFolder(HASH)).catch(() => {
                const { invitesToFolders } = store.getState();
                expect(invitesToFolders.invites).toEqual([HASH]);
                expect(invitesToFolders.invitesByHash[HASH].status).toEqual(INVITES_STATUSES.INITIAL);
                done();
            });
        });
    });

    describe('remoteRejectInviteToFolder', () => {
        it('отклонение приглашения для приглашенного', () => {
            const HASH = 'b5912e77431e183d27a5c2b4410f67b1';
            const GID = '8e53be648f454ddf520e68706840195e';
            const PATH = '/disk/folder';
            const store = createStore({
                invitesToFolders: {
                    isLoaded: true,
                    invites: [HASH],
                    invitesByHash: { [HASH]: { hash: HASH, gid: GID, status: INVITES_STATUSES.INITIAL } }
                }
            });

            store.dispatch(remoteRejectInviteToFolder({ folder: { path: PATH, gid: GID } }, 'actor'));

            const { invitesToFolders } = store.getState();
            expect(invitesToFolders.invites).toEqual([]);
        });
    });

    describe('remoteAcceptInviteToFolder', () => {
        it('принятие приглашения для приглашенного', () => {
            const HASH = 'b5912e77431e183d27a5c2b4410f67b1';
            const GID = '8e53be648f454ddf520e68706840195e';
            const PATH = '/disk/folder';
            const store = createStore({
                invitesToFolders: {
                    isLoaded: true,
                    invites: [HASH],
                    invitesByHash: { [HASH]: { hash: HASH, gid: GID, status: INVITES_STATUSES.INITIAL } }
                }
            });

            store.dispatch(remoteAcceptInviteToFolder({ folder: { path: PATH, gid: GID } }, 'actor'));

            const { invitesToFolders } = store.getState();
            expect(invitesToFolders.invites).toEqual([]);
        });
    });
});
