declare function step<T>(name: string, body: () => T): Promise<T> | T;
