import test from 'ava';
import * as nock from 'nock';
import * as sinon from 'sinon';
import * as memento from '../../../services/memento';
import config from '../../../services/config';
import * as tvm from '../../../services/tvm';
import { TRespGetUserObjects } from '../../../protos/alice/memento/proto/api_pb';
import { TExternalSkillUserAgreements } from '../../../protos/alice/memento/proto/user_configs_pb';

const mementoScope = nock(config.memento.url);

test.before(() => {
    sinon.replace(tvm, 'getServiceTickets', sinon.fake.resolves({ memento: { ticket: '' } }));
});

test.beforeEach(async()=> {
    nock.cleanAll();
});

test.after(() => {
    sinon.restore();
});

test('getUserNewsConfig: parse response correctly', async t => {
    const selectedNews = [
        {
            newsSource: 'test1',
            rubric: 'test1',
        },
        {
            newsSource: 'test2',
            rubric: 'test2',
        },
    ];
    const pair = memento.makeNewsConfigPair(selectedNews);

    const resp = new TRespGetUserObjects();
    resp.addUserconfigs(pair);

    mementoScope.post('/get_objects').reply(200, Buffer.from(resp.serializeBinary()));

    const response = await memento.getUserNewsConfig({ userTicket: '' });

    t.deepEqual(response, { selectedNews });
});

test('setUserNewsConfig: parse response correctly', async t => {
    const selectedNews = [
        {
            newsSource: 'test1',
            rubric: 'test1',
        },
        {
            newsSource: 'test2',
            rubric: 'test2',
        },
    ];

    const spy = sinon.spy(memento, 'makeChangeUserObjectsRequestBinary');

    mementoScope.post('/update_objects').reply(200);

    const response = await memento.setUserNewsConfig({
        userTicket: '',
        newsConfig: { selectedNews },
    });

    t.is(response.statusCode, 200);
    t.deepEqual(
        spy.returnValues[0],
        Uint8Array.from(
            Buffer.from(`
ie
=type.googleapis.com/ru.yandex.alice.memento.proto.TNewsConfig$
test1test1
test2test2`),
        ),
    );
});

test('setChildAge, getChildAge', async t => {
    let protoBody: string = '';
    mementoScope.post('/update_objects').reply((url, body) => {
        protoBody = body as string;
        return [200];
    });

    const response = await memento.setUserChildAge({
        userTicket: '',
        age: 10,
    });

    t.is(response.status, 'ok');

    mementoScope.post('/get_objects').reply(200, Buffer.from(protoBody, 'hex'));

    const childAgeResp = await memento.getUserChildAge({ userTicket: '' });
    t.is(childAgeResp, 10);
});

test('User Agreements', async t => {
    const pair = memento.makeUaPair(new TExternalSkillUserAgreements());
    const resp = new TRespGetUserObjects();
    resp.addUserconfigs(pair);

    let protoBody = Buffer.from(resp.serializeBinary());

    mementoScope.post('/update_objects').reply((_url, body) => {
        protoBody = Buffer.from(body as string, 'hex');
        return [200];
    }).persist();
    mementoScope.post('/get_objects').reply(() => [200, protoBody])
        .persist();

    await memento.appendUserAgreements({
        userTicket: '',
        agreedAt: new Date(),
        agreements: [{ userAgreementId: '1', userAgreementLinks: 'test' }],
        ip: '1.1.1.1',
        skillId: 'skill-1',
        userAgent: 'Test',
    });
    await memento.appendUserAgreements({
        userTicket: '',
        agreedAt: new Date(),
        agreements: [{ userAgreementId: '2', userAgreementLinks: 'test' }],
        ip: '1.1.1.1',
        skillId: 'skill-2',
        userAgent: 'Test',
    });
    await memento.appendUserAgreements({
        userTicket: '',
        agreedAt: new Date(),
        agreements: [{ userAgreementId: '3', userAgreementLinks: 'test' }],
        ip: '1.1.1.1',
        skillId: 'skill-2',
        userAgent: 'Test',
    });
    const response = await memento.getUserAgreements({ userTicket: '' });
    t.true(
        response[0][0] === 'skill-1' &&
        response[1][0] === 'skill-2' &&
        response[1][1].useragreementsList.length === 2,
    );

    await memento.removeUserAgreements({
        userTicket: '',
        skillId: 'skill-2',
    });
    const response2 = await memento.getUserAgreements({ userTicket: '' });
    t.true(
        response2.length === 1,
    );
});
