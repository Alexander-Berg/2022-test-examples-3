import ISegment from '../../../../interfaces/segment/ISegment';
import ITransfer from '../../../../interfaces/transfer/ITransfer';
import {TransportType} from '../../../transportType';
import SegmentSubtypeCode from '../../../../interfaces/segment/SegmentSubtypeCode';

import lastochka from '../../lastochka';

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

describe('lastochka', () => {
    describe('apply', () => {
        it('Показывает сегменты только если они ласточка, поезд-ласточка', () => {
            const result = lastochka.apply(true, trainSegmentLastochka);

            expect(result).toBe(true);
        });

        it('Показывает сегменты только если они ласточка, поезд-обычный', () => {
            const result = lastochka.apply(true, trainSegmentNormal);

            expect(result).toBe(false);
        });

        it('Показывает сегменты только если они ласточка, поезд-пересадка', () => {
            const result = lastochka.apply(true, trainTransfer);

            expect(result).toBe(false);
        });

        it('Показывает сегменты только если они ласточка, электричка-ласточка', () => {
            const result = lastochka.apply(true, suburbanSegmentLastochka);

            expect(result).toBe(true);
        });

        it('Показывает сегменты только если они ласточка, электричка-обычная', () => {
            const result = lastochka.apply(true, suburbanSegmentNormal);

            expect(result).toBe(false);
        });

        it('Показывает все сегменты, поезд-ласточка', () => {
            const result = lastochka.apply(false, trainSegmentLastochka);

            expect(result).toBe(true);
        });

        it('Показывает все сегменты, поезд-обычный', () => {
            const result = lastochka.apply(false, trainSegmentNormal);

            expect(result).toBe(true);
        });

        it('Показывает все сегменты, поезд-пересадка', () => {
            const result = lastochka.apply(false, trainTransfer);

            expect(result).toBe(true);
        });
    });
});
