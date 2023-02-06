import {TransportType} from '../../transportType';
import ISegment from '../../../interfaces/segment/ISegment';
import SegmentSubtypeCode from '../../../interfaces/segment/SegmentSubtypeCode';
import ITransfer from '../../../interfaces/transfer/ITransfer';

import isLastochka from '../isLastochka';

const trainSegmentNormal = {
    transport: {
        code: TransportType.train,
    },
} as ISegment;
const trainSegmentLastochka = {
    transport: {
        code: TransportType.train,
        subtype: {
            code: SegmentSubtypeCode.lastochkaDal,
        },
    },
} as ISegment;

const trainTransfer = {
    transport: {
        code: TransportType.train,
    },
    isTransfer: true,
} as ITransfer;

const suburbanSegmentNormal = {
    transport: {
        code: TransportType.suburban,
    },
} as ISegment;
const suburbanSegmentLastochka = {
    transport: {
        code: TransportType.suburban,
        subtype: {
            code: SegmentSubtypeCode.lastochka,
        },
    },
} as ISegment;

describe('isLastochka', () => {
    it('Поезд-ласточка, должен вернуть true', () => {
        expect(isLastochka(trainSegmentLastochka)).toBe(true);
    });

    it('Поезд-обычный, должен вернуть false', () => {
        expect(isLastochka(trainSegmentNormal)).toBe(false);
    });

    it('Поезд-пересадка, должен вернуть false', () => {
        expect(isLastochka(trainTransfer)).toBe(false);
    });

    it('Электричка-ласточка, должен вернуть true', () => {
        expect(isLastochka(suburbanSegmentLastochka)).toBe(true);
    });

    it('Электричка-обычная, должен вернуть false', () => {
        expect(isLastochka(suburbanSegmentNormal)).toBe(false);
    });
});
