import {TransportType} from '../../transportType';
import ISegment from '../../../interfaces/segment/ISegment';
import ITransferSegment from '../../../interfaces/transfer/ITransferSegment';

import isCppk from '../isCppk';

const CPPK_ID = 153;

const trainCppkSegment = {
    company: {id: CPPK_ID},
    transport: {code: TransportType.train},
} as ISegment | ITransferSegment;

const trainNotCppkSegment = {
    company: {id: 1},
    transport: {code: TransportType.train},
} as ISegment | ITransferSegment;

const suburbanCppkSegment = {
    company: {id: CPPK_ID},
    transport: {code: TransportType.suburban},
    trainPurchaseNumbers: ['882M'],
} as ISegment | ITransferSegment;

describe('isCppk', () => {
    it('Поезд-ЦППК, вернется true', () => {
        expect(isCppk(trainCppkSegment)).toBe(true);
    });

    it('Электричка-ЦППК, вернется true', () => {
        expect(isCppk(suburbanCppkSegment)).toBe(true);
    });

    it('Поезд не ЦППК, вернется false', () => {
        expect(isCppk(trainNotCppkSegment)).toBe(false);
    });
});
