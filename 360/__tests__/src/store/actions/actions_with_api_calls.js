import _ from 'lodash';
import * as actions from 'store/actions';
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

describe('dispatch updateDocStatus', () => {
    const extractCheckingFields = (store) => {
        return _.pick(
            store.getState().doc,
            ['state', 'errorCode', 'pages', 'withoutSize']
        );
    };

    const stateArray = [
        { state: 'WAIT', expectMessage: 'should change store.doc.state to WAIT and store.doc.reason to COPY' },
        { state: 'READY', expectMessage: 'should change store.doc.state to READY, store.doc.pages to [{ index: 1 }, { index: 2 }], store.doc.withoutSize to true' },
        { state: 'FAIL', expectMessage: 'should change store.doc.state to FAIL, store.doc.errorCode to UNSUPPORTED_CONVERTION' },
        { state: 'unhadled error', expectMessage: 'should change store.doc.state to FAIL, store.doc.errorCode to "unhadled error"' },
        { state: 'Exception', expectMessage: 'should change store.doc.state to FAIL, store.doc.errorCode to exception' }
    ];

    const runTest = (state) => () => {
        global.backendState.status = state;
        return store.dispatch(actions.updateDocStatus())
            .then(() => {
                expect(extractCheckingFields(store)).toMatchSnapshot();
            });
    };

    for (let i = 0; i < stateArray.length; i++) {
        const testName = `got ${stateArray[i].state} from API\n${stateArray[i].expectMessage}`;
        it(testName, runTest(stateArray[i].state));
    }
});

describe('dispatch updateDocIframe', () => {
    beforeEach(() => {
        store.dispatch(actions.updateDoc({
            state: 'READY',
            iframe: true
        }));
    });

    const extractCheckingFields = (store) => {
        return _.pick(
            store.getState().doc,
            ['state', 'iframe', 'url', 'errorCode']
        );
    };

    const stateArray = [
        { state: 'data.url', expectMessage: 'should change store.doc.state to READY, store.doc.url to some url' },
        { state: 'error', expectMessage: 'should change store.doc.state to FAIL, store.doc.errorCode to error' },
        { state: 'Exception', expectMessage: 'should change store.doc.state to FAIL, store.doc.errorCode to exception' }
    ];

    const runTest = (state) => () => {
        global.backendState.iframe = state;
        return store.dispatch(actions.updateDocIframe())
            .then(() => {
                expect(extractCheckingFields(store)).toMatchSnapshot();
            });
    };

    for (let i = 0; i < stateArray.length; i++) {
        const testName = `got ${stateArray[i].state} from API\n${stateArray[i].expectMessage}`;
        it(testName, runTest(stateArray[i].state));
    }
});

describe('dispatch updateDocPage', () => {
    beforeEach(() => {
        store.dispatch(actions.updateDoc({
            fileId: 'file-id',
            width: 400,
            pages: [{ index: 1 }, { index: 2 }, { index: 3 }]
        }));
    });

    const stateArray = [
        { state: 'READY', page: 1, expectMessage: 'should change store.doc.pages[0] to {state:WAIT} right now and to {state:READY,html:[html],forceWidth,style,textInfo} after callback' },
        { state: 'error', page: 2, expectMessage: 'should change store.doc.pages[1].state to WAIT right now and to FAIL after callback' },
        { state: 'Exception', page: 3, expectMessage: 'should change store.doc.pages[2].state to WAIT right now and to FAIL after callback' },
        { state: 'READY', retries: 3, page: 1, expectMessage: 'should change store.doc.pages[0] to {state:WAIT} right now and to {state:READY,html:[html],forceWidth,style,textInfo} after callback' },
        { state: 'error', retries: 3, page: 2, expectMessage: 'should change store.doc.pages[1].state to WAIT right now and to FAIL after callback' },
        { state: 'READY', page: '3 (page as string)', expectMessage: 'should change store.doc.pages[2] to {state:WAIT} right now and to {state:READY,html:[html],forceWidth,style,textInfo} after callback' },
        { state: 'too small index', page: 0, expectMessage: 'should throw RangeError' },
        { state: 'too big index', page: 6, expectMessage: 'should throw RangeError' },
        { state: 'index is not a number', page: 'qwerty', expectMessage: 'should throw TypeError' }
    ];

    const runTest = ({ state, page, retries }) => () => {
        global.backendState.page = state;
        global.backendState.page_retries_max = retries;
        retries = retries || 1;

        if (state === 'too small index' || state === 'too big index') {
            expect(() => store.dispatch(actions.updateDocPage(page, retries))).toThrowError(RangeError);
        } else if (state === 'index is not a number') {
            expect(() => store.dispatch(actions.updateDocPage(page, retries))).toThrowError(TypeError);
        } else {
            utilsMock.delay.mockClear();
            const prom = store.dispatch(actions.updateDocPage(page, retries));

            // статус поменялся на 'WAIT', запрос ещё в процессе
            expect(store.getState().doc.pages).toMatchSnapshot();
            prom.then(() => {
                expect(utilsMock.delay.mock.calls.length).toBe(retries - 1);
                if (retries > 1) {
                    for (let i = 0; i < retries - 1; i++) {
                        // Нулевой аргумент вызова функции - число милисекунд
                        expect(utilsMock.delay.mock.calls[i][0]).toBe(consts.retries.page.delay);
                    }
                }
                expect(store.getState().doc.pages).toMatchSnapshot();
            });
            return prom;
        }
    };

    for (let i = 0; i < stateArray.length; i++) {
        const stateObj = stateArray[i];
        const testName = `for page ${stateObj.page}, got ${stateObj.state} from API after ${stateObj.retries || 1} retries\n${stateArray[i].expectMessage}`;
        it(testName, runTest(stateObj));
    }
});

describe('dispatch updateDocPageText', () => {
    beforeEach(() => {
        store.dispatch(actions.updateCfg({
            ua: { isMobile: false }
        }));
        store.dispatch(actions.updateDoc({
            fileId: 'file-id',
            width: 400,
            pages: [
                { index: 1, textInfo: true },
                { index: 2, textInfo: false },
                { index: 3, textInfo: true, text: [{}] }
            ]
        }));
        backendMock.mockClear();
    });

    const runTest = ({ state, page, called }) => () => {
        global.backendState.page = state;
        return store.dispatch(
            actions.updateDocPageText(page)
        ).then(() => {
            if (called) {
                expect(backendMock).toHaveBeenCalledTimes(called);
            } else {
                expect(backendMock).not.toHaveBeenCalled();
            }
            expect(store.getState().doc.pages[page - 1]).toMatchSnapshot();
        });
    };

    [
        { state: 'READY', page: 1, called: 1, message: 'should add text' },
        { state: 'READY', page: 2, called: 0, message: 'should not call backend for textInfo=false' },
        { state: 'READY', page: 3, called: 0, message: 'should not call backend for page with text' },
        { state: 'error', page: 1, called: 1, message: 'should assign textInfo=false' }
    ].forEach(
        (object) => it(object.message, runTest(object))
    );
});

describe('dispatch updateActions', () => {
    const stateArray = [
        { state: 'actions-for-plain-file', expectMessage: 'should update store.doc.actions and not change store.archive.fileActions' },
        { state: 'actions-for-archive', isArchive: true, expectMessage: 'should update store.doc.actions and store.archive.fileActions' },
        { state: 'actions-for-file-from-archive', isArchive: true, archivePath: 'la-la-la',
            expectMessage: 'should update only "save" and "download" in store.doc.actions and store.archive.fileActions' },
        { state: 'error', expectMessage: 'should not change store.doc.actions and store.archive.fileActions' },
        { state: 'Exception', expectMessage: 'should throw' }
    ];
    const runTest = ({ state, isArchive, archivePath }) => () => {
        if (isArchive) {
            store.dispatch(actions.updateDoc({
                state: 'ARCHIVE',
                archivePath: archivePath || ''
            }));
        }
        global.backendState['update-actions'] = state;
        return store.dispatch(actions.updateActions())
            .then(() => {
                expect(state).not.toBe('Exception');
                expect(store.getState().doc.actions).toMatchSnapshot();
                expect(store.getState().archive.fileActions).toMatchSnapshot();
            })
            .catch(() => {
                expect(state).toBe('Exception');
            });
    };

    for (let i = 0; i < stateArray.length; i++) {
        const testName = `got ${stateArray[i].state} from API\n${stateArray[i].expectMessage}`;
        it(testName, runTest(stateArray[i]));
    }
});

describe('dispatch updateOfficeEditorUrl', () => {
    const stateArray = [
        { state: 'data.url', expectMessage: 'should set store.doc.actions.edit.url' },
        { state: 'error', expectMessage: 'should not change store.doc.actions.edit' },
        { state: 'Exception', expectMessage: 'should throw' }
    ];
    const runTest = (state) => () => {
        global.backendState['get-office-editor-url'] = state;
        return store.dispatch(actions.updateOfficeEditorUrl())
            .then(() => {
                expect(state).not.toBe('Exception');
                expect(store.getState().doc.actions.edit).toMatchSnapshot();
            })
            .catch(() => {
                expect(state).toBe('Exception');
            });
    };

    for (let i = 0; i < stateArray.length; i++) {
        const testName = `got ${stateArray[i].state} from API\n${stateArray[i].expectMessage}`;
        it(testName, runTest(stateArray[i].state));
    }
});

describe('dispatch updateArchiveListing', () => {
    const stateArray = [
        { state: 'READY', expectMessage: 'should change store.archive to {state:WAIT} right now and to {state:READY,listing,nestedCount} after callback' },
        { state: 'error', expectMessage: 'should change store.archive.state to WAIT right now and to FAIL after callback' },
        { state: 'Exception', expectMessage: 'should change store.archive.state to WAIT right now and to FAIL after callback' }
    ];

    const runTest = ({ state }) => () => {
        global.backendState['archive-listing'] = state;

        const prom = store.dispatch(actions.updateArchiveListing());

        // статус поменялся на 'WAIT', запрос ещё в процессе
        expect(store.getState().archive).toMatchSnapshot();
        prom.then(() => {
            expect(store.getState().archive).toMatchSnapshot();
        });
        return prom;
    };

    for (let i = 0; i < stateArray.length; i++) {
        const stateObj = stateArray[i];
        it(stateObj.expectMessage, runTest(stateObj));
    }
});

Object.defineProperty(window.location, 'assign', {
    value: jest.fn()
});

describe('dispatch download', () => {
    it('should assign location to some url', () => {
        return store.dispatch(actions.download()).then(() => {
            expect(window.location.assign.mock.calls.length).toEqual(1);
            expect(window.location.assign.mock.calls[0]).toMatchSnapshot();
            window.location.assign.mockClear();
        });
    });
    it('should assign location to url for file from archive', () => {
        return store.dispatch(actions.download('archive-path')).then(() => {
            expect(window.location.assign.mock.calls.length).toEqual(1);
            expect(window.location.assign.mock.calls[0]).toMatchSnapshot();
            window.location.assign.mockClear();
        });
    });
});

describe('dispatch downloadSelected', () => {
    it('should assign location to url for file from archive', () => {
        store.dispatch(actions.updateArchiveSelectedFile({
            path: 'selected/path'
        }));
        return store.dispatch(actions.downloadSelected()).then(() => {
            expect(window.location.assign.mock.calls.length).toEqual(1);
            expect(window.location.assign.mock.calls[0]).toMatchSnapshot();
            window.location.assign.mockClear();
        });
    });
});
