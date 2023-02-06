const assert = require('assert');
const proxyquire = require('proxyquire').noPreserveCache();

const originalCwd = process.cwd;
const originalEnv = process.env;
const origProcessOn = process.on;
const origStderrWrite = process.stderr.write;

const pause = () => new Promise(resolve => setTimeout(resolve, 0));

const tryLoadModule = (env = {}, { getStandName, getUrl, closeTunnelPromise } = {}) => {
    Object.assign(process.env, env);

    function Tunneler() {
        return {
            getUrl: getUrl || (() => 'default-url'),
            async mkTunnel() { await closeTunnelPromise() },
        };
    }
    Tunneler['@noCallThru'] = true;

    return proxyquire('./open-tunnel', {
        '@yandex-int/tunneler': Tunneler,
        './stand-name.js': getStandName ? getStandName() : 'stand-name',
    });
};

const loadAndOpenTunnel = async(env = {}, { resolveOrReject, localport } = {}) => {
    resolveOrReject || (resolveOrReject = 'resolve');
    localport || (localport = 65535);

    let closeTunnel;
    const { openTunnel } = tryLoadModule(env, {
        closeTunnelPromise() {
            return new Promise((resolve, reject) => { closeTunnel = ({ resolve, reject })[resolveOrReject] });
        },
    });

    const out = [];
    process.stderr.write = (...v) => out.push(v);
    openTunnel({ localport });
    closeTunnel();
    await pause(); // To wait the result of the end

    process.stderr.write = origStderrWrite;

    return { stderr: out };
};

describe('open-tunnel', () => {
    beforeEach(() => {
        process.env = {};
        process.on = () => 0; // Mock process.on to prevent leaking subscription.
    });

    afterEach(() => {
        process.stderr.write = origStderrWrite;
        process.env = originalEnv;
        process.on = origProcessOn;
    });

    it('should open tunnel on port 111', async() => {
        const out = await loadAndOpenTunnel({ STATIC_PORT: 111 });

        assert.ok(String(out[0]), 'Trying to open tunnel with 111 number:\n');
    });

    it('should print getUrl on success', async() => {
        const out = await loadAndOpenTunnel({ STATIC_PORT: 111, getUrl: () => 'resulting-url' });

        assert.ok(String(out[1]), 'resulting-url\n');
    });
});

describe('open-tunnel: staticNumber', () => {
    beforeEach(() => {
        process.env = {};
    });

    afterEach(() => {
        process.env = originalEnv;
        process.cwd = originalCwd;
    });

    it('should return some static number by ', () => {
        const { staticNumber } = tryLoadModule({ STATIC_NUMBER: 7777 });
        assert.strictEqual(staticNumber(), 7777);
    });

    it('should return static number by stand name', () => {
        const { staticNumber } = tryLoadModule({}, { getStandName: () => 'QWE-123' });
        assert.strictEqual(staticNumber(), 123);
    });

    it('should return static number by TERM_SESSION_ID env', () => {
        const { staticNumber } = tryLoadModule({ TERM_SESSION_ID: 'a1b2c3d4e5g6' });
        assert.strictEqual(staticNumber(), 3456);
    });

    it('should return number by process.cwd() by default', () => {
        process.cwd = () => '1';
        const { staticNumber } = tryLoadModule();
        assert.strictEqual(staticNumber(), 50049);
    });
});
