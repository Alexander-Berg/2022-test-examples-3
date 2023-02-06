// eslint-disable-next-line @typescript-eslint/triple-slash-reference,spaced-comment
/// <reference path="./cats.d.ts" />
// eslint-disable-next-line @typescript-eslint/triple-slash-reference,spaced-comment
/// <reference path="./shot.d.ts" />

declare namespace NodeJS {
    interface Global {
        reporter: {
            addLabel(name: string, value: string): NodeJS.Global;
            addParameter(name: string, value: string): NodeJS.Global;
            testId(testId: string): NodeJS.Global;
            addAttachment(name: string, buffer: Buffer, type: string): void;
            createAttachment(title: string, data: string | Buffer | (() => any), mimeType?: string): void;
            startStep(name: string, ts?: number): void;
            endStep(status?: string, ts?: number): void;
            runStep<T>(name: string, fn: () => T): T;
        };

        // cats namespace
        cat: CatNamespace.Cat;
        // screenshot namespace
        shot: ShotNamespace.Shot;
    }
}
