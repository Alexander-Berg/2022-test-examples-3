import Listing from 'components/archive/listing';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import _ from 'lodash';
import { Provider } from 'react-redux';

const runTest = (ownState) => {
    global.LANG = 'ru';
    const state = {
        cfg: {
            ua: {
                OSFamily: ''
            }
        },
        doc: {
            archivePath: ''
        },
        archive: {
            listing: {
                folder: [{
                    name: 'folder-1',
                    path: 'folder-1',
                    folder: [{
                        name: 'nested-folder-1',
                        path: 'folder-1/nested-folder-1',
                        file: [{
                            name: 'file-in-nested-folder-1.ppt',
                            path: 'folder-1/nested-folder-1/file-in-nested-folder-1.ppt',
                            viewable: true
                        }]
                    }, {
                        name: 'nested-folder-2 (empty)',
                        path: 'folder-1/nested-folder-2 (empty)'
                    }],
                    file: [{
                        name: 'file-in-folder-1 (encrypted).txt',
                        path: 'folder-1/file-in-folder-1 (encrypted).txt',
                        viewable: true,
                        encrypted: true
                    }]
                }, {
                    name: 'folder-2 (empty)',
                    path: 'folder-2 (empty)'
                }],
                file: [{
                    name: 'file-in-1.doc',
                    path: 'file-in-1.doc',
                    viewable: true
                }, {
                    name: 'file-with-long-extension.extension',
                    path: 'file-with-long-extension.extension'
                }, {
                    name: 'file-with-no-extension',
                    path: 'file-with-no-extension'
                }]
            },
            path: '',
            selectedFile: {
                name: '',
                path: '',
                viewable: false,
                encrypted: false
            },
            fileActions: {
                save: {
                    allow: false,
                    progressing: {}
                },
                download: {
                    allow: false,
                    progressing: {}
                }
            }
        }
    };
    const store = init(_.merge({}, state, ownState));
    const component = render(
        <Provider store={store}>
            <Listing />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

it('listing simple test', () => {
    runTest({});
});

it('listing for folder-1', () => {
    runTest({
        archive: {
            path: 'folder-1'
        }
    });
});

it('listing for nested empty folder', () => {
    runTest({
        archive: {
            path: 'folder-1/nested-folder-2 (empty)'
        }
    });
});

it('listing for folder-1 and incorrect selected file (renders as no selected)', () => {
    runTest({
        archive: {
            path: 'folder-1',
            selectedFile: {
                name: 'file-in-1.doc',
                path: 'file-in-1.doc',
                viewable: true
            }
        }
    });
});

it('listing with selected file with only download action', () => {
    runTest({
        archive: {
            selectedFile: {
                name: 'file-in-1.doc',
                path: 'file-in-1.doc',
                viewable: true
            },
            fileActions: {
                download: {
                    allow: true,
                    progressing: {}
                }
            }
        }
    });
});

it('listing with selected file with only save action', () => {
    runTest({
        archive: {
            selectedFile: {
                name: 'file-in-1.doc',
                path: 'file-in-1.doc',
                viewable: true
            },
            fileActions: {
                save: {
                    allow: true,
                    progressing: {}
                }
            }
        }
    });
});

it('listing with selected file with both actions', () => {
    runTest({
        archive: {
            path: 'folder-1/nested-folder-1',
            selectedFile: {
                name: 'file-in-nested-folder-1.ppt',
                path: 'folder-1/nested-folder-1/file-in-nested-folder-1.ppt',
                viewable: true
            },
            fileActions: {
                save: {
                    allow: true,
                    progressing: {}
                },
                download: {
                    allow: true,
                    progressing: {}
                }
            }
        }
    });
});

it('listing with selected encrypted file (no action bar for encrypted)', () => {
    runTest({
        archive: {
            path: 'folder-1',
            selectedFile: {
                name: 'file-in-folder-1 (encrypted).txt',
                path: 'folder-1/file-in-folder-1 (encrypted).txt',
                viewable: true,
                encrypted: true
            },
            fileActions: {
                save: {
                    allow: true,
                    progressing: {}
                },
                download: {
                    allow: true,
                    progressing: {}
                }
            }
        }
    });
});

it('listing with incorrect path', () => {
    runTest({
        archive: {
            path: 'folder-2 (empty)/no-such-folder'
        }
    });
});

it('listing for iOS', () => {
    runTest({
        url: {
            token: 'some-token'
        },
        cfg: {
            ua: {
                OSFamily: 'iOS'
            }
        }
    });
});
