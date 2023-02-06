/**
 * @class Devices
 * @type {{methods: {addDeviceName: (function({id: string, name: string}): Promise), removeDevice: (function(id: string): Promise)}, namespace: string}}
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

        removeDevice: (id) => ({
            path: `/${id}`,
            options: {
                method: 'DELETE',
            },
        }),
    },
};
