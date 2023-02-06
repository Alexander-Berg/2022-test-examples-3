/* global describe, it, before, after */
var assert = require('assert');
var streams = require('stream');
var t = require('../lib/transform');
var TransformStream = t.TransformStream;

describe('transform', function() {
    var fixedDate = new Date();
    /* jshint unused:false */
    var _Date = Date;
    before(function() {
        /* jshint ignore:start */
        Date = function() {
            return fixedDate;
        };
        Date.now = _Date.now;
        /* jshint ignore:end */
    });

    after(function() {
        /* jshint ignore:start */
        Date = _Date;
        /* jshint ignore:end */
    });

    describe('transform#timestamp', function() {
        it('should append timestamp at the start of a line', function() {
            var proc = {isMaster: true, pid: 1};
            var timestamped = t.timestamp(proc, 'foo');
            assert.equal(timestamped, fixedDate + ' ' + t.label(proc, 'foo'));
        });
    });

    describe('transform#label', function() {
        it('should return labeled with master or worker info line', function(){
            var proc = {isMaster: true, pid: 1};
            assert.equal(t.label(proc, 'foo'), '[master:pid=1] foo');
        });
        it('should return labeled with worker info line if proc is not master', function(){
            var proc = {pid: 1, wid: 1};
            assert.equal(t.label(proc, 'foo'), '[worker:id=1,pid=1] foo');
        });
    });

    describe('transform#passthrough', function() {
        it('should return unchanged line', function() {
            assert.equal(t.passthrough({}, 'foo'), 'foo');
        });
    });

    describe('transform#TransformStream', function() {
        it('should prefix each line of input with `prefix`', function(done) {
            var transformFn = function(proc, line) {return 'prefix ' + line;};
            var stream = new streams.Readable();

            stream._read = function(){};

            var tss = stream.pipe(new TransformStream(null, transformFn));

            var result = '';
            tss.on('data', function(chunk) {
                result += chunk;
            });

            stream.push('msg one');
            stream.push('msg two');
            stream.push('msg three');
            tss.once('end', function() {
                var expected = [
                    'prefix msg one',
                    'prefix msg two',
                    'prefix msg three',
                    ''
                ].join('\n');
                assert.equal(expected, result);
                done();
            });
            stream.push(null);
        });
    });
});
