const path = require('path');

module.exports = {
  process(src, filename) {
    return `
            module.exports = {
                id: ${JSON.stringify(path.basename(filename))}
            };
        `;
  }
};
