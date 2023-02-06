class Foo {
    foo = '';

    get() {
        return this.foo;
    }

    set(value: string) {
        this.foo = value;
    }
}

export default new Foo();
