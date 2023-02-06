import * as faker from 'faker';
import { producer, objectComposer, entityCreatorFactory } from './utils';

const userOrganizationsStateProducerFactory = producer(
    objectComposer<APIv3.Organization, APIv3.User['organizations']>((org: APIv3.Organization) => org.organization_id),
);

const chatOrganizationsIdsStateProducerFactory = producer(
    objectComposer<APIv3.Organization, APIv3.Chat['organization_ids']>((org: APIv3.Organization) => org.organization_id),
);

function createName() {
    return faker.name.jobTitle();
}

export const organizationFactory = (props: Partial<APIv3.Organization> = {}): APIv3.Organization => {
    return {
        organization_name: createName(),
        organization_id: 1,
        registration_status: 'Lu',
        public: false,
        ...props,
    };
};

function entityCreatorFactoryProducer(mapper: (org?: Partial<APIv3.Organization>) => Partial<APIv3.Organization>) {
    return (common: Partial<APIv3.Organization> = {}) =>
        entityCreatorFactory((orgOrId: Partial<APIv3.Organization> | number | undefined) => {
            const obj = typeof orgOrId === 'number' ? { organization_id: orgOrId } : orgOrId;

            return organizationFactory({
                ...common,
                ...mapper(obj || {}),
            });
        });
}

export function orgsMockFactory() {
    return {
        createName,
        createFrom: entityCreatorFactoryProducer((org = {}) => org),
        createUserOrgs: userOrganizationsStateProducerFactory(organizationFactory),
        createChatOrgIds: chatOrganizationsIdsStateProducerFactory(organizationFactory),
    };
}
