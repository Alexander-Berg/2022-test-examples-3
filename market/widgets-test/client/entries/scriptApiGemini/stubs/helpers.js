export const createEntityLongName = (n, name, delimiter) => {
    const parts = Array(n).fill(name);

    return parts.join(n % 2 ? ' ' : delimiter);
};

export const createWarnings = ({warningCode, warningText}) => {
    if (warningCode) {
        return [
            {
                code: warningCode,
                message: warningText,
            },
        ];
    }

    return [];
};
