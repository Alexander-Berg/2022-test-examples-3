/* eslint-disable */
import { RouteOptions } from '../types';
import { RoutesStructure } from '../configs/routes';
import {
    exterGetSkillGenerator,
    externalV2ChatsGenerator,
    externalV2ChatsSkillIdGenerator,
    externalV2SkillsBulkGetGenerator,
    getChatDraftStatusGenerator,
    externalV2ChatsBulkGetGenerator,
} from '../ammo/utils';

export const externalV2SkillsSkillId: Partial<RoutesStructure<RouteOptions>> = {
    'external/v2/skills/:skillId': {
        testCases: [
            {
                name: 'test',
                method: 'GET',
                RPS: 25,
                generator: exterGetSkillGenerator,
            },
        ],
    },
};

export const ExternalV1: Partial<RoutesStructure<RouteOptions>> = {
    'external/v1': {
        testCases: [
            {
                name: 'test',
                method: 'GET',
                RPS: 1,
                generator: exterGetSkillGenerator,
            },
        ],
    },
};

export const externalV2SkillsBulkGet: Partial<RoutesStructure<RouteOptions>> = {
    'external/v2/skills/bulk/get': {
        testCases: [
            {
                name: 'test',
                method: 'POST',
                RPS: 1,
                generator: externalV2SkillsBulkGetGenerator,
            },
        ],
    },
};

export const externalV2Chats: Partial<RoutesStructure<RouteOptions>> = {
    'external/v2/chats': {
        testCases: [
            {
                name: 'test',
                method: 'POST',
                RPS: 1,
                generator: externalV2ChatsGenerator,
            },
        ],
    },
};

export const externalV2ChatsSkillId: Partial<RoutesStructure<RouteOptions>> = {
    'external/v2/chats/:skillId': {
        testCases: [
            {
                name: 'test',
                method: 'DELETE',
                RPS: 1,
                generator: externalV2ChatsSkillIdGenerator,
            },
        ],
    },
};

export const externalV2ChatsSkillIdDraftStatus: Partial<RoutesStructure<RouteOptions>> = {
    'external/v2/chats/:skillId/draft/status': {
        testCases: [
            {
                name: 'test',
                method: 'GET',
                RPS: 1,
                generator: getChatDraftStatusGenerator,
            },
        ],
    },
};

export const externalV2ChatsBulkGet: Partial<RoutesStructure<RouteOptions>> = {
    'external/v2/chats/bulk/get': {
        testCases: [
            {
                name: 'test',
                method: 'POST',
                RPS: 1,
                generator: externalV2ChatsBulkGetGenerator,
            },
        ],
    },
};
