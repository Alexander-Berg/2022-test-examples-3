export const getLastSpiedModuleCall = (module, methodName) => {
    if (!module[methodName].mock) {
        throw new Error(`Module ${methodName} was not spied!`);
    }

    const {calls} = module[methodName].mock;

    return calls[calls.length - 1];
};
