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
    describe('updateOptions', () => {
        it('update default options by segment with express', () => {
            const newOptions = express.updateOptions(
                express.getDefaultOptions(),
                segmentWithExpress,
            );

            expect(newOptions).toEqual({
                withExpress: true,
                withoutExpress: false,
            });
        });

        it('update default options by segment without express', () => {
            const options = express.updateOptions(
                express.getDefaultOptions(),
                segmentWithoutExpress,
            );

            expect(options).toEqual({
                withExpress: false,
                withoutExpress: true,
            });
        });

        it('update options (withExpress == true, withoutExpress == false) by segment with express', () => {
            const options = express.updateOptions(
                {withExpress: true, withoutExpress: false},
                segmentWithExpress,
            );

            expect(options).toEqual({
                withExpress: true,
                withoutExpress: false,
            });
        });

        it('update options (withExpress == false, withoutExpress == true) by segment with express', () => {
            const options = express.updateOptions(
                {withExpress: false, withoutExpress: true},
                segmentWithExpress,
            );

            expect(options).toEqual({
                withExpress: true,
                withoutExpress: true,
            });
        });

        it('update options (withExpress == true, withoutExpress == false) by segment without express', () => {
            const options = express.updateOptions(
                {withExpress: true, withoutExpress: false},
                segmentWithoutExpress,
            );

            expect(options).toEqual({
                withExpress: true,
                withoutExpress: true,
            });
        });

        it('update options (withExpress == false, withoutExpress == true) by segment without express', () => {
            const options = express.updateOptions(
                {withExpress: false, withoutExpress: true},
                segmentWithoutExpress,
            );

            expect(options).toEqual({
                withExpress: false,
                withoutExpress: true,
            });
        });

        it('update options (withExpress == true, withoutExpress == true) by segment with express', () => {
            const options = express.updateOptions(
                {withExpress: true, withoutExpress: true},
                segmentWithExpress,
            );

            expect(options).toEqual({
                withExpress: true,
                withoutExpress: true,
            });
        });

        it('update options (withExpress == true, withoutExpress == true) by segment without express', () => {
            const options = express.updateOptions(
                {withExpress: true, withoutExpress: true},
                segmentWithoutExpress,
            );

            expect(options).toEqual({
                withExpress: true,
                withoutExpress: true,
            });
        });
    });
});
