const { exec } = require('child_process');
const { TicketsLocators } = require('../pages/locators/tickets');

const findTicketTimeout = 40000; //таймаут нахождения созданного по письму тикета

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

module.exports = async function({ from, to, subject, body, important }) {
  //посылаем письмо с указанными параметрами
  await execMail({
    from,
    to,
    subject,
    body,
    important,
  });

  //Находим созданный по нему тикет и переходим в него
  //перейти в модуль Тикеты
  const createTicket = await this.$(TicketsLocators.TICKET_MODULE);
  await createTicket.waitForDisplayed();
  await createTicket.click();

  //перейти в очередь "Отдел CRM (тест).Autotest"
  const testQueue = await this.$(TicketsLocators.TEST_QUEUE);
  await testQueue.waitForExist();
  await testQueue.click();
  await this.pause(2000);

  //в течение указанного таймаута findTicketTimeout ищем это письмо в списке
  const mailSubject = await this.$(TicketsLocators.LATEST_CREATED_TICKET_SUBJECT);

  await mailSubject.waitUntil(
    async () => {
      await this.refresh();
      const text = await mailSubject.getText();
      return text === subject;
    },
    {
      timeout: findTicketTimeout,
      interval: 5000,
    },
  );

  //переходим в письмо, если оно найдено, и ждем его открытия
  const latestTicket = await this.$(TicketsLocators.LATEST_CREATED_TICKET);
  await latestTicket.click();
  await this.pause(2000);
};
