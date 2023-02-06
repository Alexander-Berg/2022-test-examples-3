/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import config from '../../../services/config';
import * as serviceBlackbox from '../../../services/blackbox';
import * as serviceMail from '../../../services/mail';
import {
    defaultSender,
    organizationChatSender,
    makeStandartUserAddressingText,
    sendEmailConfirmationEmail,
    sendPingUnanswersAlert,
    sendReviewApprovedNotification,
    sendReviewCancelledNotification,
    sendSkillDeployCompletedNotification,
    sendSkillDeployRejectedNotification,
    sendSkillStoppedNotification,
    sendSkillUpdatedNotification,
    sendTycoonRubricsChangedNotification,
} from '../../../services/mail';
import { Channel } from '../../../db/tables/settings';

const urlRoot = config.app.developerConsolePath;

test.afterEach.always(async() => {
    sinon.restore();
});

test.serial('test getYandexoidLogin with hired user', async t => {
    sinon.stub(serviceBlackbox, 'getUserInfo').value(async() => {
        return {
            body: {
                users: [
                    {
                        aliases: {
                            '13': 'staff@yandex-team.ru',
                        },
                    },
                ],
            },
        };
    });

    const staffLogin = await serviceBlackbox.getYandexoidLogin('11111'); // user_name@yandex.ru

    t.is(staffLogin, 'staff@yandex-team.ru');
});

test.serial('test getYandexoidLogin with fired user', async t => {
    sinon.stub(serviceBlackbox, 'getUserInfo').value(async() => {
        return {
            body: {
                users: [
                    {
                        aliases: {},
                    },
                ],
            },
        };
    });

    const staffLogin = await serviceBlackbox.getYandexoidLogin('11111'); // user_name@yandex.ru

    t.is(staffLogin, undefined);
});

test.serial('test isYandexoid with hired user', async t => {
    sinon.stub(serviceBlackbox, 'getUserInfo').value(async() => {
        return {
            body: {
                users: [
                    {
                        aliases: {
                            '13': 'staff@yandex-team.ru',
                        },
                    },
                ],
            },
        };
    });

    const staffLogin = await serviceBlackbox.isYandexoid('11111'); // user_name@yandex.ru

    t.is(staffLogin, true);
});

test.serial('test isYandexoid with fired user', async t => {
    sinon.stub(serviceBlackbox, 'getUserInfo').value(async() => {
        return {
            body: {
                users: [
                    {
                        aliases: {},
                    },
                ],
            },
        };
    });

    const staffLogin = await serviceBlackbox.isYandexoid('11111'); // user_name@yandex.ru

    t.is(staffLogin, false);
});

test.serial('test isYandexoid with fired user (blackbox missed aliases)', async t => {
    sinon.stub(serviceBlackbox, 'getUserInfo').value(async() => {
        return {
            body: {
                users: [{}],
            },
        };
    });

    const staffLogin = await serviceBlackbox.isYandexoid('11111'); // user_name@yandex.ru

    t.is(staffLogin, false);
});

test.serial('check standart user addressing text', async t => {
    t.is(
        makeStandartUserAddressingText(`
        это тестовый ответ1
        это тестовый ответ2
        `).replace(/<br>/g, '<br>\n'),
        `Здравствуйте!<br>
<br>
это тестовый ответ1<br>
это тестовый ответ2<br>
<br>
Если у вас остались вопросы, задайте их в ответе на это письмо.<br>
Если у вас есть пожелания по улучшению функциональности платформы Диалогов, вы можете рассказать о них через форму https://forms.yandex.ru/surveys/10025990.a48b70dbb86431c1b2f67128aff4e19a8e69248d/<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.`,
    );
});

test.serial('check sendReviewCancelledNotification (AliceSkill)', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendReviewCancelledNotification(
        { id: '1', hasSubscription: true },
        '001',
        'Тестовый навык',
        Channel.AliceSkill,
        'Тут причина отказа',
    );

    t.true(sendEmailStub.calledOnce);
    t.snapshot(sendEmailStub.getCall(0).args);
});

test.serial('check sendReviewCancelledNotification (SmartHome)', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendReviewCancelledNotification(
        { id: '1', hasSubscription: true },
        '001',
        'Тестовый навык',
        Channel.SmartHome,
        'Тут причина отказа',
    );

    t.true(sendEmailStub.calledOnce);
    t.snapshot(sendEmailStub.getCall(0).args);
});

test.serial('check sendReviewCancelledNotification (OrganizationChat)', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendReviewCancelledNotification(
        { id: '1', hasSubscription: true },
        '001',
        'Тестовый навык',
        Channel.OrganizationChat,
        'Тут причина отказа',
    );

    t.true(sendEmailStub.calledOnce);
    t.snapshot(sendEmailStub.getCall(0).args);
});

test.serial('check sendReviewApprovedNotification', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendReviewApprovedNotification({ id: '1', hasSubscription: true }, Channel.AliceSkill);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        defaultSender,
        'test@yandex.ru',
        {
            subject: 'Диалог одобрен',
            body: `Здравствуйте!<br>
<br>
Диалог одобрен модератором, и вы можете <a href="${urlRoot}">опубликовать</a> его.<br>
<br>
Если у вас остались вопросы, задайте их в ответе на это письмо.<br>
Если у вас есть пожелания по улучшению функциональности платформы Диалогов, вы можете рассказать о них через форму https://forms.yandex.ru/surveys/10025990.a48b70dbb86431c1b2f67128aff4e19a8e69248d/<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.<br>
<br>
Чтобы отписаться от уведомлений, перейдите по <a href="${urlRoot}/unsubscribe?token=xxx">ссылке</a>.`.replace(
    /\n/g,
    '',
),
        },
    ]);
});

test.serial('check sendReviewApprovedNotification (OrganizationChat)', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendReviewApprovedNotification({ id: '1', hasSubscription: true }, Channel.OrganizationChat);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        organizationChatSender,
        'test@yandex.ru',
        {
            subject: 'Диалог одобрен',
            body: `Здравствуйте!<br>
<br>
Диалог одобрен модератором, и вы можете <a href="${urlRoot}">опубликовать</a> его.<br>
<br>
Возникшие вопросы можно направлять по адресу Яндекс.Диалоги <dialogs@support.yandex.ru> или через <a href="https://yandex.ru/dev/dialogs/chats/doc/feedback-docpage/">форму обратной связи</a>.<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.<br>
<br>
Чтобы отписаться от уведомлений, перейдите по <a href="${urlRoot}/unsubscribe?token=xxx">ссылке</a>.`.replace(
    /\n/g,
    '',
),
        },
    ]);
});

test.serial('check sendSkillDeployCompletedNotification', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendSkillDeployCompletedNotification({ id: '1', hasSubscription: true }, Channel.AliceSkill);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        defaultSender,
        'test@yandex.ru',
        {
            subject: 'Диалог опубликован',
            body: `Здравствуйте!<br>
<br>
Ваш диалог опубликован. Статусы диалогов можно посмотреть в <a href="${urlRoot}">Консоли разработчика</a>.<br>
<br>
Если у вас остались вопросы, задайте их в ответе на это письмо.<br>
Если у вас есть пожелания по улучшению функциональности платформы Диалогов, вы можете рассказать о них через форму https://forms.yandex.ru/surveys/10025990.a48b70dbb86431c1b2f67128aff4e19a8e69248d/<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.<br>
<br>
Чтобы отписаться от уведомлений, перейдите по <a href="${urlRoot}/unsubscribe?token=xxx">ссылке</a>.`.replace(
    /\n/g,
    '',
),
        },
    ]);
});

test.serial('check sendSkillDeployCompletedNotification (OrganizationChat)', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendSkillDeployCompletedNotification({ id: '1', hasSubscription: true }, Channel.OrganizationChat);

    t.true(sendEmailStub.calledOnce);
    t.snapshot(sendEmailStub.getCall(0).args);
});

test.serial('check sendSkillUpdatedNotification', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendSkillUpdatedNotification({ id: '1', hasSubscription: true }, 'Тестовый навык', Channel.AliceSkill);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        defaultSender,
        'test@yandex.ru',
        {
            subject: 'Изменения в диалоге опубликованы',
            body: `Здравствуйте!<br>
<br>
Изменения в диалоге "Тестовый навык" опубликованы. Статусы диалогов можно посмотреть в <a href="${urlRoot}">Консоли разработчика</a>.<br>
<br>
Если у вас остались вопросы, задайте их в ответе на это письмо.<br>
Если у вас есть пожелания по улучшению функциональности платформы Диалогов, вы можете рассказать о них через форму https://forms.yandex.ru/surveys/10025990.a48b70dbb86431c1b2f67128aff4e19a8e69248d/<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.<br>
<br>
Чтобы отписаться от уведомлений, перейдите по <a href="${urlRoot}/unsubscribe?token=xxx">ссылке</a>.`.replace(
    /\n/g,
    '',
),
        },
    ]);
});

test.serial('check sendSkillUpdatedNotification (OrganizationChat)', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendSkillUpdatedNotification({ id: '1', hasSubscription: true }, 'Тестовый навык', Channel.OrganizationChat);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        organizationChatSender,
        'test@yandex.ru',
        {
            subject: 'Изменения в диалоге опубликованы',
            body: `Здравствуйте!<br>
<br>
Изменения в диалоге "Тестовый навык" опубликованы. Статусы диалогов можно посмотреть в <a href="${urlRoot}">Консоли разработчика</a>.<br>
<br>
Возникшие вопросы можно направлять по адресу Яндекс.Диалоги <dialogs@support.yandex.ru> или через <a href="https://yandex.ru/dev/dialogs/chats/doc/feedback-docpage/">форму обратной связи</a>.<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.<br>
<br>
Чтобы отписаться от уведомлений, перейдите по <a href="${urlRoot}/unsubscribe?token=xxx">ссылке</a>.`.replace(
    /\n/g,
    '',
),
        },
    ]);
});

test.serial('check sendSkillDeployRejectedNotification', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendSkillDeployRejectedNotification({ id: '1', hasSubscription: true }, Channel.AliceSkill);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        defaultSender,
        'test@yandex.ru',
        {
            subject: 'Диалог не опубликован',
            body: `Здравствуйте!<br>
<br>
Ваш диалог не опубликован. Причину можно посмотреть в <a href="${urlRoot}">Консоли разработчика</a>.<br>
<br>
Если у вас остались вопросы, задайте их в ответе на это письмо.<br>
Если у вас есть пожелания по улучшению функциональности платформы Диалогов, вы можете рассказать о них через форму https://forms.yandex.ru/surveys/10025990.a48b70dbb86431c1b2f67128aff4e19a8e69248d/<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.<br>
<br>
Чтобы отписаться от уведомлений, перейдите по <a href="${urlRoot}/unsubscribe?token=xxx">ссылке</a>.`.replace(
    /\n/g,
    '',
),
        },
    ]);
});

test.serial('check sendSkillDeployRejectedNotification (OrganizationChat)', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendSkillDeployRejectedNotification({ id: '1', hasSubscription: true }, Channel.OrganizationChat);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        organizationChatSender,
        'test@yandex.ru',
        {
            subject: 'Диалог не опубликован',
            body: `Здравствуйте!<br>
<br>
Ваш диалог не опубликован. Причину можно посмотреть в <a href="${urlRoot}">Консоли разработчика</a>.<br>
<br>
Возникшие вопросы можно направлять по адресу Яндекс.Диалоги <dialogs@support.yandex.ru> или через <a href="https://yandex.ru/dev/dialogs/chats/doc/feedback-docpage/">форму обратной связи</a>.<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.<br>
<br>
Чтобы отписаться от уведомлений, перейдите по <a href="${urlRoot}/unsubscribe?token=xxx">ссылке</a>.`.replace(
    /\n/g,
    '',
),
        },
    ]);
});

test.serial('check sendSkillStoppedNotification', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendSkillStoppedNotification({ id: '1', hasSubscription: true }, Channel.AliceSkill);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        defaultSender,
        'test@yandex.ru',
        {
            subject: 'Диалог остановлен',
            body: `Здравствуйте!<br>
<br>
Ваш диалог остановлен. Причину можно посмотреть в <a href="${urlRoot}">Консоли разработчика</a>.<br>
<br>
Если у вас остались вопросы, задайте их в ответе на это письмо.<br>
Если у вас есть пожелания по улучшению функциональности платформы Диалогов, вы можете рассказать о них через форму https://forms.yandex.ru/surveys/10025990.a48b70dbb86431c1b2f67128aff4e19a8e69248d/<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.<br>
<br>
Чтобы отписаться от уведомлений, перейдите по <a href="${urlRoot}/unsubscribe?token=xxx">ссылке</a>.`.replace(
    /\n/g,
    '',
),
        },
    ]);
});

test.serial('check sendSkillStoppedNotification (OrganizationChat)', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendSkillStoppedNotification({ id: '1', hasSubscription: true }, Channel.OrganizationChat);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        organizationChatSender,
        'test@yandex.ru',
        {
            subject: 'Диалог остановлен',
            body: `Здравствуйте!<br>
<br>
Ваш диалог остановлен. Причину можно посмотреть в <a href="${urlRoot}">Консоли разработчика</a>.<br>
<br>
Возникшие вопросы можно направлять по адресу Яндекс.Диалоги <dialogs@support.yandex.ru> или через <a href="https://yandex.ru/dev/dialogs/chats/doc/feedback-docpage/">форму обратной связи</a>.<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.<br>
<br>
Чтобы отписаться от уведомлений, перейдите по <a href="${urlRoot}/unsubscribe?token=xxx">ссылке</a>.`.replace(
    /\n/g,
    '',
),
        },
    ]);
});

test.serial('check sendPingUnanswersAlert', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendPingUnanswersAlert(
        {
            id: '1',
            hasSubscription: true,
        },
        '001',
        'Тестовый навык',
        Channel.AliceSkill,
    );

    t.true(sendEmailStub.calledOnce);
    t.snapshot(sendEmailStub.getCall(0).args);
});

test.serial('check sendPingUnanswersAlert (OrganizationChat)', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeUnsubscribeUrl').value(() => `${urlRoot}/unsubscribe?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendPingUnanswersAlert(
        {
            id: '1',
            hasSubscription: true,
        },
        '001',
        'Тестовый навык',
        Channel.OrganizationChat,
    );

    t.true(sendEmailStub.calledOnce);
    t.snapshot(sendEmailStub.getCall(0).args);
});

test.serial('check sendEmailConfirmationEmail', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    sinon.stub(serviceMail, 'fetchEmail').value(() => 'test@yandex.ru');
    sinon.stub(serviceMail, 'makeEmailConfirmationUrl').value(() => `${urlRoot}/email?token=xxx`);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendEmailConfirmationEmail({
        userId: '1',
        skillId: '001',
        email: 'test@yandex.ru',
    });

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        defaultSender,
        'test@yandex.ru',
        {
            subject: 'Подтверждение электронного адреса',
            body: `Здравствуйте!<br>
<br>
Перейдите по <a href="${urlRoot}/email?token=xxx">ссылке</a>, чтобы подтвердить адрес электронной почты.<br>
Ссылка действительна в течение 24 часов с момента ее отправки.<br>
<br>
Если у вас остались вопросы, задайте их в ответе на это письмо.<br>
Если у вас есть пожелания по улучшению функциональности платформы Диалогов, вы можете рассказать о них через форму https://forms.yandex.ru/surveys/10025990.a48b70dbb86431c1b2f67128aff4e19a8e69248d/<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.`.replace(/\n/g, ''),
        },
    ]);
});

test.serial('check sendTycoonRubricsChangedNotification', async t => {
    sinon.stub(serviceMail, 'isNotificationsDisabled').value(() => false);
    const sendEmailStub = sinon.stub(serviceMail, 'sendEmail').resolves();

    await sendTycoonRubricsChangedNotification([
        {
            count: 1,
            value: [{ name: '0', permalink: '0' }],
        },
        {
            added: undefined,
            count: 1,
            removed: true,
            value: [{ name: '1', permalink: '1' }],
        },
        {
            added: true,
            count: 2,
            removed: undefined,
            value: [{ name: '2', permalink: '2' }, { name: '3', permalink: '3' }],
        },
    ]);

    t.true(sendEmailStub.calledOnce);
    t.deepEqual(sendEmailStub.getCall(0).args, [
        defaultSender,
        config.tycoon.rubricsChangedMailingList,
        {
            subject: 'Список рубрик Справочника был изменен',
            body: `Здравствуйте!<br>
<br>
В рубрики Справочника были внесены следующие изменения:<br>
<br>
<span style="color: red;">- : {"name":"1","permalink":"1"}</span><br>
<span style="color: green;">+ : {"name":"2","permalink":"2"}</span><br>
<span style="color: green;">+ : {"name":"3","permalink":"3"}</span><br>
<br>
<br>
Пожалуйста, убедитесь, что маппинг рубрик на категории Каталога навыков Алисы актуален.<br>
<br>
С уважением,<br>
команда Яндекс.Диалогов.`.replace(/\n/g, ''),
        },
    ]);
});
