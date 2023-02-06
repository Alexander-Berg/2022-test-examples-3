import { IStructure } from '@yandex-turbo/components/LcJobsFilters';
import { FacetName } from '@yandex-turbo/types/jobs';

import { getLabel } from '../LcJobsFiltersWrapper-Tags.helper';

describe('LcJobsFiltersWrapper-Tags.helper', () => {
    describe('getLabel', () => {
        let defaultFacetParams;

        beforeEach(() => {
            defaultFacetParams = {
                type: 'modelmultiplechoice' as const,
                value: '',
            };
        });

        it('should find label of choice with passed value', () => {
            const facetName = FacetName.Skills;
            const value = 1024;
            const structure: IStructure = {
                [FacetName.Skills]: {
                    key: FacetName.Skills,
                    name: FacetName.Skills,
                    ...defaultFacetParams,
                    choices: [{ value, label: '123' }],
                },
            };

            expect(getLabel(facetName, value, structure)).toBe('123');
        });

        it('should extract choices from passed facet from structure', () => {
            const facetName = FacetName.ProLevels;
            const value = 1024;
            const structure: IStructure = {
                [FacetName.ProLevels]: {
                    key: FacetName.ProLevels,
                    name: FacetName.ProLevels,
                    ...defaultFacetParams,
                    choices: [{ value, label: '456' }],
                },
                [FacetName.Services]: {
                    key: FacetName.Services,
                    name: FacetName.Services,
                    ...defaultFacetParams,
                    choices: [{ value, label: '789' }],
                },
            };

            expect(getLabel(facetName, value, structure)).toBe('456');
        });

        it('should return null if choice with passed value is not found', () => {
            const facetName = FacetName.Skills;
            const value = 1024;
            const structure = {};

            expect(getLabel(facetName, value, structure)).toBe(null);
        });
    });
});
