let test = require('tape');
let Clck = require('..');

test('decrypted', function(t) {
    let defaultOpts = {
        key: 'jQm9NqfREKGKRNgHSGLV1A==',
    };
    let str = '37093,0,72;36604,0,2;32050,0,58;36569,0,63;36596,0,35;25469,0,31;38395,0,57;37951,0,82;38277,0,82;38148,0,93;37876,0,6;35870,0,50;38107,0,2;38236,0,91;37714,0,59;38444,0,55;38436,0,76;38383,0,8;35315,0,39;23989,0,14;38471,0,71;38520,0,73;36412,0,47;10326,0,63;36574,0,15';
    let res = 'P8Xe6bY5djryvAiv2097ti_p1IpN_JTiA77Z5YoRFIXFlRitPwBq86DXaI91RnWHu-tIBG3bpSudMFXKUpfXfWrKSgeHYt2O1JmK0d3dXZeslby2vWhAWOle-SEUymRnxjTEvYjENmO1Ol0mfFFCf_mLZdjlJnrz9WN4BHJUzUsuXdFsuNisqknjh_9huuAJWhrbiEcVJP98jOK90jh9YHp1TAm6bVR7FUhuywI0wMpuxzlVsHwHIKKYaNdTaYYUNKjqkSKY1UscnZoZRuG6NvKeR-LnCrAjFurXnPDfjZRLv9qllOjtrhWjsxF1Uqg7TMyGO9WKAAxhsdNSYvG1h1OuE0CbRT7WCi_KHBLSrjA,';

    let clck = new Clck(defaultOpts);

    t.equal(clck.decrypt(res), str, 'Should be equal');

    let opts = Object.assign({}, defaultOpts, { key: Buffer.from(defaultOpts.key, 'base64') });
    let clckWithKeyBuffer = new Clck(opts);

    t.equal(clckWithKeyBuffer.decrypt(res), str, 'Should be equal');

    t.end();
});
