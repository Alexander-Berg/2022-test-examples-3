export const response403 = (roles: string[]): Response =>
  ({
    status: 403,
    json(): Promise<any> {
      return Promise.resolve({ result: roles });
    },
    statusText: 'Forbidden',
  } as any);
