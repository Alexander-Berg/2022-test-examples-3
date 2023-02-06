import { defaultBreakpointKey, getCurrentPropsByBreakpoint, getCurrentBreakpoint } from '../breakpoints';

describe('breakpoints utils', () => {
    describe('getCurrentPropsByBreakpoint()', () => {
        it('should return props by current breakpoint if they set', () => {
            const bpProps = {
                [defaultBreakpointKey]: { foo: 'default' },
                '980': { foo: '980' },
                '768': { foo: '768' },
            };
            const currentBreakpoint = '980';

            expect(getCurrentPropsByBreakpoint(bpProps, currentBreakpoint)).toEqual({ foo: '980' });
        });

        it('should return default props when current breakpoint = 980 and there are no props for it', () => {
            const bpProps = {
                [defaultBreakpointKey]: { foo: 'default' },
            };
            const currentBreakpoint = '980';

            expect(getCurrentPropsByBreakpoint(bpProps, currentBreakpoint)).toEqual({ foo: 'default' });
        });

        it('should return props for breakpoint "980" when current breakpoint = 420 and there are no props for it', () => {
            const bpProps = {
                [defaultBreakpointKey]: { foo: 'desktop' },
                '980': { foo: '980' },
            };
            const currentBreakpoint = '420';

            expect(getCurrentPropsByBreakpoint(bpProps, currentBreakpoint)).toEqual({ foo: '980' });
        });

        it('should return default props when current breakpoint = 768 and there are no props for it', () => {
            const bpProps = {
                [defaultBreakpointKey]: { foo: 'desktop' },
            };
            const currentBreakpoint = '768';

            expect(getCurrentPropsByBreakpoint(bpProps, currentBreakpoint)).toEqual({ foo: 'desktop' });
        });
    });

    describe('getCurrentBreakpoint()', () => {
        const breakpoints = [980, 768, 420];

        it('should return default breakpoint when width = 1200', () => {
            expect(getCurrentBreakpoint(1200, breakpoints)).toEqual(defaultBreakpointKey);
        });

        it('should return breakpoint "980" when width = 900', () => {
            expect(getCurrentBreakpoint(900, breakpoints)).toEqual('980');
        });

        it('should return breakpoint "768" when width = 700', () => {
            expect(getCurrentBreakpoint(700, breakpoints)).toEqual('768');
        });

        it('should return breakpoint "420" when width = 320', () => {
            expect(getCurrentBreakpoint(320, breakpoints)).toEqual('420');
        });

        it('should return breakpoint "980" when width = 980', () => {
            expect(getCurrentBreakpoint(980, breakpoints)).toEqual('980');
        });
    });
});
