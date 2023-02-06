export default function getOptionDescription(code: string): string {
    if (code.includes('SUCCESS')) {
        return 'Успешно';
    }

    if (code.includes('FAILURE')) {
        return 'Провал';
    }

    return 'Не указано';
}
