let transactions = [];

export const dbs = {
    test_notifications_db: {
        meta: {
            id: {
                unviewed_count: 15,
            },
        },
        test_blocks: {
            abc01: {
                is_read: false,
                mtime: 1,
                meta:
                    '{"entity":{"type":"resource","resource_type":"dir","preview":"downloader.yandex.com.tr"}}',
                preview: 'entity',
            },
            abc02: {
                is_read: false,
                mtime: 2,
            },
            abc03: {
                is_read: true,
                mtime: 5,
                meta:
                    '{"username":{"text":"User 1"},"link":{"type":"resource","text":"very-very-very-very-very-looooong.file.txt"}}', // eslint-disable-line max-len
            },
            abc04: {
                is_read: true,
                mtime: 7,
                meta:
                    '{"username":{"text":"User 1"},"link":{"type":"link","text":"very-very-very-very-very-looooong.link.txt"}}', // eslint-disable-line max-len
            },
            abc05: {
                is_read: true,
                mtime: 10,
                meta: '{}',
            },
            abc06: {
                is_read: true,
                mtime: 11,
                meta: '{}',
            },
            abc07: {
                is_read: false,
                mtime: 12,
                meta: '{"actor":{"type":"user","login":"someuser"}}',
            },
            abc08: {
                is_read: false,
                mtime: 13,
                meta: '{"username":{}}',
            },
            abc09: {
                meta: '{',
            },
        },
        test_message_en: {
            abc01: {
                message: 'Dummy message 01',
            },
            abc02: {
                message: 'Dummy message 02',
            },
            abc03: {
                message: '%{username} please open %{link}',
            },
            abc04: {
                message: '%{username} please open %{link}',
            },
            abc06: {
                message: '%{username} please open %{link}',
            },
            abc08: {
                message: '%{username} please sign in',
            },
        },
        match_blocks: {
            mtch01: {
                is_read: false,
                mtime: 5,
            },
        },
        match_message_en: {
            mtch01: {
                message: 'Match notification',
            },
        },
    },
    test_settings_db: {
        test_common_settings: {
            all: {
                call_to_ticket: false,
                unexisting: true,
            },
        },
    },
    test_service_db: {
        meta: {
            index: {
                notifications_database_id: 'test_notifications_db',
                settings_database_id: 'test_settings_db',
                avatar_url_template:
                    'https://avatars.mds.yandex.net/get-yapic/default/islands-retina-middle',
            },
        },
        services: {
            test: {
                block_collection: 'test_blocks',
                message_collection: 'test_message',
                settings_collection: 'test_common_settings',
                unviewed_collection: 'meta',
                unviewed_record: 'id',
            },
        },
        service_names_en: {
            test: {
                name: 'Test',
            },
        },
        test_groups_en: {
            setting: {
                text: 'Setting',
            },
            preference: {
                text: 'Preference',
            },
            nonexistent: {},
        },
    },
};

class Value {
    constructor(value) {
        this.value = value;
    }
    valueOf() {
        return this.value;
    }
}

class Record {
    constructor(data) {
        Object.assign(this, data);

        Object.defineProperty(this, 'getRecordId', {
            writable: false,
            enumerable: false,
            value: () => data.recordId,
        });
    }

    getFieldValue(key) {
        return new Value(this[key]);
    }
}

class Transaction {
    constructor(db) {
        this.db = db;
        this.operations = [];
    }
    insertRecords(records) {
        this.operations.push({
            type: 'insert',
            records,
        });
        return this;
    }
    updateRecordFields(record, fields) {
        this.operations.push({
            type: 'update',
            record,
            fields,
        });
        return this;
    }
    push() {
        this.operations.forEach(operation => {
            switch (operation.type) {
                case 'update': {
                    const databaseId = this.db.config.databaseId;
                    const collectionId =
                        this.db.config.collectionId ||
                        operation.record.collectionId;
                    Object.assign(
                        dbs[databaseId][collectionId][
                            operation.record.recordId
                        ],
                        operation.fields
                    );
                    break;
                }
            }
        });
        return Promise.resolve(this);
    }
}

class DB {
    constructor(config) {
        this.config = config;
    }
    getRecord(collection, record) {
        let collectionId = collection;
        let recordId = record;

        if (!record) {
            collectionId = this.config.collectionId;
            recordId = collection;
        }

        if (dbs[this.config.databaseId][collectionId][recordId]) {
            return new Record({
                recordId,
                collectionId,
                ...dbs[this.config.databaseId][collectionId][recordId],
            });
        }
    }
    createTransaction() {
        const transaction = new Transaction(this);

        transactions.push(transaction);

        return transaction;
    }
    forEach(collectionId, fn) {
        for (const recordId in dbs[this.config.databaseId][collectionId]) {
            fn(
                new Record({
                    recordId,
                    collectionId,
                    ...dbs[this.config.databaseId][collectionId][recordId],
                }),
                recordId
            );
        }
    }
}

export function openDatabase(config) {
    return Promise.resolve(new DB(config));
}

export function processRecord(args) {
    return args;
}

export function closeDatabase() {}

export function getLastTransaction() {
    return transactions[transactions.length - 1];
}
export function truncateTransactions() {
    transactions = [];
}

export function deleteUnviewedField() {
    delete dbs.test_notifications_db.meta.id.unviewed_count;
}
export function restoreUnviewedField() {
    dbs.test_notifications_db.meta.id.unviewed_count = 10;
}

export function deleteMetaId() {
    delete dbs.test_notifications_db.meta.id;
}
export function restoreMetaId() {
    dbs.test_notifications_db.meta.id = { unviewed_count: 10 };
}

export function deleteSettingsField() {
    delete dbs.test_settings_db.test_common_settings.all;
}
export function restoreSettingsField() {
    dbs.test_settings_db.test_common_settings.all = {
        call_to_ticket: false,
        unexisting: true,
    };
}
