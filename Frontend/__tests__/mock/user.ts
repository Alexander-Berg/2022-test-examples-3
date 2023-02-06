import * as faker from 'faker';
import { UsersState } from '../../users';
import { producer, objectComposer, entityCreatorFactory } from './utils';
import { generateGuid, generateGuids } from './common';
import { createAvatar } from '../../../helpers/avatar';

const usersStateProducerFactory = producer(objectComposer<APIv3.User, UsersState>((user: APIv3.User) => user.guid));

function createName() {
    return `${faker.name.findName()} ${faker.name.lastName()}`;
}

export const userFactory = (props: Partial<APIv3.User> = {}): APIv3.User => {
    return {
        display_name: createName(),
        version: 1,
        guid: generateGuid(),
        ...props,
    };
};

export const employeesInfo = (): Record<number, APIv3.EmployeeInfo> => {
    return {
        '0': {
            position: 'developer',
            organization_id: 0,
            is_admin: false,
        },
    };
};

function entityCreatorFactoryProducer(mapper: (user?: Partial<APIv3.User>) => Partial<APIv3.User>) {
    return (common: Partial<APIv3.User> = {}) =>
        entityCreatorFactory((userOrGuid: Partial<APIv3.User> | string | undefined) => {
            const obj = typeof userOrGuid === 'string' ? { guid: userOrGuid } : userOrGuid;

            return userFactory({
                ...common,
                ...mapper(obj || {}),
            });
        });
}

interface UIUserData {
    name: string;
    guid: string;
    avatar: Client.Avatar;
}

export const uiUserDataFactory = (props: Partial<UIUserData> = {}): UIUserData => {
    const name = props.name || faker.name.firstName() + ' ' + faker.name.lastName();
    const guid = props.guid || faker.random.uuid();

    return {
        name,
        guid,
        avatar: props.avatar || createAvatar(undefined, name, guid),
    };
};

function uiUserDataEntityCreatorFactoryProducer() {
    return (common: Partial<UIUserData> = {}) =>
        entityCreatorFactory((userOrGuid: Partial<UIUserData> | string | undefined) => {
            const obj = typeof userOrGuid === 'string' ? { guid: userOrGuid } : userOrGuid;

            return uiUserDataFactory({
                ...common,
                ...obj,
            });
        });
}

export function usersMockFactory() {
    return {
        createName,
        generateGuids: (count: number) => generateGuids(count),
        createFrom: entityCreatorFactoryProducer((user = {}) => user),
        createUnlimited: entityCreatorFactoryProducer((user = {}) => ({
            ...user,
            registration_status: 'U',
            phone_id: faker.random.uuid(),
            need_phone_confirmation: false,
        })),
        createLimited: entityCreatorFactoryProducer((user = {}) => ({
            ...user,
            need_phone_confirmation: true,
            registration_status: 'Lu',
        })),
        createAnonimous: entityCreatorFactoryProducer((user = {}) => ({
            ...user,
            need_phone_confirmation: true,
            registration_status: 'L',
        })),
        createContact: entityCreatorFactoryProducer((user = {}) => ({
            contact_name: faker.name.title(),
            registration_status: 'U',
            need_phone_confirmation: false,
            ...user,
        })),
        createOrganizationUser: entityCreatorFactoryProducer((user = {}) => ({
            organizations: {
                '123': {
                    organization_id: 123,
                    organization_name: 'test',
                    registration_status: 'U',
                    public: false,
                },
            },
            ...user,
        })),
        createState: usersStateProducerFactory(userFactory),
        createUIUserData: uiUserDataEntityCreatorFactoryProducer(),
    };
}
