import * as actions from 'store/actions';
import * as sagas from 'store/sagas';
import init from 'store';
import * as consts from 'store/consts';

jest.mock('lib/backend');
import backendMock from 'lib/backend';
jest.mock('store/utils');
import * as utilsMock from 'store/utils';

let store = null;
const resetStore = () => {
    store = init(undefined, backendMock);
};

beforeEach(() => {
    resetStore();
});

beforeAll(() => {
    global.backendState = {};
    global.LANG = 'ru';
});

afterAll(() => {
    delete global.backendState;
});

describe('dispatch convert saga', () => {
// ToDo: написать
});

describe('dispatch preparePrint saga', () => {
// ToDo: написать
});

const runIdleShareTests = () => {
    // share не должен ничего делать если share.state уже в статусе WAIT
    const idleShareStatesArray = ['WAIT'];
    const runTestForIdleState = (state) => () => {
        store.dispatch(actions.updateAction('share', { state }));
        backendMock.mockClear();
        expect(store.getState().doc.actions.share).toMatchSnapshot();
        const res = store.dispatch(sagas.share());
        expect(res).toBe(undefined);
        expect(backendMock.mock.calls.length).toBe(0);
        expect(utilsMock.delay.mock.calls.length).toBe(0);
        expect(store.getState().doc.actions.share).toMatchSnapshot();
    };
    for (let i = 0; i < idleShareStatesArray.length; i++) {
        const testName = 'should not call backend and change store if share already in ' + idleShareStatesArray[i] + ' state';
        it(testName, runTestForIdleState(idleShareStatesArray[i]));
    }
};

// FixMe: хорошо бы ещё добавить тест на то, что поделение не запускается повторно (если статус поделения READY или WAIT)
describe('dispatch simple (not for doc from mail) share saga', () => {
    const stateArray = [
        { state: 'data.url', expectMessage: 'should change store.doc.actions.share to {state:WAIT} right now and to {state:READY,url:[url]} after callback' },
        { state: 'error', expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { state: 'Exception', expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' }
    ];
    const runTest = (state) => () => {
        global.backendState.share = state;

        const prom = store.dispatch(sagas.share());

        // статус поменялся на 'WAIT', запрос ещё в процессе
        expect(store.getState().doc.actions.share).toMatchSnapshot();
        prom.then(() => {
            expect(store.getState().doc.actions.share).toMatchSnapshot();
        });
        return prom;
    };

    for (let i = 0; i < stateArray.length; i++) {
        const testName = `got ${stateArray[i].state} from "share" API method\n${stateArray[i].expectMessage}`;
        it(testName, runTest(stateArray[i].state));
    }

    runIdleShareTests();
});

describe('dispatch share saga for document from mail', () => {
    beforeEach(() => {
        store.dispatch(actions.updateAction('share', { isFromMail: true }));
    });

    const stateArray = [
        { save: 'data-with-oid', mpfsStatus: { state: 'READY', retries: 1 }, expectMessage: 'should change store.doc.actions.share to {state:WAIT} right now and to {state:READY,saved:true,url:[url]} after callback' },
        { save: 'data-with-oid', mpfsStatus: { state: 'FAIL', retries: 1 }, expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'data-with-oid', mpfsStatus: { state: 'error', retries: 1 }, expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'data-with-oid', mpfsStatus: { state: 'Exception', retries: 1 }, expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'data-with-oid', mpfsStatus: { state: 'READY', retries: 3 }, expectMessage: 'should change store.doc.actions.share to {state:WAIT} right now and to {state:READY,saved:true,url:[url]} after callback' },
        { save: 'data-with-oid', mpfsStatus: { state: 'FAIL', retries: 3 }, expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'data-with-oid', mpfsStatus: { state: 'error', retries: 3 }, expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'data-with-oid', mpfsStatus: { state: 'Exception', retries: 3 }, expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'data.url', expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'data-with-no-url', expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'out-of-space', expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'error', expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' },
        { save: 'Exception', expectMessage: 'should change store.doc.actions.share.state to WAIT right now and to FAIL after callback' }
    ];

    const runTest = (state) => () => {
        global.backendState['save-to-disk'] = state.save;
        global.backendState['mpfs-status'] = state.mpfsStatus;
        if (global.backendState['mpfs-status']) {
            global.backendState['mpfs-status'].retries_made = 0;
        }
        utilsMock.delay.mockClear();

        const prom = store.dispatch(sagas.share());

        // статус поменялся на 'WAIT', запрос ещё в процессе
        expect(store.getState().doc.actions.share).toMatchSnapshot();
        prom.then(() => {
            const retries = state.mpfsStatus ? state.mpfsStatus.retries : 1;
            expect(utilsMock.delay.mock.calls.length).toBe(retries - 1);
            if (retries > 1) {
                for (let i = 0; i < retries - 1; i++) {
                    // Нулевой аргумент вызова функции - число милисекунд
                    expect(utilsMock.delay.mock.calls[i][0]).toBe(consts.retries.mpfsStatus.delay);
                }
            }
            expect(store.getState().doc.actions.share).toMatchSnapshot();
        });
        return prom;
    };

    for (let i = 0; i < stateArray.length; i++) {
        const shareState = stateArray[i];
        let testName = `got ${shareState.save} from "save-to-disk" API method`;
        if (shareState.mpfsStatus) {
            testName += ` and ${shareState.mpfsStatus.state} from "mpfs-status" API method after ${shareState.mpfsStatus.retries} retries`;
        }
        testName += `\n${shareState.expectMessage}`;
        it(testName, runTest(shareState));
    }

    runIdleShareTests();
});

const runSaveTestCases = function(sagaName, ...sagaArguments) {
    const isSaveFromArchive = sagaName !== 'saveToDisk';
    const getStoreActionsToCheck = () => isSaveFromArchive ?
        store.getState().archive.fileActions.save :
        store.getState().doc.actions.save;
    const storeActionsString = isSaveFromArchive ? 'store.archive.fileActions.save' : 'store.doc.actions.save';

    const stateArray = [
        { save: 'data-with-oid', mpfsStatus: { state: 'READY', retries: 1 }, expectMessage: `should change ${storeActionsString} to {state:WAIT} right now and to {state:READY,folderUrl:[url]} after callback` },
        { save: 'data-with-oid', mpfsStatus: { state: 'FAIL', retries: 1 }, expectMessage: `should change ${storeActionsString}.state to WAIT right now and to FAIL after callback` },
        { save: 'data-with-oid', mpfsStatus: { state: 'error', retries: 1 }, expectMessage: `should change ${storeActionsString}.state to WAIT right now and to FAIL after callback` },
        { save: 'data-with-oid', mpfsStatus: { state: 'Exception', retries: 1 }, expectMessage: `should change ${storeActionsString}.state to WAIT right now and to FAIL after callback` },
        { save: 'data-with-oid', mpfsStatus: { state: 'READY', retries: 4 }, expectMessage: `should change ${storeActionsString} to {state:WAIT} right now and to {state:READY,folderUrl:[url]} after callback` },
        { save: 'data-with-oid', mpfsStatus: { state: 'FAIL', retries: 3 }, expectMessage: `should change ${storeActionsString}.state to WAIT right now and to FAIL after callback` },
        { save: 'data-with-oid', mpfsStatus: { state: 'error', retries: 5 }, expectMessage: `should change ${storeActionsString}.state to WAIT right now and to FAIL after callback` },
        { save: 'data-with-oid', mpfsStatus: { state: 'Exception', retries: 2 }, expectMessage: `should change ${storeActionsString}.state to WAIT right now and to FAIL after callback` },
        { save: 'data.url', expectMessage: `should change ${storeActionsString} to {state:WAIT} right now and to {state:READY,folderUrl:[url]} after callback` },
        { save: 'data-with-no-url', expectMessage: `should change ${storeActionsString}.state to WAIT right now and to FAIL after callback` },
        { save: 'out-of-space', expectMessage: `should change ${storeActionsString}.state to WAIT right now and to OUT_OF_SPACE after callback` },
        { save: 'error', expectMessage: `should change ${storeActionsString}.state to WAIT right now and to FAIL after callback` },
        { save: 'Exception', expectMessage: `should change ${storeActionsString}.state to WAIT right now and to FAIL after callback` }
    ];
    const runTest = (state, archivePath) => () => {
        global.backendState['save-to-disk'] = state.save;
        global.backendState['mpfs-status'] = state.mpfsStatus;
        if (global.backendState['mpfs-status']) {
            global.backendState['mpfs-status'].retries_made = 0;
        }
        global.backendState.saveSagaName = sagaName;
        utilsMock.delay.mockClear();
        if (archivePath) {
            store.dispatch(actions.updateDoc({ archivePath }));
        }

        const prom = store.dispatch(sagas[sagaName].apply(this, sagaArguments));

        // статус поменялся на 'WAIT', запрос ещё в процессе
        expect(getStoreActionsToCheck()).toMatchSnapshot();
        prom.then(() => {
            const retries = state.mpfsStatus ? state.mpfsStatus.retries : 1;
            expect(utilsMock.delay.mock.calls.length).toBe(retries - 1);
            if (retries > 1) {
                for (let i = 0; i < retries - 1; i++) {
                    // Нулевой аргумент вызова функции - число милисекунд
                    expect(utilsMock.delay.mock.calls[i][0]).toBe(consts.retries.mpfsStatus.delay);
                }
            }
            expect(getStoreActionsToCheck()).toMatchSnapshot();
        });
        return prom;
    };

    for (let i = 0; i < stateArray.length; i++) {
        const saveState = stateArray[i];
        let testName = `got ${saveState.save} from API`;
        if (saveState.mpfsStatus) {
            testName += ` and ${saveState.mpfsStatus.state} from "mpfs-status" API method after ${saveState.mpfsStatus.retries} retries`;
        }
        testName += `\n${saveState.expectMessage}`;
        it(testName, runTest(saveState));
        const fromArchivePrefix = isSaveFromArchive ? '(for archive from archive)' : '(for file from archive)';
        it(fromArchivePrefix + ' ' + testName, runTest(saveState, 'archive-path'));
    }
};

describe('dispatch saveToDisk', () => {
    runSaveTestCases('saveToDisk');
});

describe('dispatch saveFromArchiveListing', () => {
    runSaveTestCases('saveFromArchiveListing', 'file-path-in-archive-listing', 'file-name-in-archive-listing');
});

describe('dispatch saveSelected', () => {
    let counter = 0;
    beforeEach(() => {
        counter++;
        store.dispatch(actions.updateArchiveSelectedFile({
            path: 'archive-selected-file-path-' + counter,
            name: 'archive-selected-file-name-' + counter
        }));
    });

    runSaveTestCases('saveSelected');
});
