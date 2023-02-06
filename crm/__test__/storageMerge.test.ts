import storageMerge from '../storageMerge';

describe('storage merge', () => {
  test('merge without nodes', () => {
    const left = {
      issues: {
        252536: {
          id: 252536,
          data: {},
          scheme: {},
          props: {},
        },
      },
    };

    const right = {
      issues: {
        252537: {
          id: 252537,
          data: {},
          scheme: {},
          props: {},
        },
      },
    };

    const result = {
      issues: {
        252536: {
          id: 252536,
          data: {},
          scheme: {},
          props: {},
        },
        252537: {
          id: 252537,
          data: {},
          scheme: {},
          props: {},
        },
      },
    };

    expect(storageMerge(left, right)).toEqual(result);
  });

  test('merge with nodes', () => {
    const left = {
      issues: {
        252536: {
          id: 252536,
          data: {},
          scheme: {},
          props: {},
        },
      },
      nodes: {
        accounts: {
          '100': {
            issues: {
              map: {
                '0': {
                  counter: 0,
                  eof: true,
                  items: [1],
                  id: 0,
                },
              },
            },
          },
        },
        issues: {
          '100': {
            issues: {
              map: {},
            },
          },
        },
        issueList: {
          map: {},
        },
      },
    };

    const right = {
      issues: {
        252537: {
          id: 252537,
          data: {},
          scheme: {},
          props: {},
        },
      },
      nodes: {
        accounts: {
          '100': {
            issues: {
              map: {
                '1': {
                  counter: 0,
                  eof: true,
                  items: [1],
                  id: 0,
                },
              },
            },
          },
          '101': {
            issues: {
              map: {
                '0': {
                  counter: 0,
                  eof: true,
                  items: [1],
                  id: 0,
                },
              },
            },
          },
        },
        issues: {
          '100': {
            issues: {
              map: {
                '0': {
                  counter: 0,
                  eof: true,
                  items: [1],
                  id: 0,
                },
              },
            },
          },
        },
        issueList: {
          map: {},
        },
      },
    };

    const result = {
      issues: {
        252536: {
          id: 252536,
          data: {},
          scheme: {},
          props: {},
        },
        252537: {
          id: 252537,
          data: {},
          scheme: {},
          props: {},
        },
      },
      nodes: {
        accounts: {
          '100': {
            issues: {
              map: {
                '0': {
                  counter: 0,
                  eof: true,
                  items: [1],
                  id: 0,
                },
                '1': {
                  counter: 0,
                  eof: true,
                  items: [1],
                  id: 0,
                },
              },
            },
          },
          '101': {
            issues: {
              map: {
                '0': {
                  counter: 0,
                  eof: true,
                  items: [1],
                  id: 0,
                },
              },
            },
          },
        },
        issues: {
          '100': {
            issues: {
              map: {
                '0': {
                  counter: 0,
                  eof: true,
                  items: [1],
                  id: 0,
                },
              },
            },
          },
        },
        issueList: {
          map: {},
        },
      },
    };

    expect(storageMerge(left, right)).toEqual(result);
  });

  test('replace issue', () => {
    const left = {
      issues: {
        252536: {
          id: 252536,
          data: {
            isDone: true,
          },
          scheme: {},
          props: {},
        },
      },
    };

    const right = {
      issues: {
        252536: {
          id: 252536,
          data: {
            isDone: true,
          },
          scheme: {},
          props: {},
        },
      },
    };

    const result = right;

    expect(storageMerge(left, right)).toEqual(result);
  });

  test('dont erase nodes by context', () => {
    const left = {
      nodes: {
        issueList: {
          map: {
            '0': {},
          },
        },
      },
    };

    const right = {
      nodes: {
        issueList: {
          map: {},
        },
      },
    };

    const result = left;

    expect(storageMerge(left, right)).toEqual(result);
  });
});
