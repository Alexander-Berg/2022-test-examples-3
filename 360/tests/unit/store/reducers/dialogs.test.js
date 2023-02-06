import '../../noscript';
import dialogsReducer from '../../../../components/redux/store/reducers/dialogs';
import { DESTROY_RESOURCE } from '../../../../components/redux/store/actions/types';
import { STATE } from '../../../../components/redux/store/actions/dialogs';

describe('dialogs reducer', () => {
    describe('DESTROY_RESOURCE', () => {
        it('должен сбросить folderId в открытом selectFolder если folderId - пропавшая папка', () => {
            expect(dialogsReducer({
                selectFolder: {
                    state: STATE.OPENED,
                    data: {
                        folderId: '/disk/folder'
                    }
                }
            }, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/folder'
                }
            })).toEqual({
                selectFolder: {
                    state: STATE.OPENED,
                    data: {
                        folderId: '/disk'
                    }
                }
            });
        });

        it('должен сбросить folderId в открытом selectFolder если folderId - вложенная папка пропавшей папки', () => {
            expect(dialogsReducer({
                selectFolder: {
                    state: STATE.OPENED,
                    data: {
                        folderId: '/disk/folder/subfolder'
                    }
                }
            }, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/folder'
                }
            })).toEqual({
                selectFolder: {
                    state: STATE.OPENED,
                    data: {
                        folderId: '/disk'
                    }
                }
            });
        });

        it('не должен менять state если открыта другая папка', () => {
            const state = {
                selectFolder: {
                    state: STATE.OPENED,
                    data: {
                        folderId: '/disk/folder2'
                    }
                }
            };
            expect(dialogsReducer(state, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/folder'
                }
            })).toBe(state);
        });

        it('не должен менять state если диалог уже закрыт', () => {
            const state = {
                selectFolder: {
                    state: STATE.CLOSED,
                    data: {
                        folderId: '/disk/folder'
                    }
                }
            };
            expect(dialogsReducer(state, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/folder'
                }
            })).toBe(state);
        });
    });
});
