import React, {useCallback, useState} from 'react';

import {WEB, IOS, ANDROID} from 'server/constants/platforms';

import {
    readFakePlatformCookie,
    setFakePlatformCookie,
} from './utilities/fakePlatformCookie';
import {
    readApplicationCookie,
    removeApplicationCookie,
    setApplicationCookie,
} from 'projects/testControlPanel/pages/TestTravelAppPage/utilities/applicationCookie';

import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';
import RadioButtonSlide from 'components/RadioButtonSlide/RadioButtonSlide';
import FieldLabel from 'components/FieldLabel/FieldLabel';
import Flex from 'components/Flex/Flex';

const TestTravelAppPage: React.FC = () => {
    const [platform, setPlatform] = useState(readFakePlatformCookie());
    const [application, setApplication] = useState(readApplicationCookie());

    const handlePlatformChange = useCallback((value: string) => {
        setPlatform(value);

        setFakePlatformCookie(value);

        if (value === WEB) {
            removeApplicationCookie();
        } else {
            setApplicationCookie('true');
        }

        setApplication(readApplicationCookie());
    }, []);
    const handleApplicationChange = useCallback((value: string) => {
        setApplication(value);

        setApplicationCookie(value);
    }, []);

    return (
        <CardWithDeviceLayout>
            <Flex flexDirection="column" between={3}>
                <FieldLabel label="Платформа">
                    <RadioButtonSlide
                        name="platform"
                        value={platform}
                        onChange={handlePlatformChange}
                    >
                        <RadioButtonSlide.Option value={WEB}>
                            web
                        </RadioButtonSlide.Option>
                        <RadioButtonSlide.Option value={IOS}>
                            ios
                        </RadioButtonSlide.Option>
                        <RadioButtonSlide.Option value={ANDROID}>
                            android
                        </RadioButtonSlide.Option>
                    </RadioButtonSlide>
                </FieldLabel>
                {platform !== WEB && (
                    <FieldLabel label="Приложение">
                        <RadioButtonSlide
                            name="application"
                            value={application}
                            onChange={handleApplicationChange}
                        >
                            <RadioButtonSlide.Option value="true">
                                Travel App
                            </RadioButtonSlide.Option>
                            <RadioButtonSlide.Option value="false">
                                Avia App
                            </RadioButtonSlide.Option>
                        </RadioButtonSlide>
                    </FieldLabel>
                )}
            </Flex>
        </CardWithDeviceLayout>
    );
};

export default TestTravelAppPage;
