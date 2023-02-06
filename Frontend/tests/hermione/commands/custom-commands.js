const path = require('path');
const fs = require('fs');
const _ = require('lodash');
const platformsConfig = require('../../../.config/.platforms.conf');

/**
 * Загружает функцию команды из указанного файла и добавляет в коллекцию команд
 * Изменяет объект commands
 *
 * @param {CommandsCollection} commands - Коллекция команд
 * @param {String} filepath - путь до файла с командой
 *
 * @returns {CommandsCollection}
 */
function assignCommand(commands, filepath) {
    return path.extname(filepath) === '.js' ?
        Object.assign(commands, { [path.basename(filepath, '.js')]: require(filepath) }) :
        commands;
}

/**
 * По имени платформы возвращает список названий уровней переопределения
 * @param {String} platform - Название платформы
 * @returns {String[]}
 */
function getLevels(platform) {
    const levels = platformsConfig[platform];

    if (!Array.isArray(levels)) { throw new Error(`Для платформы ${platform} не указаны уровни переопределения`) }

    return levels;
}

module.exports = class CustomCommands {
    /**
     * Папки, из которых нужно прочитать команды
     * @param {String[]} dirs
     */
    constructor(dirs) {
        this.dirs = dirs;

        /** @type Object.<String, CommandsCollection> */
        this._commandsCache = {};
    }

    /**
     * Смерджить команды для платформы по всем уровням переопределения
     * @param {String} platform
     * @param {String} app
     *
     * @returns {CommandsCollection}
     */
    get(platform, app) {
        let levels = getLevels(platform);

        if (app) {
            levels.push(app);
        }

        levels.forEach(this._collectCommands, this);

        return _(this._commandsCache)
            .at(levels)
            .compact()
            .reduce((res, obj) => Object.assign(res, obj), {});
    }

    /**
     * Добавить команды в кэш для указанного уровня переопределения
     * Добавляет структуры вида Object.<String, CommandsCollection>
     * @example
     * пример стуктуры { level: { cmd1: fn1, cmd2: fn2 } }
     * @param {String} level
     */
    _collectCommands(level) {
        if (this._commandsCache[level]) { return }

        this._commandsCache[level] = {};

        this.dirs.forEach(dir => {
            const levelPath = path.resolve(__dirname, dir, level);

            if (!fs.existsSync(levelPath)) { return }

            const readCommands = fs.readdirSync(levelPath)
                .map(name => path.resolve(levelPath, name))
                .reduce(assignCommand, {});

            this._commandsCache[level] = Object.assign(this._commandsCache[level], readCommands);
        });
    }
};

/**
 * Объект, содержащий пары <имя команды: функция-реализация команды>
 * @typedef Object.<String, Function> CommandsCollection
 */
