/* eslint-disable */
import { v4 as uuid } from 'uuid';
import * as _ from 'lodash';
import * as cryptoRandomString from 'crypto-random-string';
import { Draft, Image, Operation, sequelize, Skill, PhoneConfirmation, OAuthApp } from '../../../db';
import { Channel, AliceSkillBackendType } from '../../../db/tables/settings';
import { DraftAttributes, draftStatuses } from '../../../db/tables/draft';
import { SkillAttributes } from '../../../db/tables/skill';
import log from '../../../services/log';
import { generateImage } from './create-image';
import { generateOperations } from './create-operation';
import { bigTimeout, getRandomInt, getRandomBoolean } from './utils';

import { categories } from '../../../fixtures/categories';
import { create } from '../../../services/skill-lifecycle';
import { defaultNotificationSettings } from '../../../types';

/** Для переопределения параметров SkillAttributes в обязательные */
interface MSkillAttributes extends SkillAttributes {
    userId: string;
}

interface MSkillAttributesWithDraft extends MSkillAttributes {
    draft: DraftAttributes;
}

const getSkillPropsWithDefaults = (props: SkillAttributes): MSkillAttributesWithDraft => {
    const {
        // id
        slug = 'skill' +
            uuid()
                .toString()
                .substring(8),
        onAir = getRandomBoolean(),
        useZora = getRandomBoolean(),
        exposeInternalFlags = getRandomBoolean(),

        userId = '0', // overwrite later
        // draft,
        channel = _.sample<Channel>(Object.values(Channel)),
        name = 'skill ' + String(Math.random()),
        // logoId = uuid(), ! Необходимо проставить позже. Вот так:
        //  update skills set "logoId" = images.id from images where "logoId" is null and  skills.id = images."skillId";

        activationPhrases = [String(Math.random()) + '  ' + String(Math.random()), String(Math.random())],
        backendSettings = {
            backendType: (getRandomBoolean() ? 'webhook' : 'function') as AliceSkillBackendType,
            uri: getRandomBoolean() ? 'https://example.com' : '',
            functionId: getRandomBoolean() ? 'id' : '',
        },
        publishingSettings = {
            category: _.sample(categories)!.type, // ex.: 'news',
            structuredExamples: [
                {
                    marker: 'запусти навык',
                    activationPhrase: Math.random()
                        .toString(36)
                        .substring(2),
                    request: '',
                },
            ],
            description:
                'The one skill: ' +
                Math.random()
                    .toString(36)
                    .substring(2),
            developerName:
                'John Preston#' +
                Math.random()
                    .toString(36)
                    .substring(2),
        },
        catalogRank = 1,
        look = 'external',
        notificationSettings = defaultNotificationSettings,
        oauthAppId,
    } = props;

    const skillId = uuid();
    const draft = {
        skillId,
        channel,
        name,
        activationPhrases,
        backendSettings,
        publishingSettings,
        status: _.sample(draftStatuses),
    };
    return {
        id: skillId,
        slug,
        onAir,
        useZora,
        exposeInternalFlags,

        userId,
        draft,
        channel,
        name,

        activationPhrases,
        backendSettings,
        publishingSettings,

        catalogRank,
        look,
        notificationSettings,
        oauthAppId,
        // logoId,
    };
};

export const createSkill = async(props: SkillAttributes = {}) => {
    const fullSkillProps = getSkillPropsWithDefaults(props);

    return await create(fullSkillProps);
};

function generateSkill(props: MSkillAttributes): MSkillAttributesWithDraft {
    return getSkillPropsWithDefaults(props);
}

async function generateSkillsByUser(props: MSkillAttributes, nSkills: number): Promise<MSkillAttributesWithDraft[]> {
    const skills = [];
    for (let i = 0; i < nSkills; i++) {
        skills.push(generateSkill(props));
    }
    return skills;
}

interface PhoneConfirmationParams {
    phoneNumber: string;
    skillId: string;
    code?: string;
}

/** Заливает в БД до nMaxSkills скиллов,
 * рандомно выбирая кому назначить скилл (важно, чтобы userid были порядковыми в БД).
 * т.е., были все с id от 0 до n.
 * Каждому скиллу - назначается рандомное от ... до ... количество операций
 */
export async function randomCreateSkillsWithOperations(nUsers: number, nMaxSkills: number, nMaxSkillsByUser: number) {
    await sequelize.transaction(async t => {
        // await sequelize.query('SET CONSTRAINTS ALL DEFERRED;', { transaction: t });
        const skills = [];
        const drafts: DraftAttributes[] = [];
        const confirmations: PhoneConfirmationParams[] = [];
        const oauthApps = [];

        for (let userId = 0; userId < nUsers; userId++) {
            const nSkills = getRandomInt(0, nMaxSkillsByUser);

            const oauthAppId = uuid();

            oauthApps.push({
                id: oauthAppId,
                userId: String(userId),
                name: cryptoRandomString(20),
                socialAppName: cryptoRandomString(20),
            });

            if (skills.length + nSkills <= nMaxSkills) {
                skills.push(...(await generateSkillsByUser({ userId: String(userId), oauthAppId }, nSkills)));
            }
        }

        const maxOperations = skills.length * 4;
        const operations = [];
        const images = [];
        for (const skill of skills) {
            drafts.push(skill.draft);
            confirmations.push({ skillId: skill.id!, code: '123456', phoneNumber: '+79999999999' });

            const opsBySkill = getRandomInt(1, 10);
            images.push(generateImage({ skillId: skill.id, id: skill.logoId }));

            if (operations.length + opsBySkill < maxOperations) {
                operations.push(...(await generateOperations({ itemId: String(skill.id) }, opsBySkill)));
            }
        }

        await OAuthApp.bulkCreate(oauthApps, { retry: bigTimeout.retry, transaction: t } as any);
        log.info('Created ' + oauthApps.length + ' oauth apps');

        await Skill.bulkCreate(skills, { retry: bigTimeout.retry, transaction: t } as any);
        log.info('Created ' + skills.length + ' skills');

        await Draft.bulkCreate(drafts, { retry: bigTimeout.retry, transaction: t } as any);
        log.info('Created ' + drafts.length + ' drafts');

        await Image.bulkCreate(images, { retry: bigTimeout.retry, transaction: t } as any);
        log.info('Images created');

        await Operation.bulkCreate(operations, { retry: bigTimeout.retry, transaction: t } as any);
        log.info('Created ' + operations.length + ' operations');

        await PhoneConfirmation.bulkCreate(confirmations, { retry: bigTimeout.retry, transaction: t } as any);
        log.info('Created ' + confirmations.length + ' confirmations');
    });
}
