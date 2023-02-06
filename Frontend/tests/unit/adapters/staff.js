const proxyquire = require('proxyquire');

const rp = require('../fixtures/request-promise');
const StaffApiAdapter = proxyquire.load('../../../src/server/adapters/staff', {
    'request-promise': rp,
});

describe('adapters/staff', function() {
    let staffApiAdapter;

    const serviceTicket = '************************';
    const staffApiHost = 'https://staff-api.yandex-team.ru';

    beforeEach(() => {
        staffApiAdapter = new StaffApiAdapter(staffApiHost, serviceTicket, () => {});
    });

    afterEach(function() {
        rp.resetBehavior();
        rp.reset();
    });

    describe('getGroupMembers:', function() {
        const result = [
            { person: { login: 'eroshinev' } },
            { person: { login: 'vladpotapov' } },
        ];
        const groupId = '62552';
        const requestOptions = {
            uri: 'https://staff-api.yandex-team.ru/v3/groupmembership',
            headers: {
                'X-Ya-Service-Ticket': serviceTicket,
            },
            method: 'GET',
            json: true,
            qs: {
                _fields: 'person.login',
                'group.id': '62552',
            },
        };

        it('должен формировать корректный запрос к Staff API', function() {
            rp.withArgs(requestOptions).returns(Promise.resolve({ result }));
            staffApiAdapter.getGroupMembers(groupId);
            assert.calledWith(rp, requestOptions);
        });

        it('должен возвращать в Promise только логины пользователей – членов группы', function() {
            const expected = ['eroshinev', 'vladpotapov'];
            rp.withArgs(requestOptions).returns(Promise.resolve({ result }));

            return staffApiAdapter.getGroupMembers(groupId).then((data) => {
                assert.deepEqual(data, expected);
            });
        });
    });
});

