'use strict';

const Config = require('./base.js');

class TestingConfig extends Config {
  constructor(core) {
    super(core);

    // Добавляем статичные свойства конструктора
    Object.assign(this, TestingConfig);

    this.IS_CORP = false;
    this.IS_PROD = false;

    this.env = 'testing';

    this.metrikaCID = 42367769;

    this.services.abook = 'http://abook.tst.mail.yandex.net';
    this.services.aceventura = 'http://aceventura-search-qa.n.yandex-team.ru';
    this.services.ava = 'http://ava-qa.mail.yandex.net';
    this.services.blackbox = 'https://blackbox-mimino.yandex.net/blackbox';
    this.services.calendar = 'https://calendar-public-testing.common.yandex.net';
    this.services.catdog = 'https://catdog-qa.mail.yandex.net';
    this.services.collie = 'http://collie-qa.mail.yandex.net';
    this.services.datasync = 'http://cloud-api.dst.yandex.net:8080/v1';
    this.services.directory = 'https://api-integration-qa.directory.ws.yandex.net/v7';
    this.services.reminders = 'http://reminders-test-back.cmail.yandex.net:80/api/v1';
    this.services.telemost = 'http://api-mimino.dst.yandex.net:8080';

    this.urls.connectForbidden = 'https://connect-integration-qa.ws.yandex.ru/portal/forbidden';

    this.bunkerVersion = 'latest';
  }
}

module.exports = TestingConfig;
