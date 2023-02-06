require('models/operation/operation-get-contacts');

const helperOperation = require('helpers/operation');

describe('Получения списка контактов пользователя', () => {
    beforeEach(function() {
        const operation = this.operation = helperOperation.initialize('getContacts', {});

        const history = this.historyStatus = [];

        this.operation.on('ns-model-changed.status', () => {
            history.push(operation.get('.status'));
        });
    });

    afterEach(function() {
        delete this.operation;
    });

    describe('при успешном завершении', () => {
        beforeEach(() => {
            addResponseModel([
                {
                    oid: 1,
                    type: 'contacts'
                },
                {
                    status: 'WAITING',
                    type: 'contacts'
                },
                {
                    status: 'EXECUTING',
                    type: 'contacts'
                },
                {
                    status: 'DONE',
                    type: 'contacts',
                    stages: [
                        {
                            status: 'success',
                            service: 'email',
                            details: [
                                {
                                    user: {
                                        name: 'Alfa',
                                        userid: 'alfa@domain.com'
                                    }
                                },
                                {
                                    user: {
                                        userid: 'bravo@domain.com'
                                    }
                                },
                                {
                                    user: {
                                        name: 'Charlie',
                                        userid: 'charlie@domain.com'
                                    }
                                }
                            ]
                        },
                        {
                            status: 'success',
                            service: 'twitter',
                            details: [
                                {
                                    user: {
                                        userid: 'delta@domain.com'
                                    }
                                },
                                {
                                    user: {
                                        userid: 'echo@domain.com'
                                    }
                                }
                            ]
                        },
                        {
                            status: 'failure',
                            service: 'facebook',
                            error: 'no tokens found for profile id XXXXX'
                        }
                    ]
                }
            ]);
        });

        testOperation({
            desc: 'должна пройти определенные статусы',
            status: 'done',
            callback: function() {
                expect(this.historyStatus).to.be.eql([
                    'created',
                    'progressing',
                    'done'
                ]);
            }
        });

        testOperation({
            desc: 'должна отфильтровать пользователей с сервисом email, и поместить полученые контакты по пути `.contacts`',
            status: 'done',
            callback: function() {
                expect(this.operation.get('.contacts')).to.have.length(3);
            }
        });
    });

    describe('при неудачном завершении', () => {
        beforeEach(() => {
            addResponseModel([
                {
                    oid: 1,
                    type: 'contacts'
                },
                {
                    error: 'Internal server error',
                    code: 500
                }
            ]);
        });

        testOperation({
            desc: 'должна пройти определенные статусы',
            status: 'failed',
            statusTestDone: 'failed',
            callback: function() {
                expect(this.historyStatus).to.be.eql([
                    'created',
                    'progressing',
                    'failed'
                ]);
            }
        });
    });

    describe('при отсутствии идентификатора операции', () => {
        beforeEach(() => {
            addResponseModel([
                {
                    error: 'Internal server error',
                    code: 500
                }
            ]);
        });

        testOperation({
            desc: 'должна моментально завершиться с ошибкой',
            status: 'failed',
            statusTestDone: 'failed',
            callback: function() {
                expect(this.historyStatus).to.be.eql([
                    'created',
                    'failed'
                ]);
            }
        });
    });

    describe('при порционной выдаче данных', () => {
        beforeEach(() => {
            addResponseModel([
                {
                    oid: 1,
                    type: 'contacts'
                },
                {
                    status: 'WAITING',
                    type: 'contacts'
                },
                {
                    status: 'EXECUTING',
                    type: 'contacts',
                    stages: [
                        {
                            status: 'success',
                            service: 'email',
                            details: [
                                {
                                    user: {
                                        name: 'Alfa',
                                        userid: 'alfa@domain.com'
                                    }
                                }
                            ]
                        }
                    ]
                },
                {
                    status: 'DONE',
                    type: 'contacts',
                    stages: [
                        {
                            status: 'success',
                            service: 'email',
                            details: [
                                {
                                    user: {
                                        name: 'bravo',
                                        userid: 'bravo@domain.com'
                                    }
                                }
                            ]
                        }
                    ]
                }
            ]);
        });

        testOperation({
            desc: 'должна не потерять данные из первой порции',
            status: 'done',
            callback: function() {
                expect(this.operation.get('.contacts')).to.have.length(2);
            }
        });
    });

    describe('при выдаче неожиданного статуса операции', () => {
        beforeEach(() => {
            addResponseModel([
                {
                    oid: 1,
                    type: 'contacts'
                },
                {
                    status: 'UNKNOWN',
                    type: 'contacts'
                }
            ]);
        });

        testOperation({
            desc: 'должна моментально завершиться с ошибкой',
            status: 'failed',
            statusTestDone: 'failed',
            callback: function() {
                expect(this.historyStatus).to.be.eql([
                    'created',
                    'progressing',
                    'failed'
                ]);
            }
        });
    });
});
