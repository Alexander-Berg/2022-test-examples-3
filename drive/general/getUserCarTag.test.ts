import { getUserCarTag } from 'entities/User/helpers/getUserCarTag/getUserCarTag';

const CAR_OBJECT = {
    number: 'am068c665',
    imei: '',
    model_id: 'skoda_octavia',
    vin: '',
    id: '',
};

const USER_TAGS_MOCK = {
    user_tags: [],
    records: [
        {
            tag_id: '123',
            priority: 0,
            tag: 'assigned_to_operator',
            performer: '',
            display_name: 'Привязка пользователя',
            object_id: '',
            object_info: CAR_OBJECT,
        },
    ],
};

describe('getUserCarTag', function () {
    it('works correctly', function () {
        expect(getUserCarTag(USER_TAGS_MOCK)?.tag_id).toEqual('123');
    });

    it('works with another tag', function () {
        USER_TAGS_MOCK.records[0].tag = 'another_tag';
        expect(getUserCarTag(USER_TAGS_MOCK)).toBeUndefined();
    });
});
