import { compose } from '@bem-react/core';
import { Modal as ModalDesktop, withThemeNormal } from '@yandex-lego/components/Modal/desktop';
import { withZIndex } from '@yandex-lego/components/withZIndex';

export const Modal = compose(withThemeNormal, withZIndex)(ModalDesktop);
export type ModalProps = Parameters<typeof Modal>[0];
