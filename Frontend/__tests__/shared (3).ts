export const changeUrl = (url: string) => {
  global.window = Object.create(window);
  Object.defineProperty(window, 'location', {
    value: {
      search: url,
    },
    writable: true,
  });
};
