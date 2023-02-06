import { ContactsType, IContactsItemProps } from '@yandex-turbo/components/lcTypes/lcTypes';
import { prepareItems } from '../LcPhone.helpers';

describe('prepareItems', () => {
    it('should handle with only one phone item', () => {
        const items: IContactsItemProps[] = [{
            type: ContactsType.Phone,
            text: '+7 926 123 45 67',
        }, {
            type: ContactsType.Hours,
            text: 'Пн-Пт 8:00 - 18:00',
        }];

        expect(prepareItems(items)).toEqual([{
            type: ContactsType.Phone,
            text: '+7 926 123 45 67',
            isPhoneComponent: true,
        }, {
            type: ContactsType.Hours,
            text: 'Пн-Пт 8:00 - 18:00',
        }]);
    });

    it('should handle with several phone items that separated', () => {
        const items: IContactsItemProps[] = [{
            type: ContactsType.Phone,
            text: '+7 926 123 45 67',
        }, {
            type: ContactsType.Hours,
            text: 'Пн-Пт 8:00 - 18:00',
        }, {
            type: ContactsType.Phone,
            text: '+7 926 987 65 43',
        }, {
            type: ContactsType.Email,
            text: 'ololo@gmail.com',
        }, {
            type: ContactsType.Phone,
            text: '8 926 999 99 99',
        }];

        expect(prepareItems(items)).toEqual([{
            type: ContactsType.Phone,
            text: '+7 926 123 45 67',
            isPhoneComponent: true,
        }, {
            type: ContactsType.Hours,
            text: 'Пн-Пт 8:00 - 18:00',
        }, {
            type: ContactsType.Email,
            text: 'ololo@gmail.com',
        }]);
    });

    it('should handle with several phone items in the end', () => {
        const items: IContactsItemProps[] = [{
            type: ContactsType.Hours,
            text: 'Пн-Пт 8:00 - 18:00',
        }, {
            type: ContactsType.Email,
            text: 'ololo@gmail.com',
        }, {
            type: ContactsType.Phone,
            text: '+7 926 123 45 67',
        }, {
            type: ContactsType.Phone,
            text: '+7 926 987 65 43',
        }, {
            type: ContactsType.Phone,
            text: '8 926 999 99 99',
        }];

        expect(prepareItems(items)).toEqual([{
            type: ContactsType.Hours,
            text: 'Пн-Пт 8:00 - 18:00',
        }, {
            type: ContactsType.Email,
            text: 'ololo@gmail.com',
        }, {
            type: ContactsType.Phone,
            text: '+7 926 123 45 67',
            isPhoneComponent: true,
        }]);
    });
});
