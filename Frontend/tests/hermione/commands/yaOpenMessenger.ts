const userAliases = {
    user: 'c3ac9c9b-853e-4b3b-ac8f-ac5aef28168f',
    'echo-bot': 'ef212763-afac-488b-b099-f0e1c23cba3d',
};

function getUserGuid(guid, userAlias) {
    if (guid) {
        return guid;
    }

    if (userAlias) {
        if (!userAliases.hasOwnProperty(userAlias)) {
            throw new Error(`yaOpenMessenger: Alias ${userAlias} not exists. Available ${Object.keys(userAliases).join(', ')}`);
        }
        return userAliases[userAlias];
    }
}

interface IOpenMessenger {
    build: string,
    config: string,
    guid: string,
    chatId: string,
    userAlias: string,
    inviteHash: string,
    timestamp: string,
    waitToken: boolean,
    debug: boolean,
    customField: {
        name: string,
        value: string,
    }
}

/**
 * Открытие мессенджера и подстановка дампа
 *
 * @param {OpenMessengerOptions} options - опции команды
 *
 * @returns {Promise}
 */
module.exports = async function yaOpenMessenger(this: WebdriverIO.Browser, {
    build = 'chamb',
    config = 'development',
    guid,
    chatId,
    userAlias,
    inviteHash,
    timestamp,
    waitToken = true,
    customField,
    debug,
} = {} as IOpenMessenger) {
    const userGuid = getUserGuid(guid, userAlias);

    let url = `/chat?build=${build}&config=${config}&flags=waitToken=${waitToken ? 1 : 0}`;

    if (debug) {
        url += '&debug=';
    }

    if (customField?.name && customField?.value) {
        url += `&${customField.name}=${customField.value}`;
    }

    if (userGuid) {
        url += `&guid=${userGuid}`;
    }

    if (build === 'yamb' && chatId) {
        url += `#/chats/${encodeURIComponent(chatId)}`;

        if (timestamp) {
            url += `/${timestamp}`;
        }
    }

    if (inviteHash) {
        url += `#/join/${inviteHash}`;

        if (timestamp) {
            url += `/${timestamp}`;
        }
    }

    await this.url(url);

    if (userGuid || inviteHash || (build === 'yamb' && chatId)) {
        await this.waitForVisible('.yamb-chat', 'Чат не открылся');
    } else {
        await this.waitForVisible('.yamb-sidebar', 'Не показался список чатов');
    }

    if (chatId && build === 'chamb') {
        this.execute(function (id) {
            window.postMessage({
                type: 'iframe-open',
                payload: {
                    chatId: id,
                },
            }, '*');
        }, chatId);
    }
};

/**
 * @typedef {Object} OpenMessengerOptions
 *
 * @property {String} [build=chamb] - сборка, которую надо запустить
 * @property {String} [config=development] - конфигурационный файл
 * @property {String} [guid] - ID пользователя, с кем открыть чат
 * @property {String} [userAlias] - userAlias пользователя, с которым открыть чат. Только из списка:
 *      1) user - пользователь yndx-mssngr-tst-2 (guid 84e2c95f-7a61-42d4-a546-c3c9c3ae0397)
 *      2) echo-bot - эхо-бот (guid ef212763-afac-488b-b099-f0e1c23cba3d)
 */
