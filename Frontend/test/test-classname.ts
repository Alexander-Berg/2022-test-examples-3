import { ClassNameFormatter, ClassNameInitilizer } from '@bem-react/classname';
import * as qs from 'query-string';

import { classname } from 'utils/classname';

let isEnabled: boolean = false;

export const testClassname: ClassNameInitilizer = (blockName, ...params): ClassNameFormatter => {
    const cnInstance = classname(`t-${blockName}`, ...params);

    return ((...innerParams) => {
        if (!isEnabled) return '';

        return cnInstance(...innerParams);
    }) as ClassNameFormatter;
};

// Включает testCn, если находимся в режиме разработки или есть специальный параметр в URL'e
export const setTestClassnameStatus = (search: string) => {
    const searchParams = qs.parse(search);

    isEnabled = Boolean(searchParams['enable-test-cn']);
};
