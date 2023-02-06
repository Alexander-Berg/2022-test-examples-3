/* global describe, it, beforeEach */

var assert = require('chai').assert,
    sinon = require('sinon'),
    Vow = require('vow');

describe('Solomon', function() {
    var Solomon = require('../app/lib/solomon'),
        solomonConfig = {
            backend: {
                host: 'someHost',
                port: 'somePort',
                path: 'somePath'
            },
            commonLabels: {
                project: 'someProject',
                service: 'someService',
                host: 'someHost'
            },
            wrapDebugTimers: true,
            timeBuckets: [
                20,
                100
            ]
        };

    describe('method', function() {
        var solomonInstance;

        beforeEach(function() {
            solomonInstance = new Solomon(solomonConfig);
        });

        describe('.saveSensor', function() {
            it('should add sensor to sensor storage', function() {
                var sensor = {
                    name: '5xx',
                    value: 1
                };

                solomonInstance.saveSensor(sensor);

                assert.deepEqual(solomonInstance._sensors, [ sensor ]);
            });

            it('should put sensor to timeBucket if sensor.useTimeBucket is set', function() {
                var putSensorToTimeBucket = sinon.stub(solomonInstance, 'putSensorToTimeBucket');
                var sensor = {
                    name: 'get_page_data',
                    value: 19,
                    useTimeBucket: true
                };

                solomonInstance.saveSensor(sensor);

                assert.isTrue(putSensorToTimeBucket.calledWith(sensor));
            });
        });

        describe('.putSensorToTimeBucket', function() {
            it('should put sensor to appropriate time bucket', function() {
                var sensor = {
                    name: 'get_page_data',
                    value: 19,
                    useTimeBucket: true
                };

                solomonInstance.putSensorToTimeBucket(sensor);

                assert.deepEqual(
                    solomonInstance._sensors,
                    [
                        { name: 'get_page_data20ms', value: 1 },
                        { name: 'get_page_data100ms', value: 0 }
                    ]
                );
            });
        });

        describe('.getExtendedSensors', function() {
            it('should return extended ._sensors array', function() {
                var sensor = {
                    name: '5xx',
                    value: 1
                };

                solomonInstance.saveSensor(sensor);

                var extendedSensors = solomonInstance.getExtendedSensors();

                assert.deepEqual(extendedSensors, [
                    {
                        labels: { sensor: sensor.name },
                        value: sensor.value,
                        mode: sensor.mode,
                        ts: sensor.ts
                    }
                ]);
            });
        });

        describe('.wrapDebugTimers', function() {
            var debug,
                debugFinishTimer,
                label,
                timerValue = 19;

            beforeEach(function() {
                label = 'Dispatcher';
                debug = {
                    finishTimer: sinon.stub(),
                    _timers: {}
                };
                debug._timers[label]  = timerValue;
                debugFinishTimer = debug.finishTimer;
            });

            it('should silently wrap debug.finishTimer function', function() {
                solomonInstance.wrapDebugTimers(debug);

                debug.finishTimer(label);

                assert.isTrue(debugFinishTimer.calledOn(debug));
                assert.isTrue(debugFinishTimer.calledWith(label));
            });

            it('should should add sensor with useTimeBucket option on debug.finishTimer', function() {
                var saveSensor = sinon.spy(solomonInstance, 'saveSensor');

                solomonInstance.wrapDebugTimers(debug);

                debug.finishTimer(label);

                assert.deepEqual(saveSensor.getCall(0).args[0], {
                    name: label,
                    useTimeBucket: true,
                    value: timerValue
                });

                solomonInstance.saveSensor.restore();
            });
        });

        describe('.sendSensors', function() {
            beforeEach(function() {
                solomonInstance._asker = sinon.stub().returns(Vow.resolve());
            });

            it('should extend sensors before sending them', function() {
                var getExtendedSensors = sinon.spy(solomonInstance, 'getExtendedSensors');

                solomonInstance.sendSensors();

                assert.strictEqual(getExtendedSensors.callCount, 1);

                solomonInstance.getExtendedSensors.restore();
            });

            it('should take backendOptions and commonLabels from config', function() {
                solomonInstance.sendSensors();

                var askerCallArgument = solomonInstance._asker.getCall(0).args[0];

                assert.equal(askerCallArgument.host, solomonConfig.backend.host);
                assert.equal(askerCallArgument.port, solomonConfig.backend.port);
                assert.equal(askerCallArgument.path, solomonConfig.backend.path);

                assert.deepEqual(askerCallArgument.body.commonLabels, solomonConfig.commonLabels);
            });

            it('should log error on request fail', function(done) {
                var AskerError = Solomon.Error;
                var error = { code: 'I am an error' };

                solomonInstance._asker = sinon.stub().returns(Vow.reject(error));

                assert.logsTerror(AskerError, AskerError.CODES.UNKNOWN_ERROR, function() {
                    var deferred = Vow.defer();

                    solomonInstance.sendSensors();

                    setTimeout(function() { deferred.resolve(); });

                    return deferred.promise();
                }).then(done);
            });
        });
    });

});
