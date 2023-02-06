/* global describe, it, before, after */
var fs = require('fs');
var path = require('path');
var assert = require('assert');
var sinon = require('sinon');
var streams = require('stream');
var extension = require('../lib/extension');
var TransformStream = require('../lib/transform').TransformStream;

describe('extension', function() {
    var fixedDate = new Date();
    /* jshint unused:false */
    var _Date = Date;
    var _config = extension.config;
    var _streams = extension.streams;
    before(function() {
        /* jshint ignore:start */
        Date = function() {
            return fixedDate;
        };
        Date.now = _Date.now;
        /* jshint ignore:end */
        extension.config = {
            resolve: function() {return '/dev/null';},
            data: {},
            get: function(key) {return this.data[key];}
        };
        extension.streams = {};
    });

    after(function() {
        /* jshint ignore:start */
        Date = _Date;
        /* jshint ignore:end */
        extension.config = _config;
        extension.streams = _streams;
    });

    describe('extension#setupStream', function() {
        it('should setup three streams', function() {
            var proc = {
                pid: 1,
                wid: 1,
                process: {
                    stdout: fs.createReadStream(path.resolve(__dirname, 'data/input.log'), {encoding: 'utf8'})
                }
            };
            var streamSet = extension.setupStream(proc, 'stdout');
            assert.equal(streamSet.length, 3);
        });

        it('should setup three streams', function(done) {
            var proc = {
                pid: 1,
                wid: 1,
                process: {}
            };

            proc.process.stdout = fs.createReadStream(path.resolve(__dirname, 'data/input.log'), {encoding: 'utf8'});
            var streamSet = extension.setupStream(proc, 'stdout');
            var transformStream = streamSet[1];

            var result = '';
            transformStream.on('data', function(chunk) {
                result += chunk;
            });

            transformStream.on('end', function() {
                var sDate = String(fixedDate);
                var expected = [
                    sDate + ' [worker:id=1,pid=1] msg one',
                    sDate + ' [worker:id=1,pid=1] msg two',
                    sDate + ' [worker:id=1,pid=1] msg three',
                    ''
                ].join('\n');

                assert.equal(expected, result);
                done();
            });
        });

        it('should reset streams', function(done) {
            var stream = new streams.Readable();
            stream._read = function(){};

            var proc = {
                pid: 1,
                wid: 1,
                process: {
                    stdout: stream
                }
            };

            var streamSet = extension.setupStream(proc, 'stdout');

            var resultBuf = [];
            var endSpy = sinon.spy();
            streamSet[1].on('data', function(data) {
                resultBuf.push(data);
            });

            streamSet[1].once('end', endSpy);

            stream.push('foo');
            stream.push('bar');

            var newStreamSet = extension.setupStream(proc, 'stdout');
            newStreamSet[1].on('data', function(data) {
                resultBuf.push(data);
            });

            newStreamSet[1].once('end', function() {
                var result = resultBuf.map(String);
                var expected = [
                    fixedDate + ' [worker:id=1,pid=1] foo\n',
                    fixedDate + ' [worker:id=1,pid=1] bar\n',
                    fixedDate + ' [worker:id=1,pid=1] beep\n',
                    fixedDate + ' [worker:id=1,pid=1] boop\n'
                ];
                assert.equal(result.join(''), expected.join(''));
                assert.equal(endSpy.calledOnce, true);
                done();
            });

            stream.push('beep');
            stream.push('boop');
            stream.push(null);
        });
    });

    describe('extension#isStreamUsable', function() {
        it('should return false if stream is not described in config', function() {
            var _config = extension.config;
            extension.config = {stdout: 'foo'};
            assert.equal(extension.isStreamUsable('stderr'), false);
            assert.equal(extension.isStreamUsable('stdout'), true);
            extension.config = _config;
        });
    });

    describe('extension#setupStreams', function() {
        it('should not setup unusable streams', function() {
            var _config = extension.config;
            extension.config = {};
            assert.equal(extension.setupStreams(null).length, 0);
            extension.config = _config;
        });

        it('should not setup unusable streams, but setup usable', function() {
            var _out = extension.config.stdout;
            extension.config.stdout = '/dev/null';

            assert.equal(extension.setupStreams({process: process}).length, 1);

            extension.config.stdout = _out;
        });
    });

    describe('extension#createTransformStream', function() {
        it('should return new instance of TransformStream', function() {
            assert.equal(extension.createTransformStream({}) instanceof TransformStream, true);
        });
    });

    describe('extension#setupSignalHandler', function() {
        it('should attach working SIG handler', function(done) {
            var _signalHandler = extension.signalHandler;
            var spy = sinon.spy(function(signal) {
                assert(spy.calledOnce);
                assert.equal(signal, extension.RELOAD_SIGNAL);
                done();
            });

            extension.signalHandler = spy;
            extension.setupSignalHandler({process: process});
            extension.signalHandler = _signalHandler;

            process.kill(process.pid, extension.RELOAD_SIGNAL);
        });
    });
});
