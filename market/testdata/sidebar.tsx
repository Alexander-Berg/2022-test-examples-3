import type {SidebarMenuMock} from '../../../utils/types';

const urlParams = {platformType: 'manager'};

export const managerSidebarScheme: SidebarMenuMock = {
    items: [
        {
            tankerCode: 'common.sidebar.manager:shops',
            expectedPage: 'market-partner:html:manager-partner-list:get',
            shownToRole: ['managerWriter', 'managerReader', 'customerManager', 'agencyManager', 'yaSuper'],
            shownToShop: [],
        },
        {
            tankerCode: 'common.sidebar.manager:api-logs',
            expectedPage: 'market-partner:html:manager-api-log:get',
            shownToRole: ['managerWriter', 'managerReader', 'customerManager', 'agencyManager', 'yaSuper'],
            shownToShop: [],
            urlParams,
        },

        {
            tankerCode: 'common.sidebar.manager:manage-shops',
            shownToRole: ['managerWriter', 'customerManager', 'agencyManager', 'yaSuper'],
            shownToShop: [],
            items: [
                {
                    tankerCode: 'common.sidebar.manager:change-shop-manager',
                    expectedPage: 'market-partner:html:manager-partner-list-change-manager:get',
                    shownToRole: ['managerWriter', 'customerManager', 'agencyManager', 'yaSuper'],
                    shownToShop: [],
                    urlParams,
                },
                {
                    tankerCode: 'common.sidebar.manager:notes-search',
                    expectedPage: 'market-partner:html:manager-notes-search:get',
                    shownToRole: ['managerWriter', 'customerManager', 'agencyManager', 'yaSuper'],
                    shownToShop: [],
                    urlParams,
                },
            ],
        },
        {
            tankerCode: 'common.sidebar.manager:notes-search',
            expectedPage: 'market-partner:html:manager-notes-search:get',
            shownToRole: ['managerReader'],
            shownToShop: [],
            urlParams,
        },
        {
            tankerCode: 'common.sidebar.manager:manage-agencies',
            shownToRole: ['managerWriter', 'managerReader', 'customerManager', 'agencyManager', 'yaSuper'],
            shownToShop: [],
            items: [
                {
                    tankerCode: 'common.sidebar.manager:add-agency',
                    expectedPage: 'market-partner:html:manager-agency-add:get',
                    shownToRole: ['managerWriter', 'managerReader', 'customerManager', 'agencyManager', 'yaSuper'],
                    shownToShop: [],
                    urlParams,
                },
                {
                    tankerCode: 'common.sidebar.manager:change-agency-manager',
                    expectedPage: 'market-partner:html:manager-agency-list:get',
                    shownToRole: ['managerWriter', 'managerReader', 'customerManager', 'agencyManager', 'yaSuper'],
                    shownToShop: [],
                    urlParams,
                },
            ],
        },
        {
            tankerCode: 'common.sidebar.manager:health-jobs',
            expectedPage: 'market-partner:html:health-jobs:get',
            shownToRole: ['managerWriter', 'managerReader', 'customerManager', 'agencyManager', 'yaSuper'],
            shownToShop: [],
            urlParams,
        },
    ],
};
