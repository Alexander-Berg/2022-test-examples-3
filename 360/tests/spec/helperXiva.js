const helperXiva = require('helpers/xiva');

/**
 *
 */
function FakeWS() {
    // ping only in next tick
    // since callbacks will be subscribed right after initialization
    process.nextTick(() => this.onmessage({
        data: JSON.stringify({
            operation: 'ping',
            'server-interval-sec': 1
        })
    }));

    this.spy();
    this.close = () => this.onclose();
}

describe('helperXiva', () => {
    before(function() {
        this.clock = sinon.useFakeTimers();
        this._ws = global.WebSocket;
        global.WebSocket = FakeWS;
    });

    it('should resubscribe if ping has timed out', function(done) {
        const spy = sinon.spy();
        FakeWS.prototype.spy = spy;

        helperXiva.subscribe({
            services: ['disk-json']
        });

        process.nextTick(() => {
            this.clock.tick(4000);
            expect(spy.calledTwice).to.be(true);
            done();
        });
    });

    after(function() {
        global.WebSocket = this._ws;
        this.clock.restore();
    });
});
