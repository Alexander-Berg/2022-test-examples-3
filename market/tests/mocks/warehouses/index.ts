import type {BusinessWarehousesList} from '~/app/bcm/mbiPartner/Client/BusinessWarehouseClient/types';
import {ShipmentType, WarehouseStatus, MarketStatus, Warehouse, WarehouseType} from '~/app/entities/warehouse/types';
import {ProgramStatus, TestingState} from '~/app/entities/program/types';
import {OrgType} from '~/app/entities/partner/types';

export const getWarehouse = (draft?: Partial<Warehouse>): Warehouse => ({
    partnerId: 1,
    warehouseType: WarehouseType.Dropship,
    externalId: '6c0d0ce6-f679',
    address: 'Новинский бульвар, 8с1, Москва, Москва, 121099',
    shipmentType: ShipmentType.Express,
    settlement: 'Москва',
    campaignId: 2,
    marketStatus: MarketStatus.On,
    id: 51449,
    name: 'Тест склад',
    programStatus: {
        program: 'marketplace',
        status: ProgramStatus.Full,
        isEnabled: true,
        subStatuses: [],
        needTestingState: TestingState.NotRequired,
        newbie: false,
    },
    status: WarehouseStatus.Enabled,
    region: {
        id: 213,
        name: 'Москва',
    },
    legalName: 'Интернет решения',
    orgType: OrgType.Chp,
    ...draft,
});

export const getWarehousesList = (warehouses: Warehouse[]): BusinessWarehousesList => ({
    paging: {},
    warehouses,
});
