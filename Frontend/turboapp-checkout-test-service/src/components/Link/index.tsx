import { compose } from '@bem-react/core';
import { Link as LegoLink, withThemePseudo } from '@yandex-lego/components/Link/desktop';

export const Link = compose(withThemePseudo)(LegoLink);

export type LinkProps = Parameters<typeof Link>[0];
