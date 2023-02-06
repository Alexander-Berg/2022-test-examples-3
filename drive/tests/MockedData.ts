import { _CHATS, ADDITIONAL_CONTROL_TYPES } from '../../types';

export const MENU_DATA = {
    data: [_CHATS.MY, {
        info: { totalItems: 10 },
        items: [
            {
                display_name: 'Тасочка',
                name: 'task',
                chats_count: 10,
            },
            {
                display_name: 'Тасочка1',
                name: 'task1',
                chats_count: 5,
            }],
    }],
    selectedItems: {
        menuItem: null,
        subMenuItem: null,
    },
    onMenuClick: () => {},
    onFavClick: () => {},
};

export const SELECTED_ITEMS_EXAMPLE = {
    menuItem: _CHATS.MY,
    submenuItem: 'task',
};

const UPDATE_BUTTON = {
    type: ADDITIONAL_CONTROL_TYPES.UPDATE_BUTTON,
    onClick: () => {},
    state: { isLoading: false },
};

export const LINES_LIST_DATA = {
    linesListData: [MENU_DATA],
    selectedItems: SELECTED_ITEMS_EXAMPLE,
    onSelectLine: () => {},
    additionalControls: [UPDATE_BUTTON],
    additionalMenus: [],
    openFavouritesConfirm: () => {},
};

export const ARCHIVE_MENU = {
    type: _CHATS.ARCHIVE,
    title: 'Архив',
    onClick: () => {},
};
