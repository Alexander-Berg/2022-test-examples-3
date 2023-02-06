import Cookie from 'js-cookie';

export function isE2ETesting(): boolean {
    return Cookie.get('e2e_test') === 'true';
}
