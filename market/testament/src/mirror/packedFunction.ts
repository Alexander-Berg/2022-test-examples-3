export type SerializedFunction<TArgs extends readonly any[]> = {
    code: string;
    args: TArgs;
};

export type TFn<TArgs extends readonly any[], TReturn> = (
    this: null,
    ...args: TArgs
) => TReturn;

export default class PackedFunction<TArgs extends readonly any[], TReturn> {
    static deserialize<TSArgs extends readonly any[], TSReturn>(
        from: SerializedFunction<TSArgs>,
    ): PackedFunction<TSArgs, TSReturn> {
        return new PackedFunction<TSArgs, TSReturn>(
            // eslint-disable-next-line no-new-func
            new Function(`return (${from.code.toString()})(...arguments)`) as (
                ...args: TSArgs
            ) => TSReturn,
            from.args,
        );
    }

    protected args: [...TArgs];

    constructor(protected fn: TFn<TArgs, TReturn>, args: TArgs) {
        this.args = [...args];
    }

    getFn(): TFn<TArgs, TReturn> {
        return this.fn;
    }

    getArgs(): TArgs {
        return this.args;
    }

    serialize(): SerializedFunction<TArgs> {
        return {
            code: this.fn.toString(),
            args: this.args,
        };
    }

    toString(): string {
        return `(...args) => (${this.getFn()})(...${JSON.stringify(
            this.getArgs(),
        )}, ...args)`;
    }
}

export function packFunction<TArgs extends readonly any[], TReturn>(
    fn: TFn<TArgs, TReturn>,
    args: TArgs,
): PackedFunction<TArgs, TReturn> {
    return new PackedFunction(fn, args);
}
