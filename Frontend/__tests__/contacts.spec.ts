import * as SharedActions from '../sharedActions';
import * as ContactsActions from '../contacts';
import { usersMockFactory } from './mock/user';
import { mutate } from './mock/utils';

describe('Contacts reducer', () => {
    const usersMock = usersMockFactory();

    describe('#Set contacts', () => {
        it('should set contacts', () => {
            const [
                contact1,
                contact2,
            ] = usersMock.createContact()(2);

            const state = ContactsActions.contactsReducer(
                undefined,
                SharedActions.setContacts([contact1, contact2]),
            );

            expect(state).toMatchObject({
                maxVersion: contact2.version,
                byId: {
                    [contact1.guid]: contact1,
                    [contact2.guid]: contact2,
                },
            });
        });

        it('should update contacts', () => {
            const [
                contact1,
                contact2,
            ] = usersMock.createContact()(2);

            const state = ContactsActions.contactsReducer(
                undefined,
                SharedActions.setContacts([contact1, contact2]),
            );

            expect(state).toMatchObject({
                maxVersion: contact2.version,
                byId: {
                    [contact1.guid]: contact1,
                    [contact2.guid]: contact2,
                },
            });

            const newContact2 = mutate(contact2, { contact_name: contact2.contact_name + '_changed', version: contact2.version + 1 });

            const state2 = ContactsActions.contactsReducer(
                state,
                SharedActions.setContacts([contact1, newContact2]),
            );

            expect(state2).toMatchObject({
                maxVersion: newContact2.version,
                byId: {
                    [contact1.guid]: contact1,
                    [contact2.guid]: newContact2,
                },
            });
        });
    });
});
