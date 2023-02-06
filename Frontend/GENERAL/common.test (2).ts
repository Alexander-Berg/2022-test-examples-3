import { formatDayDuration } from './common';

const wrap = (suite: Function) => describe('helpers', () => { describe('date', () => { suite() }) });

wrap(() => describe('common', () => {
    it('should correct format day duration', () => {
        expect(JSON.stringify(formatDayDuration('03:20', '05:22')))
            .toBe(JSON.stringify({ hours: 21, minutes: 58 }));
    });
}));
