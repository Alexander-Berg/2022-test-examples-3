export default function validateJSON(str: string): string {
    if (!str) {
        return '';
    }

    try {
        JSON.parse(str);

        return '';
    } catch (e) {
        return 'Невалидный JSON';
    }
}
