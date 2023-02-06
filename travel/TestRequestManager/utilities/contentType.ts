import {parse} from 'content-type';

export function parseContentType(
    headers: Record<string, string>,
): string | null {
    if (!('content-type' in headers)) {
        return null;
    }

    return parse({headers}).type;
}
