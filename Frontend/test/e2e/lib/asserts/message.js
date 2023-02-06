module.exports.ErrorMessage = class ErrorMessage {
    constructor(messagePrefix, client, data) {
        this.messagePrefix = messagePrefix;
        this.client = client;
        this.data = data;
    }

    toString() {
        return [
            this.messagePrefix,
            this.client.setraceUrl,
            `Ответ uniproxy: ${JSON.stringify(this.data, null, 4)}\n\n`
        ].join('\n');
    }

    static create(message, client, data) {
        return new ErrorMessage(message, client, data);
    }
};
