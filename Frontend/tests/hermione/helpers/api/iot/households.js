/**
 * @class Households
 * @type {{methods: {addHousehold: (function({name: *, address: *}): {path: string, options: {method: string, body: string}}), get: (function(): {path: string}), removeHousehold: (function(id: *): {path: string, options: {method: string}})}, namespace: string}}
 */
module.exports = {
    namespace: '/m/user/households',
    methods: {
        get: () => ({
            path: '',
        }),

        removeHousehold: ({ id }) => ({
            path: `/${id}`,
            options: {
                method: 'DELETE',
            },
        }),

        addHousehold: ({ name, address }) => ({
            path: '',
            options: {
                method: 'POST',
                body: JSON.stringify({ name, address }),
            },
        }),
    },
};
