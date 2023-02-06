'use strict';

class Ticket {
    create() {
        const id = 42;

        return Promise.resolve(id);
    }

    update() {
        return Promise.resolve();
    }

    postStatusComment() {
        return Promise.resolve();
    }

    postCommentByTemplate() {
        return Promise.resolve();
    }

    getIssue() {
        return {
            getId() {
                return 'TEST';
            },
            getAssignee() {
                return {
                    getId() {
                        return 'someone';
                    },
                };
            },
        };
    }
}

module.exports = Ticket;
