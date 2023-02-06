/* eslint-disable */
import { User } from '../../../db';
import { UserAttributes } from '../../../db/tables/user';
import { bigTimeout } from './utils';

import log from '../../../services/log';

/** * Для переопределения параметров в обязательные */
interface MUserAttributes extends UserAttributes {
    id: string;
}

function generateUser(props: MUserAttributes) {
    const { id = '0' } = props;

    const name = 'user#' + id;

    return {
        id,
        name,
    };
}

export const generateUsers = async(nUsers: number) => {
    const startId = await User.count();
    const users = [];
    for (let id = startId; id < startId + nUsers; id++) {
        users.push(generateUser({ id: String(id) }));
    }
    return users;
};

export const createUsers = async(nUsers: number) => {
    const users = await generateUsers(nUsers);
    await User.bulkCreate(users, bigTimeout as any);
    log.info('Created ' + users.length + ' users');
};
