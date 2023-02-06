import React from 'react';
import { Button } from '../../../Button';

import { Icon } from '../../../Icon';

type Props = {
    onClick: () => void;
    className?: string;
};

function CrossIcon(className: string) {
    return <Icon glyph="type-cross" size="m" className={className} />;
}

export const DeleteButton: React.FC<Props> = ({ onClick, className }) => {
    return <Button view="default" size="m" onClick={onClick} icon={CrossIcon} className={className} />;
};
