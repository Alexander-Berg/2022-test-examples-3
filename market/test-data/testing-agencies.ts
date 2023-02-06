import users from './users';

export default {
    'autotest-subcl-shop-in-msk': {
        campaignId: 1000605432,
        shopId: 10299396,
        contacts: {
            agency: users['autotest-agency-with-manager'],
            subclient: users['autotest-subcl-with-manager'],
        },
    },

    'autotest-shop-subcl-no-mng-msk': {
        campaignId: 1000605433,
        shopId: 10299397,
        contacts: {
            agency: users['autotest-agency-wtht-manager'],
            subclient: users['autotst-subcl-no-mngr'],
        },
    },

    'autotest-subcl-shop-in-msk-2': {
        campaignId: 1000611520,
        shopId: 10299380,
        contacts: {
            agency: users['autotest-agency-with-manager'],
            subclient: users['autotest-subcl-with-manager'],
        },
    },

    'market-seminar.webasyst.net': {
        campaignId: 1037133,
        shopId: 37133,
        contacts: {
            owner: users.testagency,
            agency: users.testagency,
        },
    },

    'market-seminar133.webasyst.net': {
        campaignId: 1052469,
        shopId: 52469,
        contacts: {
            agency: users.testagency,
        },
    },

    'sub-market1.narod.ru': {
        campaignId: 1053120,
        shopId: 53120,
        contacts: {
            agency: users.testagency,
        },
    },
    'autotest-questions.ru': {
        campaignId: 1000611795,
        shopId: 10286272,
        contacts: {
            agency: users['autotest-agency-with-manager'],
        },
    },
};
