export default function parseExpConfigString(
    expConfigString: string | boolean,
):
    | {disabled: false; expKey: string; expValue: string}
    | {disabled: true; value: boolean} {
    if (typeof expConfigString === 'boolean') {
        return {disabled: true, value: expConfigString};
    }

    const [expKey, expValue] = expConfigString.split(':');

    return {disabled: false, expKey, expValue};
}
