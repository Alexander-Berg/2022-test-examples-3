class NodeWrapper<T extends HTMLElement> {
    private node: T | null;

    constructor(node: T | null) {
        this.node = node;
    }

    public setProp<K extends keyof T, V extends T[K]>(key: K, value: V): this {
        if (this.node) {
            Object.defineProperty(this.node, key, {
                configurable: true,
                value,
            });
        }

        return this;
    }

    public dispatchEvent(event: Event): this {
        if (this.node) {
            this.node.dispatchEvent(event);
        }

        return this;
    }
}

export function createMockRef<T>(data: Partial<T>): React.RefObject<T> {
    let ref: T | null;

    return {
        set current(value: T | null) {
            if (value) {
                for (const key in data) {
                    if (data.hasOwnProperty(key)) {
                        Object.defineProperty(value, key, {
                            configurable: true,
                            value: data[key],
                        });
                    }
                }
            }

            ref = value;
        },

        get current() {
            return ref;
        },
    };
}

export function refNode(node: HTMLElement | null) {
    return new NodeWrapper(node);
}
