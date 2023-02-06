const stylus = require('stylus');

module.exports = {
  process(src, filename, config) {
    const css = stylus(src)
      .set('filename', filename)
      .import(`${config.rootDir}/styles/variables.styl`)
      .render()
      .replace(':export', '')
      .replace(/:\s([^;]*)/g, ': "$1"')
      .replace(/;/g, ',');

    return `module.exports = ${css};`;
  }
};
