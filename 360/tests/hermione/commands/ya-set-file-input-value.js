const path = require('path');

const FILE_PATH = path.join(__dirname, '../assets/random-image.jpg');

const getDisplayStyle = async (browser, selector) => {
  const response = await browser.execute(passedSelector => {
    const element = document.querySelector(passedSelector);

    return element.style.display;
  }, selector);

  return response.value;
};

const setDisplayStyle = async (browser, selector, displayStyle) => {
  await browser.execute(
    (passedSelector, passesDisplayStyle) => {
      const element = document.querySelector(passedSelector);

      element.style.display = passesDisplayStyle;
    },
    selector,
    displayStyle
  );
};

/**
 * @param {string} selector
 * @param {string} filePath
 * @returns {Promise<string>}
 */
module.exports = async function yaSetFileInputValue(
  selector,
  filePath = FILE_PATH
) {
  const prevDisplayStyle = await getDisplayStyle(this, selector);
  const remoteFile = await this.uploadFile(filePath);
  const remoteFilePath = remoteFile.value;

  await setDisplayStyle(this, selector, 'block');
  await this.waitForVisible(selector);
  await this.setValue(selector, remoteFilePath);
  await setDisplayStyle(this, selector, prevDisplayStyle);

  return remoteFilePath;
};
