import SaveModal from 'components/save-to-disk-modal';
import _ from 'lodash';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

const runTest = (state) => {
    global.LANG = 'ru';
    const run = (state) => {
        const store = init(state);
        const component = render(
            <Provider store={store}>
                <SaveModal />
            </Provider>
        );
        expect(component).toMatchSnapshot();
    };

    run(_.merge({ cfg: { ua: { isMobile: false } } }, state));
    run(_.merge({ cfg: { ua: { isMobile: true } } }, state));
};

it('document save-to-disk-modal-ok', () => {
    const state = {
        doc: {
            actions: {
                edit: {},
                save: {
                    state: 'READY',
                    folderUrl: 'some/folder/url'
                }
            }
        }
    };

    runTest(state);
});

it('document save-to-disk-modal-fail', () => {
    const state = {
        doc: {
            actions: {
                edit: {},
                save: {
                    state: 'FAIL'
                }
            }
        }
    };

    runTest(state);
});

it('document save-to-disk-modal-out-of-space', () => {
    const state = {
        doc: {
            actions: {
                edit: {},
                save: {
                    state: 'OUT_OF_SPACE'
                }
            }
        }
    };

    runTest(state);
});

it('editing save-to-disk-modal-ok', () => {
    const state = {
        user: {
            auth: true
        },
        doc: {
            actions: {
                edit: {
                    state: 'READY',
                    folderUrl: '/folder/url/for/editing'
                },
                save: {}
            }
        }
    };

    runTest(state);
});

it('archive save-to-disk-modal-ok', () => {
    const state = {
        archive: {
            fileActions: {
                save: {
                    state: 'READY',
                    folderUrl: 'some/folder/url/for/archive',
                    name: 'saved-file-from-archive.ext'
                }
            }
        }
    };

    runTest(state);
});

it('archive save-to-disk-modal-fail', () => {
    const state = {
        archive: {
            fileActions: {
                save: {
                    state: 'FAIL',
                    name: 'not-saved-file-from-archive-because-of-fail.ext'
                }
            }
        }
    };

    runTest(state);
});

it('archive save-to-disk-modal-out-of-space', () => {
    const state = {
        archive: {
            fileActions: {
                save: {
                    state: 'OUT_OF_SPACE',
                    name: 'saved-file-from-archive-because-out-of-space.ext'
                }
            }
        }
    };

    runTest(state);
});
