import { ServiceStore } from '~/src/features/Service/redux/types/store';
import { ServicePermissions } from '~/src/common/types/Service';
import { getServiceMock } from '~/test/jest/mocks/data/common';

export const getServicePermissionsMock = (data: ServicePermissions = []): ServicePermissions => ([
    'can_edit_oebs_flags',
    ...data,
]);

export const getServiceStoreMock = (serviceId: number, data?: Partial<ServiceStore>): ServiceStore => ({
    permissions: getServicePermissionsMock(),
    ...getServiceMock(serviceId),
    useForHr: false,
    useForProcurement: false,
    useForRevenue: false,
    oebsAgreement: null,
    ...data,
});
