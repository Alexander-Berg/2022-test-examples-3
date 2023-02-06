import editorMiddleware from '../../../../../src/store/middleware/editor';
import { initEditor, pasteHtmlOnNoteLoaded, editorEnabled } from '../../../../../src/store/actions';

describe('editorMiddleware =>', () => {
    const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch) : arg);
    const currentNoteId = '1';
    const getState = () => ({
        notes: { current: currentNoteId }
    });
    const next = jest.fn();
    const mockedEditor = {
        editing: {
            view: {
                document: {
                    fire: jest.fn()
                }
            }
        }
    };
    const mockedRawHtml = '<span>ABC</span>';

    beforeEach(() => {
        jest.resetAllMocks();
    });
    beforeEach(() => {
        window.yandex = {
            notes: {
                OnSelectionRequested: { addListener: jest.fn() }
            }
        };
    });
    afterEach(() => {
        window.yandex = undefined;
    });

    it('should call addListener on editor middleware init', () => {
        editorMiddleware({ dispatch, getState })(next);
        expect(window.yandex.notes.OnSelectionRequested.addListener).toBeCalledTimes(1);
    });

    it('should add editor instance to the middleware`s closure on initEditor dispatching', () => {
        const action = initEditor(mockedEditor);

        editorMiddleware({ dispatch, getState })(next)(action);
        expect(popFnCalls(next)[0]).toEqual([action]);
    });

    it('should add rawHtml to the middleware`s closure on pasteHtmlOnNoteLoaded dispatching', () => {
        editorMiddleware({ dispatch, getState })(next)(initEditor(mockedEditor));

        const action = pasteHtmlOnNoteLoaded(mockedRawHtml);

        editorMiddleware({ dispatch, getState })(next)(action);
        expect(popFnCalls(next)[1]).toEqual([action]);
    });

    it('enableEditor should fire "clipboardInput" if rawHtml is in the middleware`s closure', () => {
        editorMiddleware({ dispatch, getState })(next)(initEditor(mockedEditor));
        editorMiddleware({ dispatch, getState })(next)(pasteHtmlOnNoteLoaded(mockedRawHtml));
        editorMiddleware({ dispatch, getState })(next)(editorEnabled());
        editorMiddleware({ dispatch, getState })(next)(editorEnabled());
        // check that rawHtml is pasted only on on the first editorEnabled action
        expect(mockedEditor.editing.view.document.fire).toBeCalledTimes(1);
        expect(popFnCalls(mockedEditor.editing.view.document.fire)[0][1]).toEqual({ rawHtml: mockedRawHtml });
        expect(dispatch).toBeCalledTimes(1);
    });
});
