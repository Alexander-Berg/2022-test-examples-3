import React from 'react';

import {prepareQaAttributes} from 'utilities/qaAttributes/qaAttributes';

import Text from 'components/Text/Text';
import Box from 'components/Box/Box';
import SecureIFrameProxy from 'components/SecureIFrameProxy/SecureIFrameProxy';
import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';

import cx from './Test3DSExternalDemoPage.scss';

const ROOT_QA = 'test3DSExternalDemoPage';

interface ITest3DSExternalDemoPageProps {}

const Test3DSExternalDemoPage: React.FC<ITest3DSExternalDemoPageProps> = () => {
    return (
        <CardWithDeviceLayout>
            <Text size="m">
                Пример открытия сайтов, которые не разрешены в политиках csp
                приложения
            </Text>
            <Box above={4}>
                <SecureIFrameProxy
                    className={cx('iframe')}
                    src="https://www.pochta.ru/"
                    {...prepareQaAttributes({
                        parent: ROOT_QA,
                        current: 'iframe',
                    })}
                />
            </Box>
        </CardWithDeviceLayout>
    );
};

export default Test3DSExternalDemoPage;
