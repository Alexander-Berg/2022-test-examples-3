/* eslint-disable */
import { RouteOptions } from '../types';
import {
    categoGenerator,
    findDialogGenerator,
    getBuiltinCollectionsGenerator,
    getCollectionsGenerator,
    getCompilationGenerator,
    getDialogGenerator,
    getDialogsByCategoryGenerator,
    getExperimentsGenerator,
    getMainPageDataGenerator,
    getReviewFormGenerator,
    getSkillForAccountLinkingGenerator,
    getSkillSlugByIdGenerator,
    getSmartHomeCategoryGenerator,
    getSuggestGenerator,
} from '../ammo/utils';
import { RoutesStructure } from '../configs/routes';

export const catalogueV1Categories: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/categories': {
        testCases: [
            {
                name: 'test',
                method: 'GET',
                RPS: 23,
                generator: categoGenerator,
            },
        ],
    },
};

export const catalogueV1DialogsSlug: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/dialogs/:slug': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 8,
                generator: getDialogGenerator,
            },
        ],
    },
};

export const catalogueV1DialogsSearch: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/dialogs/search': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 1,
                generator: findDialogGenerator,
            },
        ],
    },
};

export const catalogueV1DialogsSuggest: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/dialogs/suggest': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 1,
                generator: getSuggestGenerator,
            },
        ],
    },
};

export const catalogueV1Dialogs2: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/dialogs2': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 3,
                generator: getDialogsByCategoryGenerator,
            },
        ],
    },
};

export const catalogueV1DialogsSkillIdSlug: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/dialogs/:skillId/slug': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 1,
                generator: getSkillSlugByIdGenerator,
            },
        ],
    },
};

export const catalogueV1DialogsSkillIdAccountLinking: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/dialogs/:skillId/account_linking': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 1,
                generator: getSkillForAccountLinkingGenerator,
            },
        ],
    },
};

export const catalogueV1Collections: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/collections': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 1,
                generator: getCollectionsGenerator,
            },
        ],
    },
};

export const catalogueV1BuiltinCollections: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/builtin-collections': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 7,
                generator: getBuiltinCollectionsGenerator,
            },
        ],
    },
};

export const catalogueV1Reviewform: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/reviewform': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 5,
                generator: getReviewFormGenerator,
            },
        ],
    },
};

export const catalogueV1ExperimentsYandexuid: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/experiments/:yandexuid': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 23,
                generator: getExperimentsGenerator,
            },
        ],
    },
};

export const catalogueV1CompilationsSlug: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/compilations/:slug': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 1,
                generator: getCompilationGenerator,
            },
        ],
    },
};

export const catalogueV1PagesMain: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/pages/main': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 11,
                generator: getMainPageDataGenerator,
            },
        ],
    },
};

export const catalogueV1SmartHomeGetNativeSkills: Partial<RoutesStructure<RouteOptions>> = {
    'catalogue/v1/smart_home/get_native_skills': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 1,
                generator: getSmartHomeCategoryGenerator,
            },
        ],
    },
};
