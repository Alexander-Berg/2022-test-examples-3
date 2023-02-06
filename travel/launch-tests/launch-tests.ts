import {Logger} from 'src/utilities/logger';
import getEnvVariable, {
    EEnvironmentVariables,
} from 'src/utilities/getEnvVariable';

import ThiriumService from 'src/services/thirium/thirium';

import config, {EThiriumConfigPreset, isThiriumConfigPreset} from './config';
import {delay} from 'src/utilities/delay';
import {ELaunchStatus} from 'src/services/thirium/types';
import EThiriumTestFailedType, {
    isThiriumTestFailedType,
} from 'registry/thirium/launch-tests/types/EThiriumTestFailedType';
import getThiriumLaunchUrl from './utilities/getThiriumLaunchUrl';
import addThiriumBadge from './utilities/addThiriumBadge';

const MAX_RETRIES = 120;
const STATUS_POLLING_DELAY = 30 * 1000;

(async function () {
    const logger = new Logger('THIRIUM:LAUNCH_TESTS');

    try {
        const token = getEnvVariable(EEnvironmentVariables.THIRIUM_TOKEN);

        const presetName = getEnvVariable(EEnvironmentVariables.THIRIUM_PRESET);

        if (!isThiriumConfigPreset(presetName)) {
            logger.error(
                `Unexpected THIRIUM_PRESET value ${presetName}, allow values ${Object.values(
                    EThiriumConfigPreset,
                ).join(', ')}`,
            );

            return;
        }

        const testFailedType = getEnvVariable(
            EEnvironmentVariables.THIRIUM_TEST_FAILED,
            EThiriumTestFailedType.ERROR,
        );

        if (!isThiriumTestFailedType(testFailedType)) {
            logger.error(
                `Unexpected THIRIUM_TEST_FAILED value ${testFailedType}, allow values ${Object.values(
                    EThiriumTestFailedType,
                ).join(', ')}`,
            );

            return;
        }

        const thiriumClient = new ThiriumService({
            oauth: token,
        });

        const preset = config[presetName];

        const launch = await thiriumClient.launchTests(preset);

        const launchId = launch.id;
        const url = getThiriumLaunchUrl(launchId);

        logger.log(`Thirium launch. Report ${url}`);

        for (let i = 0; i < MAX_RETRIES; i++) {
            const launchStatus = await thiriumClient.launchStatus(launchId);

            if (launchStatus.status === ELaunchStatus.FINISHED) {
                await addThiriumBadge({
                    status: 'SUCCESSFUL',
                    text: `Thirium finished`,
                    url,
                });

                logger.success('ok');

                return;
            }

            if (launchStatus.status === ELaunchStatus.FAILED) {
                await addThiriumBadge({
                    status: 'FAILED',
                    text: `Thirium failed`,
                    url,
                });

                const message = `Thirium failed. Report ${url}`;

                if (testFailedType === EThiriumTestFailedType.ERROR) {
                    logger.error(message);
                } else {
                    logger.success(message);
                }

                return;
            }

            logger.log(`Thirium progress ${launchStatus.finishedPercent}%`);

            await delay(STATUS_POLLING_DELAY);
        }

        await addThiriumBadge({
            status: 'FAILED',
            text: 'Thirium stopped. Reason: over max retry',
            url,
        });
    } catch (error) {
        if (error instanceof Error) {
            logger.logError(error);
        }
    }
})();
