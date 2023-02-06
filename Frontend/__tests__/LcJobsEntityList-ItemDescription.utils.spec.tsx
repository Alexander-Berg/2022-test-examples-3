import { sortByPriorityFirst } from '../LcJobsEntityList-ItemDescription.utils';
import { LcJobsEntityListCity } from '../../LcJobsEntityList.types';

let unsortedList: LcJobsEntityListCity[];

beforeEach(() => {
    unsortedList = [
        { priority: 10, slug: 'beru', name: 'Беру' },
        { priority: 64, slug: 'serp', name: 'Поисковик' },
        { priority: 14, slug: 'avia', name: 'Авиабилеты' },
        { priority: 50, slug: 'market', name: 'Маркет' },
        { priority: 51, slug: 'cloud', name: 'Cloud' },
    ];
});

describe('LcJobsEntityList-ItemDescription.utils', () => {
    describe('sortByPriority', () => {
        it('should sort by priority first', () => {
            const sortedList: LcJobsEntityListCity[] = [
                { priority: 64, slug: 'serp', name: 'Поисковик' },
                { priority: 51, slug: 'cloud', name: 'Cloud' },
                { priority: 50, slug: 'market', name: 'Маркет' },
                { priority: 14, slug: 'avia', name: 'Авиабилеты' },
                { priority: 10, slug: 'beru', name: 'Беру' },
            ];

            expect(sortByPriorityFirst(unsortedList)).toEqual(sortedList);
        });

        it('should sort by slug ascending if items have same priority', () => {
            const unsortedListSamePriority = unsortedList.map(
                item => ({ ...item, priority: 0 })
            );

            const sortedList: LcJobsEntityListCity[] = [
                { priority: 0, slug: 'avia', name: 'Авиабилеты' },
                { priority: 0, slug: 'beru', name: 'Беру' },
                { priority: 0, slug: 'cloud', name: 'Cloud' },
                { priority: 0, slug: 'market', name: 'Маркет' },
                { priority: 0, slug: 'serp', name: 'Поисковик' },
            ];

            expect(sortByPriorityFirst(unsortedListSamePriority)).toEqual(sortedList);
        });

        it('should sort by name ascending if items have same priority and slug', () => {
            const unsortedListSamePrioritySlug = unsortedList.map(
                item => ({ ...item, priority: 0, slug: '' })
            );

            const sortedList: LcJobsEntityListCity[] = [
                { priority: 0, slug: '', name: 'Cloud' },
                { priority: 0, slug: '', name: 'Авиабилеты' },
                { priority: 0, slug: '', name: 'Беру' },
                { priority: 0, slug: '', name: 'Маркет' },
                { priority: 0, slug: '', name: 'Поисковик' },
            ];

            expect(sortByPriorityFirst(unsortedListSamePrioritySlug)).toEqual(sortedList);
        });
    });
});
