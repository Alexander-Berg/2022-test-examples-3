/* eslint-disable */
import { RouteOptions } from '../types';
import { RoutesStructure } from '../configs/routes';
import {
    getStatusGenerator,
    getSkillImagesGenerator,
    getSkillImagesUploadGenerator,
    getSkillSoundGenerator,
    uploadSkillSoundGenerator,
} from '../ammo/utils';

export const publicV1Status: Partial<RoutesStructure<RouteOptions>> = {
    'public/v1/status': {
        testCases: [
            {
                name: 'test',
                method: 'GET',
                RPS: 1,
                generator: getStatusGenerator,
            },
        ],
    },
};

export const publicV1SkillsSkillIdImages: Partial<RoutesStructure<RouteOptions>> = {
    'public/v1/skills/:skillId/images': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 1,
                generator: getSkillImagesGenerator,
            },
            {
                name: 'test',
                method: 'POST',
                RPS: 1,
                generator: getSkillImagesUploadGenerator,
            },
        ],
    },
};

export const publicV1SkillsSkillIdSounds: Partial<RoutesStructure<RouteOptions>> = {
    'public/v1/skills/:skillId/sounds': {
        testCases: [
            {
                name: 'test',
                method: 'GET',
                RPS: 1,
                generator: getSkillSoundGenerator,
            },
            {
                name: 'test',
                method: 'POST',
                RPS: 1,
                generator: uploadSkillSoundGenerator,
            },
        ],
    },
};
