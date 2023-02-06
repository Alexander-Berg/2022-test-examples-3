import {MINUTE, SECOND} from 'utilities/dateUtils/constants';

import {
    EBusesBookOutcome,
    EBusesConfirmOutcome,
    EBusesRefundInfoOutcome,
    EBusesRefundOutcome,
    IBusesGetTestContextRequestParams,
} from 'server/api/BusesTravelApi/types/IBusesGetTestContext';

const initialValues: IBusesGetTestContextRequestParams = {
    bookOutcome: EBusesBookOutcome.BBO_SUCCESS,
    confirmOutcome: EBusesConfirmOutcome.BCO_SUCCESS,
    refundInfoOutcome: EBusesRefundInfoOutcome.BRIO_SUCCESS,
    refundOutcome: EBusesRefundOutcome.BRO_SUCCESS,
    expireAfterSeconds: (15 * MINUTE) / SECOND,
};

export default initialValues;
