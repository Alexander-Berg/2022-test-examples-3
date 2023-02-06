import RenderMode from '../../../interfaces/RenderMode';

import getPreviewVector from '../getPreviewVector';

const vector = [9, 8, 7, 6, 5, 4, 3, 2, 1, 0];
const filteredSegmentIndices = [
    false,
    true,
    false,
    true,
    false,
    true,
    false,
    true,
    false,
    true,
];
const environment = {renderMode: RenderMode.light};
const data = {vector, filteredSegmentIndices, environment};

describe('test getPreviewVector function', () => {
    it('should return original vector if render mode is full', () => {
        const result = getPreviewVector({
            ...data,
            environment: {
                renderMode: RenderMode.full,
                segmentsLimitRender: 20,
            },
        });

        expect(result).toEqual(data.vector);
    });

    it('should return limited vector (limit less than data length)', () => {
        const result = getPreviewVector({
            ...data,
            environment: {
                ...data.environment,
                segmentsLimitRender: 3,
            },
        });

        expect(result).toEqual([9, 7, 5]);
    });

    it('should return limited vector (limit more than data length)', () => {
        const result = getPreviewVector({
            ...data,
            environment: {
                ...data.environment,
                segmentsLimitRender: 30,
            },
        });

        expect(result).toEqual([9, 7, 5, 3, 1]);
    });

    it('should return empty vector', () => {
        const result = getPreviewVector({
            ...data,
            environment: {
                ...data.environment,
                segmentsLimitRender: 0,
            },
        });

        expect(result).toEqual([]);
    });

    it('should return empty vector if all segments are hidden', () => {
        const result = getPreviewVector({
            ...data,
            filteredSegmentIndices: Array.from({length: 10}, () => false),
            environment: {
                ...data.environment,
                segmentsLimitRender: 5,
            },
        });

        expect(result).toEqual([]);
    });
});
