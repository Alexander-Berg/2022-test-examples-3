const WebSocket = require('ws');
const uuid4 = require('uuid').v4;

const config = require('./config');
const surfaces = require('./surfaces');
const locations = require('./locations');

const SOCKET_TIMEOUT = 5000;

module.exports = class AliceClient {
    constructor(goodwinHost, surface = surfaces.YANDEX_MINI) {
        this.goodwinHost = goodwinHost;
        this.surface = surface;
        this.uuid = this.generateUuid('deadbeef');
        this.initMessage = this.getInitMessage();
        this.location = locations.MOSCOW;
    }

    async init() {
        this.socket = new WebSocket(config.uniproxyUrl, { perMessageDeflate: true });

        return new Promise((resolve, reject) => {
            let timerId;

            this.socket.on('open', () => {
                clearTimeout(timerId);
                this.socket.send(JSON.stringify(this.initMessage));

                resolve(this);
            });

            timerId = setTimeout(() => reject(this.throwError('Socket timeout on init')), SOCKET_TIMEOUT);
        });
    }

    getInitMessage() {
        return {
            event: {
                header: {
                    namespace: 'System',
                    name: 'SynchronizeState',
                    messageId: this.generateUuid(),
                },
                payload: {
                    uuid: this.uuid,
                    auth_token: this.surface.auth_token,
                    vins: {
                        application: {
                            ...this.surface.application,
                            uuid: this.uuid,
                        },
                    },
                },
            },
        };
    }

    setLocation(location) {
        this.location = location;
    }

    getClientTime(date) {
        const clientTime = new Date(date.getTime() + -date.getTimezoneOffset() * 60 * 1000);

        return clientTime.toISOString().replace(/[:-]/g, '').replace(/\..+$/, '');
    }

    getExperiments(params) {
        let experiments = {
            analytics_info: '1',
            ...params.flags,
        };

        if (params.exp_flags) {
            for (const k in params.exp_flags) {
                experiments[`websearch_cgi_exp_flags=${k}=${params.exp_flags[k]}`] = 1;
            }
        }

        if (params.date) {
            const date = this.getClientTime(params.date).replace('T', '');

            experiments[`websearch_cgi_user_time=${date}`] = 1;
        }

        if (params.rearr) {
            experiments[`websearch_cgi_rearr=${params.rearr}`] = 1;
        }

        if (this.goodwinHost) {
            experiments[`websearch_cgi_srcrwr=TEMPLATE_RENDERER:${this.goodwinHost}`] = 1;
        }

        let customExperiments = process.env.GOODWIN_EXPERIMENTS;

        if (customExperiments) {
            try {
                customExperiments = JSON.parse(customExperiments);
                experiments = Object.assign(experiments, customExperiments);
            } catch (e) {}
        }

        return experiments;
    }

    generateUuid(prefix) {
        let uuid = uuid4();

        if (prefix) {
            uuid = prefix + uuid.slice(prefix.length);
        }

        return uuid;
    }

    generateReqid() {
        return this.generateUuid('ffffffff-ffff-ffff');
    }

    send(text, params) {
        this.socket.send(JSON.stringify(this.buildMessage(text, params)));

        return this;
    }

    read() {
        let timerId;
        const socket = this.socket;

        return new Promise((resolve, reject) => {
            socket.on('message', event => {
                if (typeof event !== 'string') return;

                try {
                    event = JSON.parse(event);
                } catch (err) {
                    this.throwError('Cannot parse message', event);
                }

                const directive = event && event.directive;
                const payload = directive && event.directive.payload || {};
                const response = payload.response || {};
                const analyticsMeta = response.meta;
                const card = response.card && response.card.text;
                const voice = payload.voice_response && payload.voice_response.output_speech &&
                    payload.voice_response.output_speech.text;
                const analytics = analyticsMeta && analyticsMeta.find(i => i.type === 'analytics_info');

                if (directive && directive.header.name === 'VinsResponse') {
                    clearTimeout(timerId);
                    resolve({ voice, card, analytics, rawEvent: event });
                }
            });

            timerId = setTimeout(() => reject(this.throwError('Socket timeout on request')), SOCKET_TIMEOUT);
        });
    }

    async request(text, params = {}) {
        this.lastReqid = null;
        this.lastMessageId = null;

        this.send(text, params);

        return this.read();
    }

    close() {
        let timerId;
        const socket = this.socket;

        socket.close();

        return new Promise((resolve, reject) => {
            socket.on('close', event => {
                clearTimeout(timerId);
                socket.terminate();
                resolve(event);
            });

            timerId = setTimeout(() => reject(this.throwError('Socket timeout on close')), SOCKET_TIMEOUT);
        });
    }

    buildMessage(text, params) {
        const experiments = this.getExperiments(params);
        const date = params.date || new Date();
        const reqid = this.generateReqid();
        const messageId = this.generateUuid();
        const customVinsUrl = process.env.GOODWIN_MEGAMIND_URL;
        const customBassUrl = process.env.GOODWIN_BASS_URL;

        this.lastReqid = reqid;
        this.lastMessageId = messageId;

        const message = {
            event: {
                header: {
                    namespace: 'VINS',
                    name: 'TextInput',
                    messageId: messageId,
                },
                payload: {
                    header: { request_id: reqid },
                    request: {
                        location: {
                            lon: this.location.lon,
                            lat: this.location.lat,
                        },
                        voice_session: true,
                        event: {
                            text: text,
                            type: 'text_input',
                        },
                        experiments,
                    },
                    application: {
                        client_time: this.getClientTime(date),
                        timezone: 'Europe/Moscow',
                        timestamp: String(Math.floor(date.getTime() / 1000)),
                        lang: 'ru-RU',
                    },
                    additional_options: {
                        bass_options: {
                            region_id: this.location.region,
                        },
                    },
                },
            },
        };

        if (customVinsUrl) {
            message.event.payload.vinsUrl = customVinsUrl;
        }

        if (customBassUrl) {
            message.event.payload.additional_options.bass_url = customBassUrl;
        }

        return message;
    }

    get setraceUrl() {
        return [
            `Setrace Uuid: https://setrace.yandex-team.ru/ui/alice/sessionsList?trace_by=${this.uuid}`,
            this.lastReqid ?
                `Setrace Reqid: https://setrace.yandex-team.ru/ui/alice/sessionsList?trace_by=${this.lastReqid}` :
                '',
            this.lastMessageId ?
                `Setrace messageId: https://setrace.yandex-team.ru/ui/alice/sessionsList?trace_by=${this.lastMessageId}` :
                '',
        ].filter(Boolean).join('\n');
    }

    throwError(message, additionalData) {
        message = `${message}\nInitMessage: ${JSON.stringify(this.initMessage)}` +
            (additionalData ? `\nadditionalData: ${additionalData}` : '') +
            `\nSetrace URL: ${this.setraceUrl}`;

        throw new Error(message);
    }
};
