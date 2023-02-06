import {memo, FC} from 'react';
import {NavLink as CommonNavLink, NavLinkProps} from 'react-router-dom';

import cx from './NavLink.scss';

interface INavLinkProps extends NavLinkProps {}

const NavLink: FC<INavLinkProps> = props => {
    const {className, activeClassName, ...restProps} = props;

    return (
        <CommonNavLink
            className={cx('link', className)}
            activeClassName={cx('link_active', activeClassName)}
            {...restProps}
        />
    );
};

export default memo(NavLink);
