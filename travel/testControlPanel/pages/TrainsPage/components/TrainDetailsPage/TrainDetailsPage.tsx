import React, {useCallback, useState} from 'react';
import Cookie from 'js-cookie';

import {TEST_TRAIN_DETAILS_COOKIE_NAME} from 'constants/testContext';

import {ECardWithDeviceLayoutVariation} from 'components/CardWithDeviceLayout/types/ECardWithDeviceLayoutVariation';
import {ETrainDetailMock} from 'projects/testControlPanel/pages/TrainsPage/types';

import {renderSelect} from 'projects/testControlPanel/utilities/renderSelect';
import {useDeviceType} from 'utilities/hooks/useDeviceType';
import {useAsyncState} from 'utilities/hooks/useAsyncState';
import {deviceMods} from 'utilities/stylesUtils';
import {getMockImPath} from 'utilities/testUtils/getMockImPath';

import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';
import Heading from 'components/Heading/Heading';
import Flex from 'components/Flex/Flex';
import Form from 'components/Form/Form';
import Field from 'components/Form/components/Field/Field';
import Button from 'components/Button/Button';
import Text from 'components/Text/Text';
import Spinner from 'components/Spinner/Spinner';
import {TwoColumnLayout} from 'components/Layouts/TwoColumnLayout/TwoColumnLayout';
import FieldLabel from 'components/FieldLabel/FieldLabel';

import cx from './TrainDetailsPage.scss';

interface ITestTrainDetailsForm {
    train: ETrainDetailMock;
}

const TRAIN_SELECT_OPTIONS = Object.entries(ETrainDetailMock).map(
    ([trainName, value]) => ({
        value,
        data: trainName,
    }),
);

const FORM_INITIAL_VALUES = {
    train: ETrainDetailMock.SAPSAN,
};

const TrainDetailsPage: React.FC = () => {
    const deviceType = useDeviceType();

    const uiState = useAsyncState();

    const [cookie, setCookie] = useState(getMockImPath() ?? '');

    const onSubmit = useCallback(
        (formValues: ITestTrainDetailsForm) => {
            uiState.loading();

            try {
                const {train} = formValues;

                Cookie.set(TEST_TRAIN_DETAILS_COOKIE_NAME, train);

                setCookie(train);

                uiState.success();
            } catch (e) {
                uiState.error();

                return;
            }
        },
        [uiState],
    );

    const onReset = useCallback(() => {
        Cookie.remove(TEST_TRAIN_DETAILS_COOKIE_NAME);

        setCookie('');
    }, []);

    return (
        <TwoColumnLayout
            className={cx('root', deviceMods('root', deviceType))}
            deviceType={deviceType}
            rightColumnOffset={10}
            rightColumnWidth={80}
        >
            <TwoColumnLayout.LeftColumn>
                <CardWithDeviceLayout below={5}>
                    <Form<ITestTrainDetailsForm>
                        initialValues={FORM_INITIAL_VALUES}
                        onSubmit={onSubmit}
                    >
                        {({handleSubmit}): React.ReactNode => (
                            <form onSubmit={handleSubmit}>
                                <Text className={cx('title')} tag="div">
                                    ???????? ?????? ???????????????? ???????? - ????????????????????????????
                                    (AUTO ?? ??????????????) ?? ????????????. ????????????????????????????
                                    ?????????????? ???????????????? ?? ???????? ?? ????????????????????????????
                                    ?????????? ???????????? (?????????????? ??????????) ?? ??????????
                                    ???????????????????? ???????????????????? ?????? ????????????, ????????????????????
                                    ???? ????????????. ???????????? ?????????????? ????????????????????
                                    ???????????????????? ?? ????????????, ?????????????? ?????????? ???????????? ??
                                    ??????????????.
                                </Text>

                                <Flex
                                    flexDirection="column"
                                    above={4}
                                    below={8}
                                    between={5}
                                >
                                    <Field
                                        name="train"
                                        options={TRAIN_SELECT_OPTIONS}
                                        label="?????? ????????????"
                                    >
                                        {renderSelect}
                                    </Field>
                                </Flex>

                                <Flex alignItems="baseline" between={5} inline>
                                    <Button
                                        theme="primary"
                                        type="submit"
                                        size="l"
                                    >
                                        ?????????????????? ???????????????? ???????????? ????????????
                                    </Button>

                                    {uiState.isError && (
                                        <Text color="alert">
                                            ?????????????????? ????????????
                                        </Text>
                                    )}

                                    {uiState.isLoading && <Spinner size="xs" />}
                                </Flex>
                            </form>
                        )}
                    </Form>
                </CardWithDeviceLayout>
            </TwoColumnLayout.LeftColumn>

            <TwoColumnLayout.RightColumn>
                <CardWithDeviceLayout
                    className={cx('rightCard')}
                    variation={ECardWithDeviceLayoutVariation.ASIDE}
                >
                    <Heading level={2}>?????????????? ????????????????</Heading>

                    <Flex
                        flexDirection="column"
                        above={5}
                        below={5}
                        between={3}
                        style={{width: 300}}
                    >
                        <FieldLabel label={TEST_TRAIN_DETAILS_COOKIE_NAME}>
                            {cookie || '-'}
                        </FieldLabel>
                    </Flex>

                    <Button size="l" onClick={onReset}>
                        ????????????????
                    </Button>
                </CardWithDeviceLayout>
            </TwoColumnLayout.RightColumn>
        </TwoColumnLayout>
    );
};

export default TrainDetailsPage;
