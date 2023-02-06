import * as actions from '../../../../src/store/actions';
import { actions as actionTypes } from '../../../../src/store/consts';

beforeAll(() => {
    global.LANG = 'ru';
});

it('updateURL', () => {
    expect(actions.updateURL({}, '', '')).toMatchSnapshot();
});

it('updateUser', () => {
    expect(actions.updateUser({})).toMatchSnapshot();
});

it('updateCfg', () => {
    expect(actions.updateCfg({})).toMatchSnapshot();
});

it('updateDoc', () => {
    expect(actions.updateDoc({})).toMatchSnapshot();
});

it('updateDocStatus', () => {
    expect(actions.updateDocStatus()).toMatchSnapshot();
});

it('updateDocIframe', () => {
    expect(actions.updateDocIframe()).toMatchSnapshot();
});

it('updateDocPage', () => {
    expect(actions.updateDocPage()).toMatchSnapshot();
});

it('updateActions', () => {
    expect(actions.updateActions()).toMatchSnapshot();
});

it('updateAction', () => {
    expect(actions.updateAction('save', { allow: true, state: 'READY', buttonUrl: 'some/button/url' })).toMatchSnapshot();
    expect(actions.updateAction('edit', { allow: true, url: 'some/edit/url' })).toMatchSnapshot();
    expect(actions.updateAction('share', { allow: true, state: 'FAIL' })).toMatchSnapshot();
    expect(actions.updateAction('print', { allow: false, state: 'WAIT' })).toMatchSnapshot();
    expect(actions.updateAction('download', { allow: false })).toMatchSnapshot();
});

describe('goToPage', () => {
    const mockDispatch = jest.fn();
    const mockGetState = (currentPage, totalPages = 5) => () => ({
        cfg: {
            embed: false,
            ua: {
                isMobile: false
            }
        },
        doc: {
            pages: new Array(totalPages),
            iframe: false,
            withoutSize: false,
            contentFamily: 'presentation'
        },
        url: {
            query: {
                page: currentPage
            }
        }
    });

    it('should call CALL_HISTORY action with pushState', () => {
        actions.goToPage(2)(mockDispatch, mockGetState(1));
        expect(popFnCalls(mockDispatch)).toMatchSnapshot();
    });

    it('should call CALL_HISTORY action with replaceState', () => {
        actions.goToPage(2, true)(mockDispatch, mockGetState(1));
        expect(popFnCalls(mockDispatch)).toMatchSnapshot();
    });

    it('should call CALL_HISTORY and UPDATE_CONTEXT actions', () => {
        const originalDateNow = Date.now;
        Date.now = () => 12345678;
        actions.goToPage(3, true, true)(mockDispatch, mockGetState(2));
        expect(popFnCalls(mockDispatch)).toMatchSnapshot();
        Date.now = originalDateNow;
    });

    it('should not do anything if page is the same as current', () => {
        actions.goToPage(4)(mockDispatch, mockGetState(4));
        expect(popFnCalls(mockDispatch)).toEqual([]);
    });

    it('should switch to page 1 if page <= 0', () => {
        actions.goToPage(0)(mockDispatch, mockGetState(2));
        expect(popFnCalls(mockDispatch)).toMatchSnapshot();

        actions.goToPage(-5)(mockDispatch, mockGetState(4));
        expect(popFnCalls(mockDispatch)).toMatchSnapshot();
    });

    it('should not do anything if current page is 1 and page <= 0', () => {
        actions.goToPage(0)(mockDispatch, mockGetState(1));
        expect(popFnCalls(mockDispatch)).toEqual([]);

        actions.goToPage(-5)(mockDispatch, mockGetState(1));
        expect(popFnCalls(mockDispatch)).toEqual([]);
    });

    it('should switch to last page if page > last', () => {
        actions.goToPage(6)(mockDispatch, mockGetState(2));
        expect(popFnCalls(mockDispatch)).toMatchSnapshot();

        actions.goToPage(100)(mockDispatch, mockGetState(1));
        expect(popFnCalls(mockDispatch)).toMatchSnapshot();
    });

    it('should not do anything if current page is last and page > last 0', () => {
        actions.goToPage(6)(mockDispatch, mockGetState(5));
        expect(popFnCalls(mockDispatch)).toEqual([]);

        actions.goToPage(100)(mockDispatch, mockGetState(5));
        expect(popFnCalls(mockDispatch)).toEqual([]);
    });

    it('should load neighbours in single page mode', () => {
        actions.goToPage(7)(mockDispatch, mockGetState(6, 11));
        const calls = popFnCalls(mockDispatch);

        expect(calls).toMatchSnapshot();
        expect(calls[0][0] === actions.loadNeighboursFor);
    });
});

describe('loadNeighboursFor', () => {
    const getMockDispatch = (getStateFn) => {
        const mockDispatch = jest.fn((arg) => {
            if (typeof arg === 'function') {
                arg(mockDispatch, getStateFn);
            } else if (arg.type === actionTypes.CALL_API) {
                return Promise.resolve({ data: {} });
            }
        });
        return mockDispatch;
    };

    const getPageIndexesToUpdate = (fn) => popFnCalls(fn).filter(([arg]) => {
        return typeof arg === 'object' && arg.type === actionTypes.UPDATE_DOC_PAGE;
    }).map(([arg]) => arg.page.index);

    it('should load neighbours', () => {
        const mockGetState = () => ({
            doc: {
                pages: (new Array(15)).fill({})
            }
        });
        const mockDispatch = getMockDispatch(mockGetState);

        actions.loadNeighboursFor(8)(mockDispatch, mockGetState);
        expect(getPageIndexesToUpdate(mockDispatch)).toMatchSnapshot();
    });

    it('should not load neighbours because they\'re already loaded/loading', () => {
        const mockGetState = () => ({
            doc: {
                pages: [{ status: 'READY' }, {}, { status: 'WAIT' }]
            }
        });
        const mockDispatch = getMockDispatch(mockGetState);

        actions.loadNeighboursFor(2)(mockDispatch, mockGetState);
        expect(getPageIndexesToUpdate(mockDispatch)).toMatchSnapshot();
    });

    it('should correctly handle out-of-bounds page', () => {
        const mockGetState = () => ({
            doc: {
                pages: [{}, {}]
            }
        });
        const mockDispatch = getMockDispatch(mockGetState);

        actions.loadNeighboursFor(5)(mockDispatch, mockGetState);
        expect(getPageIndexesToUpdate(mockDispatch)).toMatchSnapshot();
        actions.loadNeighboursFor(-1)(mockDispatch, mockGetState);
        expect(getPageIndexesToUpdate(mockDispatch)).toMatchSnapshot();
    });
});

it('saveToDisk', () => {
    expect(actions.saveToDisk('publish', 'archive/path', 'name')).toMatchSnapshot();
});

it('updateOfficeEditorUrl', () => {
    expect(actions.updateOfficeEditorUrl()).toMatchSnapshot();
});

it('share', () => {
    expect(actions.share()).toMatchSnapshot();
});

it('mpfsStatus', () => {
    expect(actions.mpfsStatus('oid-123')).toMatchSnapshot();
});

it('updateArchivePath', () => {
    expect(actions.updateArchivePath('some/archive/path')).toMatchSnapshot();
});

it('updateArchiveSelectedFile', () => {
    expect(actions.updateArchiveSelectedFile({
        name: 'file-name.ext',
        path: 'some-folder/another-folder/file-name.ext',
        viewable: true,
        encrypted: true
    })).toMatchSnapshot();
});

it('updateArchiveAction', () => {
    expect(actions.updateArchiveAction('save', {
        allow: true,
        state: 'READY',
        name: 'another-name.doc',
        folderUrl: '/another/folder/urrrl'
    })).toMatchSnapshot();
    expect(actions.updateArchiveAction('download', {
        allow: true
    })).toMatchSnapshot();
});

it('disableEditPromo', () => {
    expect(document.cookie).toEqual('');
    expect(actions.disableEditPromo()).toMatchSnapshot();
    expect(document.cookie).toEqual('edit-promo-disabled=1');
});

it('getPageState', () => {
    expect(actions.getPageState(1)(null, () => ({
        doc: {
            pages: [{ index: 1, state: 'WAIT' }]
        }
    }))).toEqual('WAIT');
    expect(actions.getPageState(2)(null, () => ({
        doc: {
            pages: [{ index: 1, state: 'READY' }, { index: 2 }]
        }
    }))).toEqual(undefined);
});

it('goToPageWithoutSize', () => {
    const mockDispatch = jest.fn();
    const mockGetState = () => ({
        doc: {
            pages: [
                { index: 1 },
                { index: 2, state: 'WAIT' },
                { index: 3, state: 'READY' },
                { index: 4, state: 'FAIL' },
                { index: 5 },
                { index: 6 }
            ]
        }
    });
    actions.goToPageWithoutSize(1)(mockDispatch, mockGetState);
    expect(popFnCalls(mockDispatch)).toMatchSnapshot();
});
