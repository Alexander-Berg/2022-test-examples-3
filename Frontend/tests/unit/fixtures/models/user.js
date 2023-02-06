module.exports = {
    _data: [
        {
            login: 'eroshinev',
            token: '**************************************',
            lastClosedNotificationId: '1507718735691',
        },
        {
            login: 'gwer',
            token: '**************************************',
            lastClosedNotificationId: '1507720941349',
        },
    ],

    set() {
        return Promise.resolve();
    },

    get() {
        return Promise.resolve(this._data[0]);
    },

    disableNotification() {
        return Promise.resolve();
    },
};
