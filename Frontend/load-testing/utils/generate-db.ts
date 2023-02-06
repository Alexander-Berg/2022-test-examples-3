/* eslint-disable */
import { User } from '../../db';
import { randomCreateSkillsWithOperations } from '../db/generate/create-skill';
import { createUsers } from '../db/generate/create-user';

import log from '../../services/log';
import { wipeDatabase } from '../../test/functional/_helpers';

interface GenerateDbParams {
    usersCount: number;
    skillsCount: number;
    maxSkillsForUser: number;
}

const generateDb = async({ usersCount, maxSkillsForUser, skillsCount }: GenerateDbParams) => {
    if (process.env.CONFIG_ENV !== 'load') {
        throw new Error('Only available in "load" environment');
    }

    await wipeDatabase();

    await createUsers(usersCount);
    await randomCreateSkillsWithOperations(await User.count(), skillsCount, maxSkillsForUser);

    log.info("Word's done");
    process.exit(0);
};

export default generateDb;
