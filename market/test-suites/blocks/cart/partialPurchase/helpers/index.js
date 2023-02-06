export const getTotalItemsCount = (carts, ignoreCartIndex = -1) =>
    carts.reduce((cartAcc, cart, cartIndex) => {
        if (cartIndex === ignoreCartIndex) {
            return cartAcc;
        }
        return cartAcc + cart.items.reduce((itemAcc, item) =>
            itemAcc + item.count,
        0);
    }, 0);

export const getTotalWeight = (carts, ignoreCartIndex = -1) =>
    carts.reduce((cartAcc, cart, cartIndex) => {
        if (cartIndex === ignoreCartIndex) {
            return cartAcc;
        }
        return cartAcc + cart.items.reduce((itemAcc, item) =>
            itemAcc + (Number(item.offerMock.weight) * 1000 * item.count),
        0);
    }, 0);

export const getTotalPrice = (carts, ignoreCartIndex = -1) =>
    carts.reduce((cartAcc, cart, cartIndex) => {
        if (cartIndex === ignoreCartIndex) {
            return cartAcc;
        }
        return cartAcc + cart.items.reduce((itemAcc, item) =>
            itemAcc + Number(item.offerMock.prices.value),
        0);
    }, 0);
