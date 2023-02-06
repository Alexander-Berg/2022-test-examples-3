const Entity = require('bem-page-object').Entity;

const sMessage = new Entity({ block: 's-message' });
const sMessageTypeSuccess = new Entity({ block: 's-message' }).mods({ type: 'success' });
const sMessageTypeError = new Entity({ block: 's-message' }).mods({ type: 'error' });

module.exports = {
    sMessage,
    sMessageTypeSuccess,
    sMessageTypeError,
};
