module.exports = {
    getRes() {
        const DEFAULT_STATUS = 200;
        const r = {
            _status: DEFAULT_STATUS,
            _headers: {},
            setHeader() {
                return r;
            },
            json(json) {
                this._body = json;
                return r;
            },
            send(body) {
                this._body = body;
                return Promise.resolve(r);
            },
            end() {
                return Promise.resolve(r);
            },
            status(status) {
                this._status = status;
                return r;
            },
            getJsonBody() {
                return this._body;
            },
            getStatus() {
                return this._status;
            },
            on() {
                return r;
            },
            emit() {
                return r;
            },
            redirect() {
                return r;
            },
            type(type) {
                this._headers['Content-Type'] = type;
                return r;
            },
            getHeader(name) {
                return this._headers[name];
            },
        };
        return r;
    },
};
