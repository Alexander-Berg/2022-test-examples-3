import * as faker from '../../jest/faker';
import { getFullName } from '../fullName';

describe('getFullName', () => {
    it('returns display name', () => {
        const firstName = faker.firstName();
        const lastName = faker.lastName();

        const fullName = `${firstName} ${lastName}`;

        expect(getFullName(firstName, lastName)).toEqual(fullName);
    });

    it('returns first name if the last name is empty', () => {
        const firstName = faker.firstName();

        expect(getFullName(firstName, '')).toEqual(firstName);
    });

    it('returns first name if the last name is empty', () => {
        const lastName = faker.lastName();

        expect(getFullName('', lastName)).toEqual(lastName);
    });
});
