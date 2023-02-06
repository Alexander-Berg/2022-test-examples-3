module.exports = {
    queries: [
        {
            query: {},
            systems: [
                {
                    id: '1',
                    comments: [
                        {
                            isWinner: true,
                            'task-id': 'task-id-1.1',
                            text: 'Комментарий 1.1.',
                            'worker-id': 'worker-id-1.1',
                        },
                        {
                            isWinner: true,
                            'task-id': 'task-id-1.2',
                            text: 'Комментарий 1.2.',
                            'worker-id': 'worker-id-1.2',
                        },
                    ],
                },
                {
                    id: '2',
                    comments: [
                        {
                            isWinner: false,
                            'task-id': 'task-id-2.1',
                            text: 'Комментарий 2.1.',
                            'worker-id': 'worker-id-2.1',
                        },
                    ],
                },
            ],
        },

        {
            query: {},
            systems: [
                {
                    id: '3',
                    comments: [
                        {
                            isWinner: true,
                            'task-id': 'task-id-3.1',
                            text: 'Комментарий 3.1.',
                            'worker-id': 'worker-id-3.1',
                        },
                    ],
                },
                {
                    id: '4',
                    comments: [
                        {
                            isWinner: false,
                            'task-id': 'task-id-4.1',
                            text: 'Комментарий 4.1.',
                            'worker-id': 'worker-id-2.1',
                        },
                        {
                            isWinner: false,
                            'task-id': 'task-id-4.2',
                            text: 'Комментарий 4.2.',
                            'worker-id': 'worker-id-4.2',
                        },
                        {
                            isWinner: false,
                            'task-id': 'task-id-4.3',
                            text: 'Комментарий 4.3.',
                            'worker-id': 'worker-id-4.3',
                        },
                    ],
                },
            ],
        },
    ],
};
