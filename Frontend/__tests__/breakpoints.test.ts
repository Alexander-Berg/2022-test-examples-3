import { defaultBreakpointKey, getCurrentPropsByBreakpoint, getCurrentBreakpoint, getBreakpointByDevice } from '../breakpoints';

describe('breakpoints utils', () => {
    describe('getCurrentPropsByBreakpoint()', () => {
        it('should return props by current breakpoint if they are set', () => {
            const bpProps = {
                [defaultBreakpointKey]: { foo: 'default' },
                '980': { foo: '980' },
                '768': { foo: '768' },
            };
            const currentBreakpoint = '980';

            expect(getCurrentPropsByBreakpoint(bpProps, currentBreakpoint)).toEqual({ foo: '980' });
        });

        it('should return props for breakpoint "980" when current breakpoint = 420 and there are no props for it', () => {
            const bpProps = {
                [defaultBreakpointKey]: { foo: 'default' },
                '980': { foo: '980' },
            };
            const currentBreakpoint = '420';

            expect(getCurrentPropsByBreakpoint(bpProps, currentBreakpoint)).toEqual({ foo: '980' });
        });

        it('should return default props when current breakpoint = 768 and there are no props for it', () => {
            const bpProps = {
                [defaultBreakpointKey]: { foo: 'default' },
            };
            const currentBreakpoint = '768';

            expect(getCurrentPropsByBreakpoint(bpProps, currentBreakpoint)).toEqual({ foo: 'desktop' });
        });

        it('should return default props when current breakpoint is not defined', () => {
            const bpProps = {
                [defaultBreakpointKey]: { foo: 'default' },
            };

            expect(getCurrentPropsByBreakpoint(bpProps)).toEqual({ foo: 'default' });
        });
    });

    describe('getCurrentBreakpoint()', () => {
        const breakpoints = [980, 768, 420];
        const testCases: [number, string][] = [
            [1200, defaultBreakpointKey],
            [900, '980'],
            [700, '768'],
            [320, '420'],
            [980, '980'],
        ];

        it.each(testCases)('should return breakpoint %b for %w', (width, expectedBreakpoint) => {
            expect(getCurrentBreakpoint(width, breakpoints)).toEqual(expectedBreakpoint);
        });
    });

    describe('getBreakpointByDevice()', () => {
        it('should return default breakpoint if there are no breakpoints', () => {
            expect(getBreakpointByDevice([], 'desktop')).toEqual(defaultBreakpointKey);
        });

        describe('device = "desktop"', () => {
            it('should return default breakpoint if there are no breakpoints for desktop', () => {
                expect(getBreakpointByDevice([420, 768], 'desktop')).toEqual(defaultBreakpointKey);
            });

            it('should return min desktop breakpoint if there are some breakpoints for desktop', () => {
                expect(getBreakpointByDevice([420, 768, 980, 1024], 'desktop')).toEqual('980');
            });
        });

        describe('device = "tablet"', () => {
            it('should return min tablet breakpoint if there are some breakpoints for tablet', () => {
                expect(getBreakpointByDevice([420, 768, 800, 980], 'tablet')).toEqual(768);
            });

            it('should return min desktop breakpoint if there are no breakpoints for tablet but there are breakpoint for desktop', () => {
                expect(getBreakpointByDevice([420, 980], 'tablet')).toEqual(980);
            });

            it('should return default breakpoint if there are no breakpoints for tablet or desktop', () => {
                expect(getBreakpointByDevice([420], 'tablet')).toEqual(defaultBreakpointKey);
            });
        });

        describe('device = "touch"', () => {
            it('should return min mobile breakpoint if there are some breakpoints for mobile', () => {
                expect(getBreakpointByDevice([420, 500, 768, 980], 'touch')).toEqual(420);
            });

            it('should return min tablet breakpoint if there are no breakpoints for mobile but there are some breakpoints for tablet', () => {
                expect(getBreakpointByDevice([768, 980], 'touch')).toEqual(768);
            });

            it('should return min desktop breakpoint if there are no breakpoints for mobile or tablet but there are some breakpoints for desktop', () => {
                expect(getBreakpointByDevice([980], 'touch')).toEqual(980);
            });
        });
    });
});
