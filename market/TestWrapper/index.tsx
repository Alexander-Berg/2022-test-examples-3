import React from 'react';

import styles from './styles.module.css';

interface Props {
    /** имя атрибута после data-ow-test */
    label: string;
    /** значение атрибута */
    value: string;
    /**
     * контент нужно заворачивать в дополнительный спан (требование Дениса (ekbstudent@))
     * данный флаг для простых контентов, которые не заворачивают потомков в спаны в своих недрах
     * */
    extraSpan?: boolean;
    hidden?: boolean;
}

/**
 * Компонент-обертка для удобного поиска тестировщиками.
 * Оборачивает элемент в настоящую dom-ноду с display: contents и data-ow-test* атрибутом.
 * Решение более гораздо более простое, контролируемое и дешевое, чем пытаться найти
 * dom-элемент в react-элементе (его зачастую может не быть).
 * Data-атрибуты начинаются с data-ow-test-, дабы избежать коллизий с другими библиотеками,
 * любящими навешивать data-test, data-tid, etc
 */
const TestWrapper: React.FC<Props> = props => {
    const {children, label, value, extraSpan, hidden} = props;
    const spanProps = {
        [`data-ow-test-${label}`]: value,
        'data-ow-test-hidden': hidden,
    };

    return (
        <span {...spanProps} className={styles.displayContents}>
            {extraSpan ? <span className={styles.displayContents}>{children}</span> : children}
        </span>
    );
};

export default TestWrapper;
