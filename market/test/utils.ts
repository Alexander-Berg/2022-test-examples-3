/* eslint-disable no-console */
export const mockConfirm = (value: boolean) => {
  return (message?: string) => {
    console.log(message);
    return value;
  };
};
