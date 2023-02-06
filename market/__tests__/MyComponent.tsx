import React, {ReactNode} from 'react';

interface GenericsExampleProps<T> {
    children?: (item: T) => ReactNode;
    items?: Array<T>;
}

export default function MyComponent<T>({
    items = [],
}: GenericsExampleProps<T>): JSX.Element {
    return (
        <div role="root" className="icon-star">
            <div role="unique" className="unique" />
            {items.join('.')}
        </div>
    );
}
