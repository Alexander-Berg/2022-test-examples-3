type TNames = {
  [key: string]: string
}

export const generateTestNames = <T extends TNames>(itemNames: T) => (testName?: string): T => {
  const testNames = {} as T;

  if (testName) {
    for (const key in itemNames) {
      testNames[key] = `${testName}${itemNames[key]}` as T[typeof key];
    }
  }

  return testNames;
};

export const convertToSelectors = <T extends TNames>(testNames: T): T => {
  const selectors = {} as T;

  for (const key in testNames) {
    selectors[key] = `[data-test-name~=${testNames[key]}]` as T[typeof key];
  }

  return selectors;
};
