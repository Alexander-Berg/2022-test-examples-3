const {resolve} = require('path');

const [accessKey, secretAccessKey] = (
    process.env.S3_ROBOT_CREDENTIALS || ''
).split(':');

module.exports = {
    socket: resolve(__dirname, '../run/node.sock'),
    environmentConfig: {},
    servicesAPI: {
        hotelsAPI: 'https://api.travel-balancer-test.yandex.net/api',
    },
    s3Settings: {
        accessKey,
        secretAccessKey,
        s3Host: 'https://s3.mdst.yandex.net',
        s3BucketName: 'hotel-order-voucher',
    },
};
