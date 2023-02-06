import {decl} from '../../../../../i-bem/i-bem.react';

export default decl({
    block: 'gemini',
    elem: 'item',
    attrs({attrs}) {
        return attrs;
    },
    cls({data}) {
        return data && Object.keys(data)
            .map(key => `${key}_${data[key]}`)
            .join(' ');
    }
});
