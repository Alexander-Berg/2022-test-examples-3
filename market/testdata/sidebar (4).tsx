import type {SidebarMenuMock} from '../../../utils/types';

export const managerSidebarScheme: SidebarMenuMock = {
    items: [
        {
            tankerCode: 'common.sidebar.manager:shops',
            expectedPage: 'market-partner:html:tpl-manager-partner-list:get',
            shownToRole: ['managerWriter', 'managerReader', 'customerManager', 'agencyManager', 'yaSuper'],
            shownToShop: [],
        },
    ],
};
