import {CURRENT_USER_ID, CURRENT_USER_LOGIN} from '~/testServer';
import type {GetBusinessContactsResult} from '~/app/bcm/mbiPartner/Client/BusinessContactClient/types';
import {BusinessRoles} from '~/app/entities/role/types';
import {Contact, ContactStatus, ContactLink} from '~/app/entities/contact/types';

export const getBusinessContactLink = (draft?: Partial<ContactLink>): ContactLink => ({
    id: 3,
    campaignId: 1001410791,
    partnerName: 'Экспрессович',
    roles: [
        {
            id: 2,
            role: BusinessRoles.ShopAdmin,
        },
    ],
    ...draft,
});

export const getBusinessContact = (draft?: Partial<Contact>): Contact => ({
    id: 1,
    userId: CURRENT_USER_ID,
    firstName: 'Vasily',
    lastName: 'Pupkin',
    login: CURRENT_USER_LOGIN,
    marketOnly: false,
    emails: [
        {
            id: 2,
            email: `${CURRENT_USER_LOGIN}@yandex.ru`,
            passport: true,
        },
    ],
    links: [getBusinessContactLink({id: 1})],
    businessRoles: [BusinessRoles.BusinessOwner],
    contactStatus: ContactStatus.Connected,
    ...draft,
});

export default (contact?: Partial<Contact>): GetBusinessContactsResult => ({
    paging: {},
    contacts: [getBusinessContact(contact)],
});
