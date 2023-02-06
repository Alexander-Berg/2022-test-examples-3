import {
    BLOCK_RULES_KEY_UI_NEW,
    fast_tags2,
    SETTINGS_KEY_UI,
} from '../../components/InterfaceAdminConfig/adminConfigKeys';
import { GVARS_INTERFACE_ADMIN_PREFIX } from '../../constants';

const fastTags2 = [{
    'enabled': true,
    'tag': 'manual_hold',
    'place': ['car_card'],
    'alias': 'Блокировка для осмотра',
}, { 'enabled': true, 'tag': 'DTP', 'place': ['car_card'], 'alias': 'ДТП' }, {
    'enabled': true,
    'tag': 'DTP_registration',
    'place': ['car_card', 'client_card'],
    'alias': 'ДТП: оформление',
}];

const activeRole = 'interface-admin';
const passiveRole = 'interface-admin2';

const ui_settings = [
    {
        'ui': 'major/*',
        'active': true,
        'roles': `${activeRole},drive_team_dev_manager`,
        'ruleName': 'major',
        'id': 1567932662718,
    },
    {
        'ui': 'major2/*',
        'active': false,
        'roles': `${activeRole}`,
        'ruleName': 'major',
        'id': 1567932662718,
    },
    {
        'ui': 'settings/mds',
        'active': true,
        'roles': `${passiveRole},drive_team_mds_editor`,
        'ruleName': 'MDS',
        'id': 1584351698177,
    },
];

const blockRules = [
    {
        name: 'AvailableSearchPanel',
        'active': true,
        'description': 'Доступ к поиску',
        'env': ['admin', 'testing', 'prestable'],
        'roles': ['!porsche_admin'],
        'type': 'show',
    },
    {
        name: 'CallCenterOperator',
        'description': 'Оператор колл-центра',
        'type': 'show',
        'roles': ['qqqqqqq'],
        'env': ['admin', 'testing', 'prestable'],
        'active': true,
    },
    {
        name: 'Checking',
        'description': 'Права на проверку пользователя и кнопку «Документы new»',
        'type': 'show',
        'roles': ['interface-admin', 'lawn_appeal_document_manager', 'interface-support-operator-payment'],
        'env': ['admin', 'testing', 'prestable'],
        'active': true,
    },
];

const requestSettingsData = {
    settings: [
        {
            setting_key: `${GVARS_INTERFACE_ADMIN_PREFIX}.${fast_tags2}`,
            setting_value: JSON.stringify(fastTags2),
        },
        {
            setting_key: SETTINGS_KEY_UI,
            setting_value: JSON.stringify(ui_settings),
        },
        {
            setting_key: BLOCK_RULES_KEY_UI_NEW,
            setting_value: JSON.stringify(blockRules),
        },
    ],
};

const permissionsData = { a: 1 };
const userId = '1234567890';

const requestRolesData: any = [{
    role_description: {
        role_optional: '0',
        role_is_idm: '1',
        role_description: 'Полный доступ к интерфейсу',
        role_group: '0',
        role_groupping_tags: 'Adminka',
        role_id: passiveRole,
        role_is_public: '1',
    },
    user_role: {
        deadline: '18446744073709',
        active: '0',
        user_id: userId,
        role_id: passiveRole,
    },
},
{
    role_description: {
        role_optional: '0',
        role_is_idm: '1',
        role_description: 'Полный доступ к интерфейсу',
        role_group: '0',
        role_groupping_tags: 'Adminka',
        role_id: activeRole,
        role_is_public: '1',
    },
    user_role: {
        deadline: '18446744073709',
        active: '1',
        user_id: userId,
        role_id: activeRole,
    },
}];

export {
    passiveRole
    , activeRole
    , blockRules
    , fastTags2
    , ui_settings
    , requestSettingsData
    , permissionsData
    , requestRolesData
    , userId,
};
