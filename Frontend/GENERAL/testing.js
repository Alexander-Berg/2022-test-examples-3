const { targetPath, bucket } = require('../static');

module.exports = {
    staticPath: `//${bucket}.s3.mds.yandex.net/${targetPath}`,
};
