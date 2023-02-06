import {
    setSegmentsPayloadCreator,
    upsertTransfersPayloadCreator,
    updatePricesPayloadCreator,
    setTariffsPayloadCreator,
} from '../payloadCreators';
import addTariffsToSegments from '../../../../lib/segments/addTariffsToSegments';
import updateSegments from '../../../../lib/segments/updateSegments';
import patchSegments from '../../../../lib/segments/patchSegments';

jest.mock('../../../../lib/segments/analyseSegments', () =>
    jest.fn(arr => arr),
);
jest.mock('../../../../lib/segments/patchSegments', () =>
    jest.fn(({segments}) => [...segments]),
);
jest.mock('../../../../lib/segments/updateSegments', () => jest.fn(arr => arr));
jest.mock('../../../../lib/segments/addTariffsToSegments', () =>
    jest.fn(arr => [...arr]),
);
jest.mock('../metaCreator', () => jest.fn(() => ({})));

const state = {
    search: {
        context: {},
        segments: [{}],
    },
};

describe('search payloadCreators', () => {
    describe('setSegmentsPayloadCreator', () => {
        it('should return empty array', () => {
            const segments = [];
            const result = setSegmentsPayloadCreator(segments, state);

            expect(result).toEqual([]);
            expect(patchSegments).toBeCalledWith(
                expect.objectContaining({
                    segments,
                    meta: state.search.context,
                }),
            );
        });

        it('should return changed segments array', () => {
            const segments = [{}];
            const result = setSegmentsPayloadCreator(segments, state);

            expect(result).not.toBe(segments);
            expect(result).toEqual([{}]);
            expect(patchSegments).toBeCalledWith(
                expect.objectContaining({
                    segments,
                    meta: state.search.context,
                }),
            );
        });
    });

    describe('setTransferSegmentsPayloadCreator', () => {
        it('should return segments from state', () => {
            const segments = [];
            const result = upsertTransfersPayloadCreator(segments, state);

            expect(result).toEqual([{}]);
            expect(patchSegments).toBeCalledWith(
                expect.objectContaining({
                    segments,
                    meta: state.search.context,
                }),
            );
        });

        it('should return merged segments array', () => {
            const segments = [{}];
            const result = upsertTransfersPayloadCreator(segments, state);

            expect(result).toEqual([{}, {}]);
            expect(patchSegments).toBeCalledWith(
                expect.objectContaining({
                    segments,
                    meta: state.search.context,
                }),
            );
        });
    });

    describe('updatePricesPayloadCreator', () => {
        it('should return merged segments array', () => {
            const payload = {segments: []};
            const result = updatePricesPayloadCreator(payload, state);

            expect(result).toEqual([{}]);
            expect(updateSegments).toBeCalledWith(
                state.search.segments,
                payload.segments,
                {},
            );
        });
    });

    describe('setTariffsPayloadCreator', () => {
        it('should return merged segments array', () => {
            const payload = [];
            const result = setTariffsPayloadCreator(payload, state);

            expect(result).toEqual(state.search.segments);
            expect(addTariffsToSegments).toBeCalledWith(
                state.search.segments,
                payload,
                {},
            );
        });
    });
});
