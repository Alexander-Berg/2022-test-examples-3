import type {BusinessListItemDto} from '~/app/bcm/mbiPartner/Client/BusinessClient/types';
import {BusinessRoles} from '~/app/entities/role/types';
import {CURRENT_USER_ID} from '~/testServer';

export default (draft: Partial<BusinessListItemDto>): BusinessListItemDto => ({
    id: 1,
    name: 'Бизнес',
    users: [{login: 'express.expressov', uid: CURRENT_USER_ID}],
    businessRoles: [BusinessRoles.BusinessOwner],
    serviceCount: 1,
    generalPlacementTypes: [],
    ...draft,
});
