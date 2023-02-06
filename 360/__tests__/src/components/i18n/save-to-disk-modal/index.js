/* global langs */
import SaveModal from 'components/save-to-disk-modal';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

const runTest = (lang, saveState, source, isMobile = false) => {
    global.LANG = lang;
    const state = {
        cfg: {
            ua: { isMobile }
        }
    };
    if (source === 'archive') {
        state.archive = {
            fileActions: {
                save: {
                    state: saveState,
                    folderUrl: 'some/folder/url/for/archive',
                    name: 'saved-file-from-archive.ext'
                }
            }
        };
    } else if (source === 'edit') {
        state.user = {
            auth: true
        };
        state.doc = {
            actions: {
                edit: {
                    state: saveState,
                    folderUrl: 'folder/url/for/editing'
                },
                save: {}
            }
        };
    } else if (source === 'save') {
        state.doc = {
            actions: {
                edit: {},
                save: {
                    state: saveState,
                    folderUrl: 'some/folder/url'
                }
            }
        };
    }
    const store = init(state);

    const component = render(
        <Provider store={store}>
            <SaveModal />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

const stateHash = {
    save: ['READY', 'FAIL', 'OUT_OF_SPACE'],
    edit: ['READY'],
    archive: ['READY', 'FAIL', 'OUT_OF_SPACE']
};

for (let i = 0; i < langs.length; i++) {
    const lang = langs[i];
    describe('[' + lang + '] save-to-disk-modal', () => {
        for (const source in stateHash) {
            if (stateHash.hasOwnProperty(source)) {
                for (let i = 0; i < stateHash[source].length; i++) {
                    const state = stateHash[source][i];
                    it(source + ' - ' + state,
                        () => runTest(lang, state, source));

                    it(source + ' - ' + state + ' - mobile',
                        () => runTest(lang, state, source, true));
                }
            }
        }
    });
}
