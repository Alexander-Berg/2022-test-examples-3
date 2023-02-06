import { DispatcherApi } from 'lib/dispatch/Dispatch';

export type Mock<T> = {
    -readonly [K in keyof T]: T[K];
}

export function mock<T>(obj: T): Mock<T> {
    return obj;
}

export const mockDispatch = (): DispatcherApi => ({
    dispatch: jest.fn(),
});
