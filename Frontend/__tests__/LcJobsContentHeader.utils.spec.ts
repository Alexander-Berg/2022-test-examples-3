import { getActiveTab } from '../LcJobsContentHeader.utils';
import { LcJobsContentHeaderTab } from '../LcJobsContentHeader.types';

describe('LcJobsContentHeader.utils', () => {
    describe('getActiveTab', () => {
        it('should return null if passed empty tabs array', () => {
            expect(getActiveTab([])).toBe(null);
        });

        it('should return a first tab if tags have not active one', () => {
            const activeTab = { title: 'example1', id: 'example1' };

            const tabs: LcJobsContentHeaderTab[] = [
                activeTab,
                { title: 'example2', id: 'example2' },
            ];

            expect(getActiveTab(tabs)).toBe(activeTab);
        });

        it('should return an active tab if found', () => {
            const activeTab = { title: 'example2', id: 'example2', active: true };

            const tabs: LcJobsContentHeaderTab[] = [
                { title: 'example1', id: 'example1' },
                activeTab,
                { title: 'example3', id: 'example3', active: false },
            ];

            expect(getActiveTab(tabs)).toBe(activeTab);
        });
    });
});
