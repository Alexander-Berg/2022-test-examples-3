import type {GetContactsResult} from '~/app/bcm/mbiPartner/Client/ContactClient/types';
import type {ContactList, ContactEmail, ContactListLink} from '~/app/entities/contact/types';

export const getContactEmail = (draft?: Partial<ContactEmail>): ContactEmail => ({
    id: 2,
    email: 'ivanov@yandex.ru',
    passport: true,
    valid: true,
    active: true,
    ...draft,
});

export const getContactListLink = (draft?: Partial<ContactListLink>): ContactListLink => ({
    id: 1,
    campaignId: 2,
    roleIDs: [1],
    sortedRoles: [
        {
            id: 1,
            roleId: 1,
        },
    ],
    ...draft,
});

export const getContactList = (draft?: Partial<ContactList>): ContactList => ({
    firstName: 'Иван',
    lastName: 'Иванов',
    userId: 244,
    id: 12,
    login: 'ivanov',
    sortedEmails: [],
    sortedLinks: [],
    ...draft,
});

export default (
    contactListItemLink?: Partial<ContactListLink>,
    contactListItem?: Partial<ContactList>,
): GetContactsResult => ({
    pagerInfo: {
        perpageNumber: 10,
        currentPage: 1,
        totalCount: 1,
    },
    data: [
        getContactList({
            sortedEmails: [getContactEmail()],
            sortedLinks: [getContactListLink(contactListItemLink)],
            ...contactListItem,
        }),
    ],
});
