'use strict';

const userphones = require('./userphones.js');

test('userphones', async () => {
    const blackbox = jest.fn();
    const core = {
        service: () => blackbox,
        auth: { get: () => ({ uid: '42' }) }
    };
    blackbox.mockResolvedValueOnce({
        users: [
            {
                id: '42',
                phones: [ {
                    id: '1',
                    attributes: {
                        4: '1',
                        103: '+7000*****11',
                        108: '1'
                    }
                }, {
                    id: '2',
                    attributes: {
                        4: '2',
                        103: '+7000*****22',
                        107: '1'
                    }
                }, {
                    id: '3',
                    attributes: {
                        4: '3',
                        103: '+7000*****33'
                    }
                }, {
                    id: '4',
                    attributes: {
                        4: '4',
                        103: '+7000*****44',
                        107: '1',
                        108: '1'
                    }
                } ]
            },
            {
                id: 'uid77',
                phones: []
            }
        ]
    });

    const result = await userphones({}, core);

    expect(result).toEqual({
        uid: '42',
        phone: [ {
            id: '1',
            active: '0',
            secure: '1',
            masked_number: '+7000*****11'
        }, {
            id: '2',
            active: '1',
            secure: '0',
            masked_number: '+7000*****22'
        }, {
            id: '3',
            active: '0',
            secure: '0',
            masked_number: '+7000*****33'
        }, {
            id: '4',
            active: '1',
            secure: '1',
            masked_number: '+7000*****44'
        } ]
    });

    expect(result.phone[0].confirmed).toEqual(1);

    expect(blackbox).toHaveBeenCalledWith('userinfo', {
        uid: '42',
        getphones: 'bound',
        phone_attributes: '4,103,107,108'
    });
});
