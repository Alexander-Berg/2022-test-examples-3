const { exec } = require('child_process');
const MailLocators = require('../pages/locators/mail');

const RECEIVE_MAIL_MS = 90 * 1000; //значение таймаута для получения письма

const mailInput = ({ from, to, subject, important, body }) => {
  return (
    `EHLO test
MAIL FROM: ${from}
${to.map((rt) => `RCPT TO: ${rt}`).join('\n')}
DATA
To: ${to.join(', ')}
From: ${from}
Subject: ${subject}
` +
    (important ? 'X-Priority: 1\n' : '') +
    `MIME_Version: 1.0
Content-Transfer-Encoding: 8bit
Content-Type: text/html; charset=utf-8

${body}

.
QUIT
`
  );
};

const execMail = async ({ from, to, subject, body, important }) => {
  try {
    return await exec(
      `echo '${mailInput({
        from,
        to,
        subject,
        body,
        important,
      })}' | nc outbound-relay.yandex.net 25`,
    );
  } catch (error) {
    console.error(error);
  }
};

module.exports = async function({
  subject = 'Hello ✔',
  text = 'test mail',
  important = undefined,
}) {
  //если счетчик входящих найден, то берем значение из него
  let mailCounter = await this.$(MailLocators.INBOX_MAIL_COUNTER);
  //если счетчик входящих не отображается, то считаем, что его значение после отправки письма должно стать 1
  let nextCount = '1';

  //если счетчик входящих найден, то берем значение из него и увеличиваем на 1
  if (await mailCounter.isDisplayed()) {
    nextCount = String(Number(await mailCounter.getText()) + 1);
  }

  //посылаем письмо с указанными параметрами
  await execMail({
    from: '<robot-space-odyssey@yandex-team.ru>',
    to: ['<test-robot-space-odyssey@yandex-team.ru>', '<robot-tcrm-test-sp@yandex-team.ru>'],
    subject,
    body: text,
    important,
  });

  //ждем, пока счетчик входящих отобразится, если его нет
  await mailCounter.waitForDisplayed({ timeout: RECEIVE_MAIL_MS });

  //ждем, пока счетчик увеличится на 1
  return mailCounter.waitUntil(
    async () => {
      const text = await mailCounter.getText();
      return text === nextCount;
    },
    {
      timeout: RECEIVE_MAIL_MS,
    },
  );
};
