/**
 * @see https://github.yandex-team.ru/tools/hermione-config/blob/master/lib/injectBrowserCommands.js
 */

const fs = require('fs');
const path = require('path');
const _camelCase = require('lodash/camelCase');

const commandsFilenamesCache = new Map();

/**
 * @function
 * @param {string} directoryPath
 * @returns {string[]}
 */
const getCommandsFilenames = directoryPath => {
  const normalizedPath = path.normalize(directoryPath);

  if (commandsFilenamesCache.has(normalizedPath)) {
    return commandsFilenamesCache.get(normalizedPath);
  }

  const commandsFilenames = fs
    .readdirSync(normalizedPath, 'utf8')
    .filter(filename => path.extname(filename) === '.js');

  commandsFilenamesCache.set(normalizedPath, commandsFilenames);

  return commandsFilenames;
};

/**
 * @function
 * @param {Object} browser
 * @param {string} path
 * @returns {void}
 */
const addBrowserCommands = (browser, directoryPath) => {
  const commandsFilenames = getCommandsFilenames(directoryPath);

  for (const filename of commandsFilenames) {
    const commandName = _camelCase(path.basename(filename, '.js'));
    const commandPath = path.resolve(directoryPath, filename);

    browser.addCommand(commandName, require(commandPath));
  }
};

module.exports = {
  getCommandsFilenames,
  addBrowserCommands
};
