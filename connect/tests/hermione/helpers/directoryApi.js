const got = require('got');
const _ = require('lodash');

const apiUrl = 'https://api-internal-test.directory.ws.yandex.net';
const settingsApiUrl = 'https://settings-test.ws.yandex.ru';

// Игнорируем ошибки о недоверии к внутреним сертификатам
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

function log(...args) {
    console.log('INFO:', ...args); // eslint-disable-line no-console
}

function error(e) {
    console.error(e); // eslint-disable-line no-console
}

function getHeaders(adminData, token = process.env.CONNECT_TOKENS__AUTH) {
    return {
        Authorization: `Token ${token}`,
        'x-uid': adminData.uid,
        'x-org-id': adminData.orgId,
        'x-user-ip': '127.0.0.1',
        'content-type': 'application/json',
    };
}

function getSettingsHeaders(adminData) {
    return getHeaders(adminData, process.env.CONNECT_TOKENS__SETTINGS);
}

/**
 * Удаляет пользователя по UID и данным администратора
 * @param {Number} uid - id удаляемого пользователя
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function dismissUser(uid, adminData) {
    log(`directoryApi.dismissUser ${uid}`);

    return got.patch(`${apiUrl}/v8/users/${uid}/`, {
        headers: getHeaders(adminData),
        body: JSON.stringify({
            is_dismissed: true,
        }),
    })
        .then(data => {
            log(`dismissed user with uid=${uid}`);

            return data;
        })
        .catch(e => {
            log('!!!Не удалось удалить пользователя.');
            log('uid', uid);
            log('adminData', adminData);
            log('ERROR', e);
        });
}

/**
 * Удаляет все лицензии трекера
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function removeTrackerLicenses(adminData) {
    log('directoryApi.removeTrackerLicenses');

    return got.put(`${apiUrl}/subscription/services/tracker/licenses/`, {
        headers: getHeaders(adminData),
        body: JSON.stringify([]),
    })
        .then(data => {
            log('remove all tracker licenses: SUCCESS');

            return data;
        })
        .catch(e => {
            log('!!!Не удалось удалить лицензии трекера.');
            log('adminData', adminData);
            log('ERROR', e);
        });
}

/**
 * Удаляет департамент по UID и данным администратора
 * @param {Number} id - id отдела
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function removeDepartment(id, adminData) {
    log(`directoryApi.removeDepartment ${id}`);

    return got.delete(`${apiUrl}/v8/departments/${id}/`, {
        headers: getHeaders(adminData),
    })
        .then(data => {
            log(`removed department with id=${id}`);

            return data;
        })
        .catch(e => {
            log('!!!Не удалось удалить отдел.');
            log('uid', id);
            log('adminData', adminData);
            error(e);
        });
}

/**
 * Удаляет команду
 * @param {Number} id - id команды
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function removeGroup(id, adminData) {
    log(`directoryApi.removeGroup ${id}`);

    return got.delete(`${apiUrl}/groups/${id}/`, {
        headers: getHeaders(adminData),
    })
        .then(data => {
            log(`removed group with id=${id}`);

            return data;
        })
        .catch(e => {
            log('!!!Не удалось удалить команду.');
            log('id', id);
            log('adminData', adminData);
            error(e);
        });
}

/**
 * Создает команду
 * @param {Object} body - данные команды
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function createGroup({ name, label }, adminData) {
    log('directoryApi.createGroup');

    return got.post(`${apiUrl}/v9/groups/`, {
        headers: getHeaders(adminData),
        body: JSON.stringify({
            name: { ru: name },
            label,
        }),
    })
        .then(({ body }) => {
            log('create group: SUCCESS');

            return JSON.parse(body);
        })
        .catch(e => {
            log('!!!Не удалось создать команду.');
            log('adminData', adminData);
            error(e);
        });
}

/**
 * Возвращает список команд
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function readGroupList(adminData) {
    log('directoryApi.readGroupList');

    return got.get(`${apiUrl}/v9/groups/`, {
        headers: getHeaders(adminData),
        query: {
            per_page: 1000,
            fields: ['type', 'label'].join(','),
        },
    })
        .then(({ body }) => {
            log('read group list: SUCCESS');

            return JSON.parse(body);
        })
        .catch(e => {
            log('!!!Не удалось получить список команд.');
            log('adminData', adminData);
            error(e);
        });
}

/**
 * Создает департамент
 * @param {Object} body - данные департамента
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function createDepartment({ name, label, parent_id: parentId }, adminData) {
    log('directoryApi.createDepartment');

    return got.post(`${apiUrl}/v8/departments/`, {
        headers: getHeaders(adminData),
        body: JSON.stringify({
            name: { ru: name },
            label,
            parent_id: parentId,
        }),
    })
        .then(({ body }) => {
            log('create department: SUCCESS');

            return JSON.parse(body);
        })
        .catch(e => {
            log('!!!Не удалось создать департамент.');
            log('adminData', adminData);
            error(e);
        });
}

/**
 * Возвращает список департаментов
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function readDepartmentList(adminData) {
    log('directoryApi.readDepartmentList');

    return got.get(`${apiUrl}/v8/departments/`, {
        headers: getHeaders(adminData),
        query: {
            per_page: 100,
            fields: ['parent_id', 'label'].join(','),
        },
    })
        .then(({ body }) => {
            log('read department list: SUCCESS');

            return JSON.parse(body);
        })
        .catch(e => {
            log('!!!Не удалось получить список департаментов.');
            log('adminData', adminData);
            error(e);
        });
}

/**
 * Удаляет домен
 * @param {String} domainName - имя домена
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function removeDomain(domainName, adminData) {
    log(`directoryApi.removeDomain ${domainName}`);

    return got.delete(`${apiUrl}/domains/${domainName}/`, {
        headers: getHeaders(adminData),
    })
        .then(data => {
            log(`Домен '${domainName}' удалён`);

            return data;
        })
        .catch(e => {
            log('[!] Не удалось удалить домен.');
            log('domainName', domainName);
            log('adminData', adminData);

            error(e);
        });
}

/**
 * Обновляет пользовательские настройки
 * @param {String} data - данные для обновления
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function updateSettings(data, adminData) {
    log(`settingsApi.update ${JSON.stringify(data)}`);

    return got.put(`${settingsApiUrl}/connect`, {
        headers: getSettingsHeaders(adminData),
        body: JSON.stringify(data),
    })
        .then(() => {
            log('Настройки обновлены');

            return null;
        })
        .catch(e => {
            log('[!] Не удалось обновить настройки');
            log('adminData', adminData);

            error(e);
        });
}

/**
 * Читает пользовательские настройки
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function fetchSettings(adminData) {
    log('settingsApi.fetch');

    return got.get(`${settingsApiUrl}/connect`, {
        headers: getSettingsHeaders(adminData),
    })
        .then(({ body }) => {
            log('Настройки прочитаны');

            return JSON.parse(body);
        })
        .catch(e => {
            log('[!] Не удалось прочитать настройки');
            log('adminData', adminData);

            error(e);
        });
}

/**
 * Удаляет пользовательскую настройку
 * @param {String} key - ключ для удаления
 * @param {Object} adminData - данные админстратора
 * @param {Number} adminData.uid - id администратора
 * @param {Number} adminData.orgId - id организации
 * @returns {Promise}
 */
function removeSettingByKey(key, adminData) {
    log(`settingsApi.remove ${key}`);

    return fetchSettings(adminData)
        .then(res => {
            const data = _.omit(res, key);

            return updateSettings(data, adminData);
        })
        .then(() => {
            log(`Настройка '${key}' удалена`);

            return null;
        })
        .catch(e => {
            log(`[!] Не удалось удалить настройку '${key}'`);
            log('adminData', adminData);

            error(e);
        });
}

module.exports = {
    dismissUser,
    removeDepartment,
    removeGroup,
    readGroupList,
    createGroup,
    createDepartment,
    readDepartmentList,
    removeDomain,
    updateSettings,
    fetchSettings,
    removeSettingByKey,
    removeTrackerLicenses,
};
