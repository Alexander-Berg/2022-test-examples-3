const calls = [];

export default {
    onUpdateCounter(...args) {
        calls.push({ procedure: 'onUpdateCounter', args });
    },
    onDataLoadOnce(...args) {
        calls.push({ procedure: 'onDataLoadOnce', args });
    },

    getLastCall() {
        return calls[calls.length - 1];
    },
};
