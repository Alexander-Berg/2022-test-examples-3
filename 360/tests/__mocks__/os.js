const os = jest.createMockFromModule('os');

os.hostname = () => '4jbflt6ibod5okvz.sas.yp-c.yandex.net';

module.exports = os;
