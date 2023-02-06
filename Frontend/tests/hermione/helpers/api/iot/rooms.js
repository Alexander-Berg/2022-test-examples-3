/**
 * @class Rooms
 * @type {{methods: {getRoomEdit: (function({id: *}): {path: string|string}), removeRoom: (function(*): {path: string, options: {method: string}}), getRooms: (function(): {path: string}), createRoom: (function({name: *, devicesIds?: *, householdId?: *}): {path: string, options: {method: string, body: string}}), updateRoom: (function({id: *, name: *, devicesIds?: *}): {path: string, options: {method: string, body: string}}), getAvailableDevices: (function({id: *}): {path: string|string})}, namespace: string}}
 */
module.exports = {
    namespace: '/m/user/rooms',
    methods: {
        getRooms: () => ({
            path: '',
        }),

        createRoom: ({ name, devicesIds, householdId }) => ({
            path: '',
            options: {
                method: 'POST',
                body: JSON.stringify({
                    name,
                    devices: devicesIds,
                    household_id: householdId,
                }),
            },
        }),

        getRoomEdit: ({ id }) => ({
            path: id ? `/${id}/edit` : '/add',
        }),

        updateRoom: ({ id, name, devicesIds }) => ({
            path: `/${id}`,
            options: {
                method: 'PUT',
                body: JSON.stringify({
                    name,
                    devices: devicesIds,
                }),
            },
        }),

        removeRoom: (id) => ({
            path: `/${id}`,
            options: {
                method: 'DELETE',
            },
        }),

        getAvailableDevices: ({ id }) => ({
            path: id ? `/${id}/devices/available` : '/add/devices/available',
        }),
    },
};
