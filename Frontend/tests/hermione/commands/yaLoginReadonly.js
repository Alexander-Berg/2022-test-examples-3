const { UserStorageKey } = require('../helpers/user-storage/keys');
const { UserStorageType } = require('../helpers/user-storage/types');

module.exports = async function yaLoginReadonly(login = 'user') {
    await this.setMeta('readonly', true);

    await this.authOnRecord(login);

    await this.yaOpenPage('promo');

    await this.yaUpdateUserStorage({
        [UserStorageKey.BACKGROUND_TYPE]: {
            type: UserStorageType.STRING,
            value: 'testing-gray',
        },
    });
};
