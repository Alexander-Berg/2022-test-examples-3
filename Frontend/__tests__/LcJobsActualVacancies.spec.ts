import { animate, makeEaseOut, linear, scrollAnimation } from '../LcJobsActualVacancies.helpers';

describe('Actual vacancies animation', () => {
    let originalRequest = requestAnimationFrame;

    beforeEach(() => {
        // @ts-ignore
        requestAnimationFrame = jest.fn().mockImplementation(cb => cb(Date.now()));
    });

    afterEach(() => {
        // @ts-ignore
        requestAnimationFrame = originalRequest;
    });

    it('should scroll right for card width + gap', async() => {
        const contentRef = { current: { scrollLeft: 0 } };
        const direction = 'right';
        const vacancyWidth = 100;
        const vacancyGap = 20;
        const scrollLeft = contentRef.current.scrollLeft;

        animate({
            duration: 400,
            timing: makeEaseOut(linear),
            draw: scrollAnimation.bind(null, {
                contentRef,
                direction,
                vacancyGap,
                vacancyWidth,
                scrollLeft
            })
        });

        expect(contentRef.current.scrollLeft).toBe(120);
    });

    it('should scroll left for card width + gap', async() => {
        const contentRef = { current: { scrollLeft: 120 } };
        const direction = 'left';
        const vacancyWidth = 100;
        const vacancyGap = 20;
        const scrollLeft = contentRef.current.scrollLeft;

        animate({
            duration: 400,
            timing: makeEaseOut(linear),
            draw: scrollAnimation.bind(null, {
                contentRef,
                direction,
                vacancyGap,
                vacancyWidth,
                scrollLeft
            })
        });

        expect(contentRef.current.scrollLeft).toBe(0);
    });

    it('should do nothing if direction is left and scrollLeft is 0', async() => {
        const contentRef = { current: { scrollLeft: 0 } };
        const direction = 'left';
        const vacancyWidth = 100;
        const vacancyGap = 20;
        const scrollLeft = contentRef.current.scrollLeft;

        animate({
            duration: 400,
            timing: makeEaseOut(linear),
            draw: scrollAnimation.bind(null, {
                contentRef,
                direction,
                vacancyGap,
                vacancyWidth,
                scrollLeft
            })
        });

        expect(scrollLeft).toBe(0);
    });
});
