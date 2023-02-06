const path = require('path');
const { getContext } = require('@yandex-int/hermione-get-context');
const fs = require('fs');

function getStateDir(test) {
    return path.join(path.dirname(test.file), 'state', test.id());
}

function getStatePath(test, stateName) {
    const filename = `${stateName}.snapshot.json`;
    const dir = getStateDir(test);

    return path.resolve(dir, filename);
}

function logFile(test, message, filepath) {
    // eslint-disable-next-line no-console
    console.log(`${test.parent.title} -> ${test.title}.`, '\n---' + message, filepath);
}

function writeFile(filepath, state) {
    // eslint-disable-next-line no-console
    console.log('---Запись файла ' + filepath);
    fs.mkdirSync(path.dirname(filepath), { recursive: true });
    fs.writeFileSync(filepath, JSON.stringify(state, null, 4));
}

module.exports = async function(stateName) {
    const test = getContext(this.executionContext);
    const filepath = getStatePath(test, stateName);

    const state = await this.yaQuasarGetAll();

    if (fs.existsSync(filepath)) {
        const stateFromFile = JSON.parse(fs.readFileSync(filepath));
        try {
            assert.deepStrictEqual(state, stateFromFile, 'Найдено различие в состоянии: ' + filepath);
        } catch (e) {
            logFile(test, 'Содержимое файла отличается', filepath);

            // Нельзя обновлять файл в режиме ci
            if (!process.env.HERMIONE_RUN) {
                writeFile(filepath, state);
            }

            throw e;
        }
    } else {
        logFile(test, 'Файл не найден', filepath);
        writeFile(filepath, state);
    }
};
