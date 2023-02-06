import UnitOfWork from '../../../../src/infrastructure/base-implementations/UnitOfWork';
import SelectormanRepository from '../../../../src/infrastructure/repositories/SelectormanRepository';
import NotificationRepository from '../../../../src/infrastructure/repositories/NotificationRepository';
import ServiceProvider from '../../../../src/core/base-implementations/ServiceProvider';
import SelectormanId from '../../../../src/domain/models/selectorman/SelectormanId';
import QNotificationService from '../../../../src/infrastructure/services/QNotificationService';
import RepositoryProvider from '../../../../src/core/base-implementations/RepositoryProvider';
import Notification from '../../../../src/domain/models/notification/Notification';
import AppConfig from '../../../../src/AppConfig';

describe.skip('Q notification service', () => {
    beforeAll(async () => {
        await AppConfig.init();
    });

    test('it should notify the selectorman', async () => {
        const repositoryProvider = new RepositoryProvider()
            .addRepository(SelectormanRepository)
            .addRepository(NotificationRepository);

        // prettier-ignore
        const serviceProvider = new ServiceProvider()
            .addService(QNotificationService);

        const unitOfWork = await UnitOfWork.create(serviceProvider, repositoryProvider);

        const notificationService = await unitOfWork.service(QNotificationService);

        // prettier-ignore
        const notificationId = await unitOfWork
            .repository(Notification)
            .then(repository => repository.nextIdentity());

        const notification = new Notification({
            id: notificationId,
            title: 'This message is created for testing purpose',
            text:
            // eslint-disable-next-line max-len
                '**This message is created for testing purpose**. If you have any questions, feel free to ask @shtruk or @kochet',
        });

        // prettier-ignore
        await unitOfWork
            .repository(Notification)
            .then(repository => repository.add(notification));

        const selectormanId = new SelectormanId(AppConfig.TEST_SELECTORMAN_ID, AppConfig.TEST_SELECTORMAN_STAFF);

        // prettier-ignore
        await notificationService.notify(notificationId, selectormanId);

        await unitOfWork.commit()
            .then(() => unitOfWork.release());
    });

    test('it should send notification to #selectors-autocontrol channel', async () => {
        const repositoryProvider = new RepositoryProvider()
            .addRepository(SelectormanRepository)
            .addRepository(NotificationRepository);

        // prettier-ignore
        const serviceProvider = new ServiceProvider()
            .addService(QNotificationService);

        const unitOfWork = await UnitOfWork.create(serviceProvider, repositoryProvider);

        const notificationService = await unitOfWork.service(QNotificationService);

        // prettier-ignore
        const notificationId = await unitOfWork
            .repository(Notification)
            .then(repository => repository.nextIdentity());

        const notification = new Notification({
            id: notificationId,
            title: 'This message is created for testing purpose',
            text:
            // eslint-disable-next-line max-len
                '**This message is created for testing purpose**. If you have any questions, feel free to ask @shtruk or @kochet',
        });

        // prettier-ignore
        await unitOfWork
            .repository(Notification)
            .then(repository => repository.add(notification));

        // prettier-ignore
        await notificationService.report(notificationId);

        await unitOfWork.commit()
            .then(() => unitOfWork.release());
    });
});
