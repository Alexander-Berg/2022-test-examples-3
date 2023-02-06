import '../../noscript';
import {
    getSelectFolderDialogDisableState
} from '../../../../components/redux/store/selectors/select-folder-dialog-disable';
import { STATE } from '../../../../components/redux/store/actions/dialogs';

const getStore = (dialogData, selected = []) => ({
    resources: {
        '/disk': {
            id: '/disk'
        },
        '/disk/folder': {
            id: '/disk/folder',
            parents: [{ id: '/disk' }]
        },
        '/disk/folder/subfolder': {
            id: '/disk/folder/subfolder',
            parents: [{ id: '/disk/folder' }, { id: '/disk' }]
        },
        '/disk/blocked': {
            id: '/disk/blocked',
            parents: [{ id: '/disk' }],
            state: {
                blocked: true
            }
        },
        '/disk/blocked/subfolder': {
            id: '/disk/blocked/subfolder',
            parents: [{ id: '/disk/blocked' }, { id: '/disk' }]
        },
        '/disk/readonly': {
            id: '/disk/readonly',
            parents: [{ id: '/disk' }],
            meta: {
                group: {
                    rights: 640
                }
            }
        },
        '/disk/readonly/subfolder': {
            id: '/disk/readonly/subfolder',
            parents: [{ id: '/disk/readonly' }, { id: '/disk' }]
        },
        '/disk/shared': {
            id: '/disk/shared',
            parents: [{ id: '/disk' }],
            meta: {
                group: {
                    gid: 1
                }
            }
        },
        '/disk/shared/subfolder': {
            id: '/disk/shared',
            parents: [{ id: '/disk/shared' }, { id: '/disk' }]
        },
        '/disk/shared2': {
            id: '/disk/shared2',
            parents: [{ id: '/disk' }],
            meta: {
                group: {
                    gid: 2,
                    is_root: true
                }
            }
        }
    },
    statesContext: {
        selected
    },
    dialogs: {
        selectFolder: {
            state: STATE.OPENED,
            data: dialogData
        }
    }
});

describe('getSelectFolderDialogDisableState', () => {
    it('должен вернуть `{ disabled: false }` при обычных условиях', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk'
        }))).toEqual({
            disabled: false
        });
    });

    it('должен вернуть `{ disabled: false }` если диалог выбора папки никогда не открывался', () => {
        expect(getSelectFolderDialogDisableState({
            dialogs: {},
            statesContext: {}
        })).toEqual({
            disabled: false
        });
    });

    it('должен вернуть `{ disabled: false }` если диалог выбора папки уже закрыт', () => {
        expect(getSelectFolderDialogDisableState({
            dialogs: {
                selectFolder: {
                    state: STATE.CLOSED
                }
            },
            statesContext: {}
        })).toEqual({
            disabled: false
        });
    });

    it('должен вернуть "папка заблокирована" для заблокированной папки', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/blocked'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_blocked_or_part')
        });
    });

    it('должен вернуть "папка заблокирована" для папки внутри заблокированной папки', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/blocked/subfolder'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_blocked_or_part')
        });
    });

    it('должен вернуть "недостаточно прав" для readonly папки', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/readonly'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_readonly')
        });
    });

    it('должен вернуть "недостаточно прав" для папки внутри readonly папки', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/readonly/subfolder'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_readonly')
        });
    });

    it('[перемещение] должен вернуть "Нельзя переместить папку внутри её самой" для папки, которую хотим перенести', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/folder',
            resourceId: '/disk/folder',
            type: 'move'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_move_choose_self')
        });
    });

    it('[перемещение] должен вернуть "Нельзя переместить папку внутри её самой" внутри папки, которую хотим перенести', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/folder/subfolder',
            resourceId: '/disk/folder',
            type: 'move'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_move_choose_self')
        });
    });

    it('[копирование] должен вернуть "Нельзя переместить папку внутри её самой" для папки, которую хотим копировать', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/folder',
            resourceId: '/disk/folder',
            type: 'copy'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_copy_choose_self')
        });
    });

    it('[копирование] должен вернуть "Нельзя переместить папку внутри её самой" внутри папки, которую хотим копировать', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/folder/subfolder',
            resourceId: '/disk/folder',
            type: 'copy'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_copy_choose_self')
        });
    });

    it('[создание обшей папки] должен вернуть "Нельзя создать общую папку внутри другой общей папки" при попытке создания общей папки в общей папке', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/shared',
            type: 'create-folder',
            creatingSharedFolder: true
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_create_shared_inside_shared')
        });
    });

    it('[создание обшей папки] должен вернуть "Нельзя создать общую папку внутри другой общей папки" при попытке создания общей папки в подпапке общей папке', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/shared/subfolder',
            type: 'create-folder',
            creatingSharedFolder: true
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_create_shared_inside_shared')
        });
    });

    it('[перемещение обшей папки] должен вернуть "Нельзя переместить общую папку в общую папку" при попытке перемещения общей папки в общую папку', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/shared',
            resourceId: '/disk/shared2',
            type: 'move'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_move_shared_to_shared')
        });
    });

    it('[перемещение обшей папки] должен вернуть "Нельзя переместить общую папку в общую папку" при попытке перемещения общей папки в подпапку общей папки', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk/shared/subfolder',
            resourceId: '/disk/shared2',
            type: 'move'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_move_shared_to_shared')
        });
    });

    it('[перемещение 1 объекта] должен вернуть "Перемещаемый объект уже в этой папке" для папки, в которой объект и так находится', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk',
            resourceId: '/disk/folder',
            type: 'move'
        }))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_move_inside_parent_one')
        });
    });

    it('[перемещение нескольких объектов] должен вернуть "Перемещаемые объекты уже в этой папке, выберите другую" для нескольких обектов, все из которых и так в этой папке', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk',
            type: 'move'
        }, ['/disk/folder', '/disk/shared']))).toEqual({
            disabled: true,
            text: i18n('%ufo_select_folder__disabled_move_inside_parent_many')
        });
    });

    it('[перемещение нескольких объектов] должен вернуть `{ disabled: false }` для нескольких обектов из разных папок', () => {
        expect(getSelectFolderDialogDisableState(getStore({
            folderId: '/disk',
            type: 'move'
        }, ['/disk/folder', '/disk/shared', '/disk/folder/subfolder']))).toEqual({
            disabled: false
        });
    });
});
