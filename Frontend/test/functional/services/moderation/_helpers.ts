/* eslint-disable */
import { createImageForSkill, createSkill, createUser } from '../../_helpers';
import { SkillInstance } from '../../../../db/tables/skill';
import { Operation, sequelize } from '../../../../db';

export const createSkillWithImageInDraft = async() => {
    const user = await createUser();
    const skill = await createSkill();
    const image = await createImageForSkill(skill);
    skill.draft.logoId = image.id;
    skill.draft.logo2 = image;
    await skill.draft.save();
    skill.user = user;
    return skill;
};

export const countOperaions = async(skill: SkillInstance) => {
    const dbCounts = await Operation.findAll({
        attributes: ['type', [sequelize.fn('count', 'itemId'), 'count']],
        where: {
            itemId: skill.id,
        },
        group: ['type'],
    });
    const counts: Record<string, number> = {};
    for (const row of dbCounts) {
        counts[row.type] = Number(row.getDataValue('count' as any));
    }
    return counts;
};

export const serializeModeratorResponses = (data: any) => {
    return data.map((e: any) => JSON.stringify(e)).join('\n');
};
