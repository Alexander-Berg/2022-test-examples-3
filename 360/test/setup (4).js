beforeEach(function() {
    this.sinon = sinon.createSandbox();
    window.__PREFETCH = {};
    window.Ya = window.Ya || {};

    this.sinon.stub(window, 'Ya').value({
        Rum: {
            getTime: this.sinon.stub(),
            getTimeMarks: this.sinon.stub(),
            makeSubPage: this.sinon.stub(),
            sendTimeMark: this.sinon.stub(),
            sendDelta: this.sinon.stub(),

            logError: this.sinon.stub(),
            ERROR_LEVEL: {
                INFO: 'info',
                DEBUG: 'debug',
                WARN: 'warn',
                ERROR: 'error',
                FATAL: 'fatal'
            }
        }
    });
});

afterEach(function() {
    this.sinon.restore();
});
