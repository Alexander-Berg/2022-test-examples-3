/**
 * @class DevicesV2
 * @type {{methods: {getDevices: (function(): {path: string})}, namespace: string}}
 */
module.exports = {
    namespace: '/m/v2/user/devices',
    methods: {
        getDevices: () => ({
            path: '',
        }),
    },
};
