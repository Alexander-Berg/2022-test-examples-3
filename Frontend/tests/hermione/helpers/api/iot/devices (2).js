/**
 * @class DevicesV2
 * @type {{methods: {addDeviceName: (function({id: *, name: *}): {path: string, options: {method: string, body: string}})}, namespace: string}}
 */
module.exports = {
    namespace: '/m/user/devices',
    methods: {
        addDeviceName: ({ id, name }) => ({
            path: `/${id}/name`,
            options: {
                method: 'POST',
                body: JSON.stringify({
                    name,
                }),
            },
        }),
    },
};
