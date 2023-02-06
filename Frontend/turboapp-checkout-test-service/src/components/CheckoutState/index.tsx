import React from 'react';

import { ResizableTextarea } from '../Textarea';

type Props = { checkoutState?: object };

const CheckoutState: React.FC<Props> = ({ checkoutState }) => {
    const json = checkoutState ? JSON.stringify(checkoutState, null, 4) : '<none>';

    return <ResizableTextarea view="default" size="m" value={json} />;
};

export default CheckoutState;
