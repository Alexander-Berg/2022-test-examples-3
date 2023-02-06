import SegmentSubtypeCode from '../../../../interfaces/segment/SegmentSubtypeCode';
import {TransportType} from '../../../transportType';
import ISegment from '../../../../interfaces/segment/ISegment';
import ITransfer from '../../../../interfaces/transfer/ITransfer';

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

describe('lastochka', () => {
    describe('updateOptions', () => {
        it('Обновить дефолтные опции, сегмент-ласточка', () => {
            const newOptions = lastochka.updateOptions(
                lastochka.getDefaultOptions(),
                trainSegmentLastochka,
            );

            expect(newOptions).toEqual({
                withLastochka: true,
                withoutLastochka: false,
            });
        });

        it('Обновить дефолтные опции, сегмент-обычный', () => {
            const options = lastochka.updateOptions(
                lastochka.getDefaultOptions(),
                trainSegmentNormal,
            );

            expect(options).toEqual({
                withLastochka: false,
                withoutLastochka: true,
            });
        });

        it('Обновить дефолтные опции, сегмент-трансфер', () => {
            const options = lastochka.updateOptions(
                lastochka.getDefaultOptions(),
                trainTransfer,
            );

            expect(options).toEqual({
                withLastochka: false,
                withoutLastochka: true,
            });
        });

        it('Обновить опции (withLastochka == true, withoutLastochka == false), сегмент-ласточка', () => {
            const options = lastochka.updateOptions(
                {withLastochka: true, withoutLastochka: false},
                trainSegmentLastochka,
            );

            expect(options).toEqual({
                withLastochka: true,
                withoutLastochka: false,
            });
        });

        it('Обновить опции (withLastochka == false, withoutLastochka == true), сегмент-ласточка', () => {
            const options = lastochka.updateOptions(
                {withLastochka: false, withoutLastochka: true},
                trainSegmentLastochka,
            );

            expect(options).toEqual({
                withLastochka: true,
                withoutLastochka: true,
            });
        });

        it('Обновить опции (withLastochka == true, withoutLastochka == false), сегмент-обычный', () => {
            const options = lastochka.updateOptions(
                {withLastochka: true, withoutLastochka: false},
                trainSegmentNormal,
            );

            expect(options).toEqual({
                withLastochka: true,
                withoutLastochka: true,
            });
        });

        it('Обновить опции (withLastochka == false, withoutLastochka == true), сегмент-обычный', () => {
            const options = lastochka.updateOptions(
                {withLastochka: false, withoutLastochka: true},
                trainSegmentNormal,
            );

            expect(options).toEqual({
                withLastochka: false,
                withoutLastochka: true,
            });
        });

        it('Обновить опции (withLastochka == true, withoutLastochka == false), сегмент-трансфер', () => {
            const options = lastochka.updateOptions(
                {withLastochka: true, withoutLastochka: false},
                trainTransfer,
            );

            expect(options).toEqual({
                withLastochka: true,
                withoutLastochka: true,
            });
        });

        it('Обновить опции (withLastochka == false, withoutLastochka == true), сегмент-трансфер', () => {
            const options = lastochka.updateOptions(
                {withLastochka: false, withoutLastochka: true},
                trainTransfer,
            );

            expect(options).toEqual({
                withLastochka: false,
                withoutLastochka: true,
            });
        });

        it('Обновить опции (withLastochka == true, withoutLastochka == true), сегмент-ласточка', () => {
            const options = lastochka.updateOptions(
                {withLastochka: true, withoutLastochka: true},
                trainSegmentLastochka,
            );

            expect(options).toEqual({
                withLastochka: true,
                withoutLastochka: true,
            });
        });

        it('Обновить опции (withLastochka == true, withoutLastochka == true), сегмент-обычный', () => {
            const options = lastochka.updateOptions(
                {withLastochka: true, withoutLastochka: true},
                trainSegmentNormal,
            );

            expect(options).toEqual({
                withLastochka: true,
                withoutLastochka: true,
            });
        });

        it('Обновить опции (withLastochka == true, withoutLastochka == true), сегмент-трансфер', () => {
            const options = lastochka.updateOptions(
                {withLastochka: true, withoutLastochka: true},
                trainTransfer,
            );

            expect(options).toEqual({
                withLastochka: true,
                withoutLastochka: true,
            });
        });
    });
});
