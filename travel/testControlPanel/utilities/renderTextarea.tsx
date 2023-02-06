import React from 'react';

import {TControlRenderFunc} from 'components/FormField/components/Field/Field';
import TextArea from 'components/TextArea/TextArea';
import FieldLabel from 'components/FieldLabel/FieldLabel';
import Flex from 'components/Flex/Flex';
import Text from 'components/Text/Text';
import LinkButton from 'components/LinkButton/LinkButton';
import Link from 'components/Link/Link';
import InfoIcon from 'icons/16/Info';
import TextWithIcon from 'components/TextWithIcon/TextWithIcon';

export const renderTextarea: TControlRenderFunc = ({
    input,
    label,
    meta,
    helpLink,
    onFillButtonClick,
}): React.ReactNode => {
    return (
        <FieldLabel label={label}>
            <Flex flexDirection="column" between={1}>
                <TextArea
                    name={input.name}
                    size="m"
                    hint={meta.error}
                    state={meta.error ? 'error' : undefined}
                    value={input.value}
                    onChange={input.onChange}
                    withAutoResize
                    hasClear
                />
                <Flex
                    inline
                    alignItems="baseline"
                    justifyContent="space-between"
                >
                    {onFillButtonClick && (
                        <Text size="s">
                            <LinkButton onClick={onFillButtonClick}>
                                Заполнить пример
                            </LinkButton>
                        </Text>
                    )}
                    {helpLink && (
                        <Link target="_blank" url={helpLink}>
                            <TextWithIcon
                                text="Помощь"
                                iconLeft={InfoIcon}
                                size="s"
                            />
                        </Link>
                    )}
                </Flex>
            </Flex>
        </FieldLabel>
    );
};
