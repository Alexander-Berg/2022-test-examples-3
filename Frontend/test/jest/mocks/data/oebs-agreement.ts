import { getRawServiceMock, getRawUserMock, getServiceMock, getUserMock } from '~/test/jest/mocks/data/common';
import { RawOebsAgreement } from '~/src/features/Service/redux/types/requests';
import { OebsAgreement } from '~/src/features/Oebs/Oebs.types';

export const getOebsAgreementMock = (serviceId: number, data?: Partial<OebsAgreement>): OebsAgreement => ({
    action: 'action',
    attributes: {
        useForHr: true,
        useForProcurement: true,
        useForRevenue: true,
        useForHardware: true,
        useForGroup: true
    },
    id: 123,
    service: getServiceMock(serviceId),
    state: 'state',
    requester: getUserMock(),
    closeRequestId: null,
    createdAt: '2021-02-09T00:00:00.0Z',
    deleteRequestId: null,
    endDate: null,
    errorMessage: null,
    errorType: null,
    issue: null,
    moveRequestId: null,
    repairIssue: null,
    startDate: null,
    updatedAt: '2021-02-09T00:00:00.0Z',
    ...data,
});

export const getRawOebsAgreementMock = (serviceId: number, data?: Partial<RawOebsAgreement>): RawOebsAgreement => ({
    action: 'action',
    attributes: {
        use_for_hr: true,
        use_for_procurement: true,
        use_for_revenue: true,
        use_for_hardware: true,
        use_for_group_only: true
    },
    id: 123,
    service: getRawServiceMock(serviceId),
    state: 'state',
    requester: getRawUserMock(),
    close_request_id: null,
    created_at: '2021-02-09T00:00:00.0Z',
    delete_request_id: null,
    end_date: null,
    error_message: null,
    error_type: null,
    issue: null,
    move_request_id: null,
    repair_issue: null,
    start_date: null,
    updated_at: '2021-02-09T00:00:00.0Z',
    ...data,
});
