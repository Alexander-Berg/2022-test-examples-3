import express from '../../express';

const segmentWithExpress = {
    title: 'Moscow - Ryazan',
    thread: {
        isExpress: true,
    },
};

const segmentWithoutExpress = {
    title: 'Moscow - Ryazan',
    thread: {
        isExpress: false,
    },
};

describe('express', () => {
    describe('apply', () => {
        it('show segments with express only, segment with express', () => {
            const result = express.apply(true, segmentWithExpress);

            expect(result).toBe(true);
        });

        it('show segments with express only, segment without express', () => {
            const result = express.apply(true, segmentWithoutExpress);

            expect(result).toBe(false);
        });

        it('show all segments, segment with express', () => {
            const result = express.apply(false, segmentWithExpress);

            expect(result).toBe(true);
        });

        it('show all segments, segment without express', () => {
            const result = express.apply(false, segmentWithoutExpress);

            expect(result).toBe(true);
        });
    });
});
