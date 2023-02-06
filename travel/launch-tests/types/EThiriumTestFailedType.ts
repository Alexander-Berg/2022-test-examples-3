enum EThiriumTestFailedType {
    ERROR = 'ERROR',
    IGNORE = 'IGNORE',
}

export default EThiriumTestFailedType;

export function isThiriumTestFailedType(
    candidate: unknown,
): candidate is EThiriumTestFailedType {
    return Object.values(EThiriumTestFailedType).some(v => v === candidate);
}
