import Archive from 'components/archive';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

const runTest = (state) => {
    global.LANG = 'ru';
    const store = init(state);
    const component = render(
        <Provider store={store}>
            <Archive />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

it('archive-with-no-selected', () => {
    const state = {
        doc: {
            title: 'archive-1.7z',
            actions: {},
            size: 100500
        },
        archive: {
            listing: {},
            nestedCount: 5,
            path: '',
            selectedFile: {
                path: ''
            }
        }
    };

    runTest(state);
});

it('archive-with-selected', () => {
    const state = {
        doc: {
            title: 'archive-2.rar',
            actions: {},
            size: 1023
        },
        archive: {
            listing: {},
            nestedCount: 23,
            path: '',
            selectedFile: {
                name: 'selected-file-name.ext',
                path: 'path/to/selected'
            },
            fileActions: {
                save: {
                    allow: false
                },
                download: {
                    allow: false
                }
            }
        }
    };

    runTest(state);
});
