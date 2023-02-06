import React, {useEffect, useState} from 'react';
import noop from 'lodash/noop';
import Cookie from 'js-cookie';

import {IIconProps} from 'icons/types/icon';

import {useAsync} from 'utilities/hooks/useAsync';
import getActiveExperimentsFromCookies, {
    TActiveUaasExperiments,
} from './utilities/getActiveExperimentsFromCookies';
import parseExpConfigString from 'projects/testControlPanel/pages/ExperimentsPage/utilities/parseExpConfigString';
import copyToClipboard from 'utilities/copyToClipboard';

import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';
import Spinner from 'components/Spinner/Spinner';
import Text from 'components/Text/Text';
import Flex from 'components/Flex/Flex';
import Checkbox from 'components/Checkbox/Checkbox';
import Button from 'components/Button/Button';
import GoToForm from 'components/GoToForm/GoToForm';
import TextWithIcon from 'components/TextWithIcon/TextWithIcon';
import CopyIcon from 'icons/16/Copy';
import Input from 'components/Input/Input';

import useSearch from 'projects/testControlPanel/pages/ExperimentsPage/hooks/useSearch';

import {experimentsProvider} from 'serviceProvider/common/experiments/experimentsProvider';

import cx from './ExperimentsPage.scss';

const ExperimentsPage: React.FC = () => {
    const [experimentsStatus, fetchExperiments] = useAsync(
        experimentsProvider.provider()?.getExperiments || noop,
    );
    const [activeExperiments, setActiveExperiments] =
        useState<TActiveUaasExperiments>({});
    const [hasChanged, setHasChanged] = useState(false);

    useEffect(() => {
        fetchExperiments();
    }, [fetchExperiments]);

    useEffect(() => {
        if (!experimentsStatus.data) {
            setActiveExperiments({});

            return;
        }

        setActiveExperiments(
            getActiveExperimentsFromCookies(experimentsStatus.data),
        );
    }, [experimentsStatus.data]);

    const {text, setText, filteredExperiments} = useSearch({
        experiments: experimentsStatus.data,
    });

    return (
        <CardWithDeviceLayout>
            <GoToForm
                active={hasChanged}
                getBorderTopToHidden={(): number => Infinity}
            >
                <Button
                    width="max"
                    theme="primary"
                    onClick={(): void => document.location.reload()}
                >
                    Обновить страницу
                </Button>
            </GoToForm>
            {experimentsStatus.error ? (
                <Flex y={5}>
                    <Text color="alert" size="l">
                        Произошла ошибка. Повторите попытку позже
                    </Text>
                </Flex>
            ) : (
                <>
                    {experimentsStatus.loading ? (
                        <Flex inset={5} justifyContent="center">
                            <Spinner size="l" />
                        </Flex>
                    ) : (
                        experimentsStatus.data && (
                            <Flex flexDirection="column" between={4}>
                                <Input
                                    className={cx('searchInput')}
                                    placeholder="Найти эксперимент"
                                    value={text}
                                    onChange={(_, v): void => {
                                        setText(v);
                                    }}
                                />
                                {filteredExperiments.map(
                                    ([
                                        expConfigName,
                                        expConfigString,
                                    ]): React.ReactNode => {
                                        const parsedConfigString =
                                            parseExpConfigString(
                                                expConfigString,
                                            );

                                        const label = (
                                            <Flex flexDirection="column">
                                                <TextWithIcon<
                                                    IIconProps,
                                                    IIconProps
                                                >
                                                    text={expConfigName}
                                                    iconRight={CopyIcon}
                                                    iconRightProps={{
                                                        onClick(e): void {
                                                            e.preventDefault();

                                                            copyToClipboard({
                                                                text: expConfigName,
                                                            });
                                                        },
                                                    }}
                                                    textClassName={cx(
                                                        'expNameText',
                                                    )}
                                                />

                                                <TextWithIcon<
                                                    IIconProps,
                                                    IIconProps
                                                >
                                                    className={cx('expString')}
                                                    text={expConfigString}
                                                    size="s"
                                                    iconRight={CopyIcon}
                                                    iconRightProps={{
                                                        className:
                                                            cx('expStringIcon'),
                                                        onClick(e): void {
                                                            e.preventDefault();

                                                            copyToClipboard({
                                                                text: expConfigString,
                                                            });
                                                        },
                                                    }}
                                                    spaceBetween={1}
                                                    textClassName={cx(
                                                        'expStringText',
                                                    )}
                                                />
                                            </Flex>
                                        );

                                        return (
                                            <div key={expConfigName}>
                                                <Checkbox
                                                    label={label}
                                                    checked={
                                                        activeExperiments[
                                                            expConfigName as keyof TActiveUaasExperiments
                                                        ]
                                                    }
                                                    disabled={
                                                        parsedConfigString.disabled
                                                    }
                                                    onChange={(event): void => {
                                                        if (
                                                            parsedConfigString.disabled
                                                        ) {
                                                            return;
                                                        }

                                                        const {
                                                            expKey,
                                                            expValue,
                                                        } = parsedConfigString;

                                                        const checked =
                                                            event.target
                                                                .checked;

                                                        setActiveExperiments({
                                                            ...activeExperiments,
                                                            [expConfigName]:
                                                                checked,
                                                        });

                                                        if (checked) {
                                                            Cookie.set(
                                                                expKey,
                                                                expValue,
                                                            );
                                                        } else {
                                                            Cookie.remove(
                                                                expKey,
                                                            );
                                                        }

                                                        setHasChanged(true);
                                                    }}
                                                />
                                            </div>
                                        );
                                    },
                                )}
                            </Flex>
                        )
                    )}
                </>
            )}
        </CardWithDeviceLayout>
    );
};

export default ExperimentsPage;
