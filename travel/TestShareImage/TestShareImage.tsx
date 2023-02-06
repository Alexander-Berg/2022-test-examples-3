import {FC} from 'react';

import cx from './TestShareImage.scss';

export const TestShareImage: FC = () => {
    return (
        <div className={cx('root')}>
            <h1>Test hotel share image title</h1>
            <p>Test hotel share image text. Test hotel share image text!</p>
        </div>
    );
};
