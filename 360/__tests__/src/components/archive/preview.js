import Preview from 'components/archive/preview';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import _ from 'lodash';
import { Provider } from 'react-redux';

const runTest = (ownState) => {
    global.LANG = 'ru';
    const state = {
        doc: {
            actions: {},
            archivePath: ''
        },
        archive: {
            selectedFile: {
                name: '',
                path: '',
                viewable: false,
                encrypted: false
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
    const store = init(_.merge({}, state, ownState));
    const component = render(
        <Provider store={store}>
            <Preview />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

it('preview - no selected file', () => {
    runTest({});
});

it('preview - encrypted', () => {
    runTest({
        archive: {
            selectedFile: {
                name: 'some-file.txt',
                path: 'some-file.txt',
                viewable: true,
                encrypted: true
            },
            fileActions: {
                save: {
                    allow: true
                },
                download: {
                    allow: true
                }
            }
        }
    });
});

it('preview - not viewable, no actions', () => {
    runTest({
        archive: {
            selectedFile: {
                name: 'some-file.exe',
                path: 'some-file.exe',
                viewable: false
            }
        }
    });
});

it('preview - not viewable, can download', () => {
    runTest({
        archive: {
            selectedFile: {
                name: 'some-file.exe',
                path: 'some-file.exe',
                viewable: false
            },
            fileActions: {
                download: {
                    allow: true
                }
            }
        }
    });
});

it('preview - not viewable, can save', () => {
    runTest({
        archive: {
            selectedFile: {
                name: 'some-file.exe',
                path: 'some-file.exe',
                viewable: false
            },
            fileActions: {
                save: {
                    allow: true
                }
            }
        }
    });
});

it('preview - not viewable, both actions', () => {
    runTest({
        archive: {
            selectedFile: {
                name: 'some-file.exe',
                path: 'some-file.exe',
                viewable: false
            },
            fileActions: {
                save: {
                    allow: true
                },
                download: {
                    allow: true
                }
            }
        }
    });
});

it('preview - viewable', () => {
    // в путь iframe-а должен передаться токен и язык
    window.history.replaceState({}, '', '/?*=token&lang=en');
    runTest({
        archive: {
            selectedFile: {
                name: 'some-file.docx',
                path: 'some-file.docx',
                viewable: true
            },
            fileActions: {
                save: {
                    allow: true
                },
                download: {
                    allow: true
                }
            }
        }
    });
});
