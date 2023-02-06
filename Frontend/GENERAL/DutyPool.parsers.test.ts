import { parseDutyPoolListItem } from '~/src/features/Duty2/redux/DutyPool.parsers';

describe('DutyPool.parser', () => {
    const data = {
        id: 10,
        service_id: 90,
        name: 'test',
        slug: 'slug',
        participants: [],
        full_service: false,
        autoupdate: true,
    };

    it('Should convert fields to frontend format', () => {
        const parsedData = parseDutyPoolListItem(data);
        const expected = {
            id: 10,
            serviceId: 90,
            name: 'test',
            slug: 'slug',
            participants: [],
            fullService: false,
            autoupdate: true,
        };
        expect(parsedData).toStrictEqual(expected);
    });
});
