import type {BackendHandler} from '../mocks/base/backendHandlers';

import blackboxResponse from './mockResponse/blackbox';
import accounts from './mockResponse/getAccounts.json';
import pageListSlim from './mockResponse/pageListSlim.json';
import popularDataSourceParams from './mockResponse/popularDataSourceParams.json';
import bunker from '../../../../.cache/bunker.json';

export const defaultBackendHandler: BackendHandler = (name, params) => {
    if (name === 'blackbox') {
        return blackboxResponse;
    }

    if (name === 'bunker.ls' && params.node === '/market-partner') {
        return [bunker];
    }

    if (name === 'bunker.ls') {
        return Promise.reject();
    }

    if (name === 'mbiPartner' && params.logName === 'getPartnerOwnerUid') {
        return {
            servant: 'market-payment',
            host: 'testing-market-mbi-partner-sas-2',
            version: '0',
            executingTime: '[0]',
            actions: '[contacts/euid]',
            result: 1444604344,
        };
    }

    if (name === 'mbiPartner.getDatasource') {
        return {
            info: {
                placementTypes: ['DROPSHIP'],
                id: 11103659,
                domain: 'Экспрессович',
                internalName: 'Экспрессович',
                managerId: -2,
            },
            manager: {
                id: -2,
                name: 'Служба Яндекс.Маркет',
                login: 'market-sales-manager',
                hosted: false,
                managerType: 'SUPPORT',
            },
        };
    }

    if (name === 'mbiPartner' && params.logName === 'getCampaignAccessCheck') {
        return {
            servant: 'market-payment',
            host: 'qpautmhil6a3yupk',
            version: '0',
            executingTime: '[0]',
            actions: '[campaigns/{campaignId}/check]',
            result: true,
        };
    }

    if (name === 'mbiPartner.getCampaignRoles') {
        return {
            userInfo: {marketOnly: 'false'},
            allowedRolesForCampaign: {role: {name: 'SHOP_ADMIN'}, campaignId: '1001410791'},
        };
    }

    if (name === 'mbiPartner.getCampaign') {
        return [
            {
                type: 'SUPPLIER',
                clientId: 1352190526,
                datasourceId: 11103659,
                tariffId: 1015,
                id: 1001410791,
                supplier: true,
                shop: false,
                crossBorder: false,
                fmcg: false,
                tpl: false,
                eatsAndLavka: false,
                sortingCenter: false,
                tplOutlet: false,
                tplPartner: false,
                tplCarrier: false,
                direct: false,
            },
        ];
    }

    if (name === 'mbiPartner' && params.logName === 'getBusinessIdByCampaignId') {
        return {
            servant: 'market-payment',
            host: 'testing-market-mbi-partner-sas-1',
            version: '0',
            executingTime: '[0]',
            actions: '[campaigns/businesses/id]',
            result: {businessId: 11103660},
        };
    }

    if (name === 'mbiPartner' && params.logName === 'getFeedsInfoByBusinessId') {
        return {
            servant: 'market-payment',
            host: 'testing-market-mbi-partner-sas-5',
            version: '0',
            executingTime: '[0]',
            actions: '[v3/business/{businessId}/feed/list]',
            result: {feeds: []},
        };
    }

    if (name === 'mbiPartner' && params.logName === 'getCurrentUserClientId') {
        return {
            servant: 'market-payment',
            host: 'xrtj72hvpxkajwys',
            version: '0',
            executingTime: '[0]',
            actions: '[contacts/clientId]',
            result: 1355082413,
        };
    }

    if (name === 'mbiPartner' && params.logName === 'getCurrencyRate') {
        return {
            ueCurrencyRates: [
                {currency: 'RUR', rate: '30', cnt: '1'},
                {currency: 'BYN', rate: '0.8', cnt: '1'},
                {currency: 'KZT', rate: '105', cnt: '1'},
            ],
        };
    }

    if (name === 'mbiPartner' && params.logName === 'getBusiness') {
        return {
            servant: 'market-payment',
            host: 'xrtj72hvpxkajwys',
            version: '0',
            executingTime: '[0]',
            actions: '[businesses/{businessId}]',
            result: {
                businessId: 11103660,
                name: 'Экспрессович',
                slug: 'ekspressovich',
                generalPlacementTypes: ['MARKETPLACE'],
            },
        };
    }

    if (name === 'mbiPartner' && params.logName === 'getBusinessPlacementTypes') {
        return {
            servant: 'market-payment',
            host: 'nhkc2nqjm7rfd3zk',
            version: '0',
            executingTime: '[0]',
            actions: '[businesses/{businessId}/placement-types]',
            result: ['FULFILLMENT', 'DROPSHIP'],
        };
    }

    if (name === 'cocon' && params.logName === 'pageListSlim') {
        return pageListSlim;
    }

    if (name === 'mbiPartner.logPartnerVisit') {
        return [];
    }

    if (name === 'passportMda.getAccounts') {
        return accounts;
    }

    if (name === 'mbiPartner.getAllowedActions') {
        return params.groups.split(',');
    }

    if (name === 'mbiPartner.getPopularDatasourceParams') {
        return popularDataSourceParams;
    }

    if (name === 'feedProcessor' && params.logName === 'getFeedsList') {
        return {};
    }

    return undefined;
};
