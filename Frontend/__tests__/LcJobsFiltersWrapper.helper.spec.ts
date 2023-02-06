import { get } from 'lodash';

import { FacetName } from '@yandex-turbo/types/jobs';
import { IStructure, translations } from '@yandex-turbo/components/LcJobsFilters';

import { getTranslatedStructure } from '../LcJobsFiltersWrapper.helper';

jest.mock('@yandex-turbo/components/LcJobsFilters');

describe('LcJobsFiltersWrapper.helper', () => {
    describe('getTranslatedStructure', () => {
        let defaultFacetParams;

        beforeEach(() => {
            // @ts-ignore
            translations.mockReturnValue('translated');

            defaultFacetParams = {
                type: 'modelmultiplechoice' as const,
                value: '',
            };
        });

        it('should translate labels via translations', () => {
            const structure: IStructure = {
                [FacetName.EmploymentTypes]: {
                    key: FacetName.EmploymentTypes,
                    name: FacetName.EmploymentTypes,
                    ...defaultFacetParams,
                    choices: [{ value: 123, label: 'not translated' }],
                },
                [FacetName.ProLevels]: {
                    key: FacetName.ProLevels,
                    name: FacetName.ProLevels,
                    ...defaultFacetParams,
                    choices: [{ value: 123, label: 'not translated' }],
                },
            };

            const translatedStructure = getTranslatedStructure(structure);

            expect(get(translatedStructure, `${FacetName.EmploymentTypes}.choices[0].label`)).toBe('translated');
            expect(get(translatedStructure, `${FacetName.ProLevels}.choices[0].label`)).toBe('translated');
        });

        it('should construct new choices if facet has not yet', () => {
            const structure: IStructure = {
                [FacetName.ProLevels]: { key: FacetName.ProLevels, name: FacetName.ProLevels, ...defaultFacetParams },
            };

            const { choices } = getTranslatedStructure(structure)[FacetName.ProLevels];

            expect(choices).toBeInstanceOf(Array);
        });

        it('should preserve whole structure parts except pro_levels and employment_types', () => {
            const facetProfessions = {
                key: FacetName.Professions, name: FacetName.Professions,
                ...defaultFacetParams, choices: [{ value: 123, label: 'not translated' }],
            };
            const facetSkills = {
                key: FacetName.Skills, name: FacetName.Skills,
                ...defaultFacetParams, choices: [{ value: 123, label: 'not translated' }],
            };
            const facetCities = {
                key: FacetName.Cities, name: FacetName.Cities,
                ...defaultFacetParams, choices: [{ value: 123, label: 'not translated' }],
            };
            const facetServices = {
                key: FacetName.Services, name: FacetName.Services,
                ...defaultFacetParams, choices: [{ value: 123, label: 'not translated' }],
            };

            const structure: IStructure = {
                [FacetName.Professions]: facetProfessions,
                [FacetName.Skills]: facetSkills,
                [FacetName.Cities]: facetCities,
                [FacetName.Services]: facetServices,
            };

            const translatedStructure = getTranslatedStructure(structure);

            expect(translatedStructure[FacetName.Professions]).toBe(facetProfessions);
            expect(translatedStructure[FacetName.Skills]).toBe(facetSkills);
            expect(translatedStructure[FacetName.Cities]).toBe(facetCities);
            expect(translatedStructure[FacetName.Services]).toBe(facetServices);
        });
    });
});
