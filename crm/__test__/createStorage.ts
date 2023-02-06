export type StorageInitialValues = Record<string, string>;

export const createStorage = (initialValues?: StorageInitialValues): Storage => {
  return Object.defineProperties(
    { ...initialValues },
    {
      length: {
        get: function() {
          return Object.keys(this).length;
        },
      },
      clear: {
        value: function() {
          for (let key of this) {
            delete this[key];
          }
        },
      },
      getItem: {
        value: function(key: string) {
          return this[key] ?? null;
        },
      },
      setItem: {
        value: function(key: string, value: string) {
          this[key] = value;
        },
      },
      removeItem: {
        value: function(key: string) {
          delete this[key];
        },
      },
    },
  );
};
