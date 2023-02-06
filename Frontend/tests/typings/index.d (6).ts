declare function specs(description: string | ISpecsDescription, fn: Function): void;
declare function it(expectation: string, callback?: ItCallback): Hermione.Test;

declare interface ISpecsDescription {
    category?: string,
    feature?: string,
    type?: string,
    experiment?: string,
}

declare type ItCallback = (this: WebdriverIO.ItDefinitionCallbackCtx, done: Hermione.TestDone) => any;

declare interface TestDone {
    (error?: any): any;
}
