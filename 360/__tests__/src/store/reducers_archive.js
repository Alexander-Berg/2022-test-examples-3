import * as actions from 'store/actions';
import * as consts from 'store/consts';
import init from 'store';

let store = null;
const resetStore = () => {
    store = init();
};

beforeEach(() => {
    resetStore();
});

describe('archive_reducer', () => {
    it('UPDATE_ARCHIVE', () => {
        expect(store.getState().archive).toMatchSnapshot();

        const archive = {
            state: 'READY',
            listing: {
                folder: [{
                    name: 'root_folder_1',
                    path: 'root_folder_1',
                    folder: [{
                        name: 'folder-in-folder',
                        path: 'root_folder_1/folder-in-folder',
                        file: [{
                            name: 'file-in-folder-in-folder.ppc',
                            path: 'root_folder_1/folder-in-folder/file-in-folder-in-folder.ppc',
                            unknownProp: 'unknownValue'
                        }]
                    }],
                    file: [{
                        name: 'file-in-folder.txt',
                        path: 'root_folder_1/file-in-folder.txt',
                        viewable: true
                    }]
                }],
                file: [{
                    name: 'root_file_1.ext',
                    path: 'root_file_1.ext',
                    viewable: true
                }, {
                    name: 'root_file_2.ext',
                    path: 'root_file_2.ext',
                    viewable: false,
                    encrypted: true
                }]
            },
            nestedCount: 999
        };
        store.dispatch({
            type: consts.actions.UPDATE_ARCHIVE,
            archive
        });
        expect(store.getState().archive).toMatchSnapshot();
    });

    it('updateArchivePath', () => {
        expect(store.getState().archive.path).toEqual('');
        const newPath = 'some/new/path';
        store.dispatch(actions.updateArchivePath(newPath));
        expect(store.getState().archive.path).toEqual(newPath);
    });

    it('updateArchiveSelectedFile', () => {
        expect(store.getState().archive.selectedFile).toMatchSnapshot();
        store.dispatch(actions.updateArchiveSelectedFile({
            name: 'selected-file-name.ext',
            path: 'path-to-selected/selected-file-name.ext',
            viewable: true,
            encrypted: true
        }));
        expect(store.getState().archive.selectedFile).toMatchSnapshot();
    });

    it('updateArchiveAction', () => {
        expect(store.getState().archive.fileActions).toMatchSnapshot();

        store.dispatch(actions.updateArchiveAction('download', { allow: true }));
        expect(store.getState().archive.fileActions).toMatchSnapshot();

        store.dispatch(actions.updateArchiveAction('save', {
            allow: true,
            state: 'READY',
            name: 'saved-file-name.extension',
            folderUrl: '/path/to/downloads'
        }));
        expect(store.getState().archive.fileActions).toMatchSnapshot();
    });
});
