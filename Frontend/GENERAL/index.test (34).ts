import {
    ok,
    strictEqual,
} from 'assert';
import {
    SinonSpy,
    replace,
    restore,
    fake,
} from 'sinon';
import { Statistics } from '.';

describe('Statistics', function() {
    let _fakeFetch: SinonSpy;

    beforeEach(function() {
        replace(self, 'fetch', _fakeFetch = fake());
        replace(Date, 'now', fake.returns(5));
    });

    afterEach(function() {
        restore();
    });

    it('#.addClickedAction()', async function() {
        const statistics = new Statistics();

        await statistics
            .addClickedAction('test', { pushId: 'id' })
            .send();

        strictEqual(_fakeFetch.callCount, 1, 'неверное количество вызовов');
        strictEqual(_fakeFetch.lastCall.args[0], '//yandex.ru/clck/click/dtype=stred/pid=457/cid=73559/path=clicked-action/vars=-push_id=id,-action=test/cts=5/*');
    });

    it('#.addClicked()', async function() {
        const statistics = new Statistics();

        await statistics
            .addClicked({ pushId: 'id0' })
            .send();

        strictEqual(_fakeFetch.callCount, 1, 'неверное количество вызовов');
        strictEqual(_fakeFetch.lastCall.args[0], '//yandex.ru/clck/click/dtype=stred/pid=457/cid=73559/path=clicked/vars=-push_id=id0/cts=5/*');
    });

    it('#.addClosed()', async function() {
        const statistics = new Statistics();

        await statistics
            .addClosed({ pushId: 'id' })
            .send();

        strictEqual(_fakeFetch.callCount, 1, 'неверное количество вызовов');
        strictEqual(_fakeFetch.lastCall.args[0], '//yandex.ru/clck/click/dtype=stred/pid=457/cid=73559/path=closed/vars=-push_id=id/cts=5/*');
    });

    it('#.addExpired()', async function() {
        const statistics = new Statistics();

        await statistics
            .addExpired({ pushId: 'id' })
            .send();

        strictEqual(_fakeFetch.callCount, 1, 'неверное количество вызовов');
        strictEqual(_fakeFetch.lastCall.args[0], '//yandex.ru/clck/click/dtype=stred/pid=457/cid=73559/path=expired/vars=-push_id=id,-reason=ttl/cts=5/*');
    });

    it('#.addIgnored()', async function() {
        const statistics = new Statistics();

        await statistics
            .addIgnored('parse-error')
            .send();

        strictEqual(_fakeFetch.callCount, 1, 'неверное количество вызовов');
        strictEqual(_fakeFetch.lastCall.args[0], '//yandex.ru/clck/click/dtype=stred/pid=457/cid=73559/path=ignored/vars=-reason=parse-error/cts=5/*');
    });

    it('#.addReceived()', async function() {
        const statistics = new Statistics();

        await statistics
            .addReceived({ pushId: 'id' })
            .send();

        strictEqual(_fakeFetch.callCount, 1, 'неверное количество вызовов');
        strictEqual(_fakeFetch.lastCall.args[0], '//yandex.ru/clck/click/dtype=stred/pid=457/cid=73559/path=received/vars=-push_id=id/cts=5/*');
    });

    it('#.addShown()', async function() {
        const statistics = new Statistics();

        await statistics
            .addShown({ pushId: 'id' })
            .send();

        strictEqual(_fakeFetch.callCount, 1, 'неверное количество вызовов');
        strictEqual(_fakeFetch.lastCall.args[0], '//yandex.ru/clck/click/dtype=stred/pid=457/cid=73559/path=shown/vars=-push_id=id/cts=5/*');
    });

    it('#.send()', async function() {
        const statistics = new Statistics();
        const data = { pushId: 'id' };

        await statistics
            .addReceived(data)
            .addIgnored('silent', data)
            .send();

        strictEqual(_fakeFetch.callCount, 2, 'неверное количество вызовов');
        ok(
            _fakeFetch.getCall(0).calledWithExactly(
                '//yandex.ru/clck/click/dtype=stred/pid=457/cid=73559/path=received/vars=-push_id=id/cts=5/*',
                { mode: 'no-cors', credentials: 'include' },
            ),
            '1 вызов',
        );
        ok(
            _fakeFetch.getCall(1).calledWithExactly(
                '//yandex.ru/clck/click/dtype=stred/pid=457/cid=73559/path=ignored/vars=-push_id=id,-reason=silent/cts=5/*',
                { mode: 'no-cors', credentials: 'include' },
            ),
            '2 вызов',
        );
    });
});
