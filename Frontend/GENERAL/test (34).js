const { processMigration } = require('./utils-migration');
const PROCESS_TIMEOUT = 1000;

const query = {};
/**
 * @param {MongoCollection} collection
 * @param {MongoCollectionItem} item
 * @param {Boolean} isDryRun
 */
function processItem(collection, item, isDryRun) {
    return new Promise((resolve ) => {
        setTimeout(() => {
            console.log(`processItem ${item.id}, ${isDryRun}`);
            resolve();
        }, PROCESS_TIMEOUT );
    });
}

processMigration( query, processItem );
