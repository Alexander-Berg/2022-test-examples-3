const clientObjects = require('../page-objects/client');

module.exports = {
    /**
     * @param {Object} bro
     * @param {number} count
     */
    async assertCountOfMembers(bro, count) {
        await bro.waitUntil(
            () => {
                return bro
                    .elements(clientObjects.common.videoOfParticipant())
                    .then((res) => count === res.value.length);
            },
            5000,
            `Количество участников не совпало с ожидаемым (${count})`
        );
    }
};
