/**
 * @class UserStorage
 * @type {{methods: {set: (function(*): {path: string, options: {method: string, body: string}}), get: (function(): {path: string}), delete: (function(): {path: string, options: {method: string}})}, namespace: string}}
 */
module.exports = {
    namespace: '/m/user/storage',
    methods: {
        get: () => ({
            path: '',
        }),

        set: (userStorage) => ({
            path: '',
            options: {
                method: 'POST',
                body: JSON.stringify(userStorage),
            },
        }),

        delete: () => ({
            path: '',
            options: {
                method: 'DELETE',
            },
        }),
    },
};
