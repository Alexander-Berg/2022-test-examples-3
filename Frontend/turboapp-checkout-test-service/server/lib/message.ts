export const createMessage = (data: object) => JSON.stringify(data);

export const parseMessage = (message: string) => JSON.parse(message);
