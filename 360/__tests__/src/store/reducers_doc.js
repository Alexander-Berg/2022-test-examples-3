import * as actions from 'store/actions';
import init from 'store';

let store = null;
const resetStore = () => {
    store = init();
};

beforeEach(() => {
    resetStore();
});

describe('doc_reducer', () => {
    it('updateViewport', () => {
        // scale не устанавливается, так как у документа не задан docWidth
        store.dispatch(actions.updateViewport({ width: 500 }));
        expect(store.getState().doc.scale).toEqual(undefined);

        store.dispatch(actions.updateDoc({ docWidth: 800, scale: 1 }));

        // прописывается scale
        store.dispatch(actions.updateViewport({ width: 500 }));
        expect(store.getState().doc.scale).toEqual(500 / 800);

        // разница меньше 0.01 - scale не меняется
        store.dispatch(actions.updateViewport({ width: 493 }));
        expect(store.getState().doc.scale).toEqual(500 / 800);

        // разница становится больше 0.01 - scale меняется
        store.dispatch(actions.updateViewport({ width: 491 }));
        expect(store.getState().doc.scale).toEqual(491 / 800);

        // width / docWidth < 0.2, scale становится 0.2
        store.dispatch(actions.updateViewport({ width: 100 }));
        expect(store.getState().doc.scale).toEqual(0.2);

        // width / docWidth > 1, scale становится 1
        store.dispatch(actions.updateViewport({ width: 1000 }));
        expect(store.getState().doc.scale).toEqual(1);
    });

    it('updateDoc ~ iframe', () => {
        store.dispatch(actions.updateDoc({
            title: 'Документ.docx',
            state: 'READY',
            iframe: true,
            url: '/some/object/url'
        }));
        expect(store.getState().doc).toMatchSnapshot();
    });

    it('updateDoc ~ conversion error', () => {
        store.dispatch(actions.updateDoc({
            title: 'Таблица.xlsx',
            state: 'FAIL',
            errorCode: 'UNKNOWN_CONVERT_ERROR'
        }));
        expect(store.getState().doc).toMatchSnapshot();
    });

    it('updateDoc ~ wait', () => {
        store.dispatch(actions.updateDoc({
            title: 'Презентация.pptx',
            state: 'WAIT'
        }));
        expect(store.getState().doc).toMatchSnapshot();
    });

    it('updateDoc ~ ok', () => {
        store.dispatch(actions.updateDoc({
            title: 'Текст.txt',
            state: 'READY',
            contentFamily: '',
            withoutSize: false,
            pages: [{
                index: 1
            }, {
                index: 2
            }]
        }));
        expect(store.getState().doc).toMatchSnapshot();
    });

    it('updateAction', () => {
        store.dispatch(actions.updateDoc({
            title: 'Книга.epub',
            state: 'READY'
        }));

        store.dispatch(actions.updateAction('save', {
            allow: true,
            buttonUrl: '/some/button/url/',
            state: 'SAVED',
            folderUrl: '/some/folder/url/'
        }));
        expect(store.getState().doc.actions).toMatchSnapshot();

        store.dispatch(actions.updateAction('edit', {
            allow: true,
            url: '/some/editor/url/',
            folderUrl: '/url/to/downloads/folder'
        }));
        expect(store.getState().doc.actions).toMatchSnapshot();

        store.dispatch(actions.updateAction('share', {
            allow: true,
            isFromMail: true,
            saved: true,
            state: 'READY',
            url: '/some/share/url/'
        }));
        expect(store.getState().doc.actions).toMatchSnapshot();

        store.dispatch(actions.updateAction('print', {
            allow: true,
            state: 'WAIT'
        }));
        expect(store.getState().doc.actions).toMatchSnapshot();

        store.dispatch(actions.updateAction('download', {
            allow: true
        }));
        expect(store.getState().doc.actions).toMatchSnapshot();
    });

    it('UPDATE_DOC_PAGE', () => {
        store.dispatch(actions.updateDoc({
            pages: [{
                index: 1
            }, {
                index: 2
            }, {
                index: 3
            }, {
                index: 4
            }]
        }));
        const actionType = require('store/consts').actions.UPDATE_DOC_PAGE;

        store.dispatch({
            type: actionType,
            page: {
                index: 1,
                width: 800,
                height: 600,
                state: 'READY',
                html: '<div>Some HTML is here</div>'
            }
        });
        expect(store.getState().doc.pages).toMatchSnapshot();

        store.dispatch({
            type: actionType,
            page: {
                index: 2,
                state: 'FAIL'
            }
        });
        expect(store.getState().doc.pages).toMatchSnapshot();

        store.dispatch({
            type: actionType,
            page: {
                index: 3,
                state: 'WAIT'
            }
        });
        expect(store.getState().doc.pages).toMatchSnapshot();
    });
});
