/* eslint-disable */
import { Skill, User, Draft, sequelize, OAuthApp } from '../../db';
import { DraftStatus } from '../../db/tables/draft';

export const getSkillSlugs = async(limit: number) => {
    const result = await Skill.findAll({
        limit,
        attributes: ['slug'],
    });

    return result.map(({ slug }) => slug);
};

export const getSkillIds = async(limit: number) => {
    const result = await Skill.findAll({
        where: {
            onAir: true,
        },
        limit,
        attributes: ['id'],
    });

    return result.map(({ id }) => id);
};

export const getUserIds = async(limit: number, isAdmin?: boolean) => {
    const result = await User.findAll({
        limit,
        where: {
            ...(isAdmin ? { isAdmin } : {}),
        },
    });

    return result.map(({ id }) => id);
};

interface GetSkillsMetaParams {
    limit: number;
    draftStatuses?: DraftStatus[];
    withWebhook?: boolean;
    withOauthApp?: boolean;
}

export const getSkillsMeta = async({ draftStatuses, limit, withWebhook, withOauthApp }: GetSkillsMetaParams) => {
    const result = await Skill.findAll({
        include: [
            {
                model: Draft,
                where: {
                    ...(draftStatuses ?
                        {
                            status: { [sequelize.Op.in]: draftStatuses },
                        } :
                        {}),
                },
            },
        ],
        where: {
            onAir: true,
            ...(withWebhook ?
                {
                    [sequelize.Op.and]: sequelize.literal("draft.\"backendSettings\" ->> 'uri' != ''"),
                } :
                {}),
            ...(withOauthApp ?
                {
                    oauthAppId: {
                        [sequelize.Op.not]: null,
                    },
                } :
                {}),
        },
        limit,
    });

    return result.map(({ id, userId }) => ({ skillId: id, userId }));
};

export const getOauthAppsMeta = async(limit: number) => {
    const result = await OAuthApp.findAll({
        limit,
        attributes: ['id', 'userId'],
    });

    return result.map(({ id, userId }) => ({ oauthAppId: id, userId }));
};
