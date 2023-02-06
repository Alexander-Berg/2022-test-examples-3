package ru.yandex.antifraud;

import java.nio.file.Files;

import org.junit.Test;

import ru.yandex.antifraud.data.TrafficFeaturesMaker;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class FeaturesTest extends TestBase {
    public FeaturesTest() {
        super(false, 0L);
    }
    @Test
    public void test() throws Exception {
        {
            final JsonObject features = TrafficFeaturesMaker.INSTANCE.makeFeatures(
                    "CpcCEgxnemlwLGRlZmxhdGUaAyovKiIWZW4tUEw7cT0xLCBydS1QTDtxPTAuOSpAY29tLnlhbmRleC5tb2JpbGUuYXV0aC5zZGsvNS4xNTEuNjg5MDQgKEFwcGxlIGlQYWQ4LDE7IGlPUyAxNC4wKTIPMjIwLjEyMC4yMDIuMjA3OJ6PA0INCghpc1RhYmxldBIBMUIRCglPU1ZlcnNpb24SBDE0LjBCDQoIaXNNb2JpbGUSATFCDgoJaXNCcm93c2VyEgEwQg8KCE9TRmFtaWx5EgNpT1NCEgoKRGV2aWNlTmFtZRIEaVBhZEIWCgtEZXZpY2VNb2RlbBIHaVBhZDgsMUIVCgxEZXZpY2VWZW5kb3ISBUFwcGxlEt4DCAYYASABKJsDMMWFs6EEOAZQNFi0C2DkygJwAoABAYgBAYoCuAMKAsAsCgLAKwoCwDAKAsAvCgIAnwoCAJ4KAsAkCgLAIwoCwCgKAsAnCgLACgoCwAkKAsAUCgLAEwoCAJ0KAgCcCgIAPQoCADwKAgA1CgIALwoCAAoSAQAaKgoCAAASJAAiAAAfbW9iaWxlcHJveHkucGFzc3BvcnQueWFuZGV4Lm5ldBoOCgIAChIIAAYAHQAXABgaCAoCAAsSAgEAGhoKAgANEhQAEgQBBQECAQQDBQMCAwICBgEGAxrnAQoCACMS4AHk4UG/IQCvrYTo0taukiZ+6/X5zZShsvQCLkO7/18DXN+T2G9mEMODFO+rnFEyMrSgti9DMjmO5M3tMDZSUzVYRTtnI0wHMQtnAwzAdN6V1vCOPNRDLoHWxvYzeMOCShYAFpNiS7Fs8HnOjNgDu5Rr0VE4K/9RNKsJwDLrARheHpXkezJ2TIbsHS2kvUcsJAA7o5Jz8jsdvsYukzAjYYeDqYyunAG+yZW1jjY7oFbHrDVROmUQU1fFf4X+wDeXRZFn8reTliRwAiy55VXdRbpVQC+hsom7OQA8eRF6RlhfKxoECgIAFxoHCgL/ARIBACCDBiiDBg==");

            YandexAssert.check(new JsonChecker(Files.readString(resource("features.json"))),
                    JsonType.NORMAL.toString(features));
        }
        {
            final JsonObject features = TrafficFeaturesMaker.INSTANCE.makeFeatures(
                    "CukBEgRnemlwKkpjb20ueWFuZGV4Lm1vYmlsZS5hdXRoLnNkay83LjE5LjAuNzE5MDAxOTkyIChzYW1zdW5nIFNNLUE1MTVGOyBBbmRyb2lkIDExKTIMNjIuMzMuMTE4LjM5OJzzAkIXCgtEZXZpY2VNb2RlbBIIU00tQTUxNUZCFwoMRGV2aWNlVmVuZG9yEgdTQU1TVU5HQg8KCU9TVmVyc2lvbhICMTFCDAoHaXNUb3VjaBIBMUINCghpc01vYmlsZRIBMUITCghPU0ZhbWlseRIHQW5kcm9pZEIOCglpc0Jyb3dzZXISATAStwQIBhgBIAEoiAQwwsOumwE4BlA5WKALYP//A3AIgAEBiAEBigKRBAoCEwEKAhMCCgITAwoCwCsKAsAsCgLMqQoCwC8KAsAwCgLMqAoCwBMKAsAUCgIAnAoCAJ0KAgAvCgIANRIBABoqCgIAABIkACIAAB9tb2JpbGVwcm94eS5wYXNzcG9ydC55YW5kZXgubmV0GgQKAgAXGgcKAv8BEgEAGg4KAgAKEggABgAdABcAGBoICgIACxICAQAa5wEKAgAjEuABa4o9ky6Xnn8xzymE0IlsiIN52nVlVD0DW3doIUyF17OEPIkwOxqR7vqHEM/5iUjipSJXpj6HV1JJ/4aYkwyITF4s/sLBv8oqaeQJv0J5XufSXRfgmXKgIY+WPKnKB8NxIfE9MvHsV0x05Uh1rA3NWaD4Cymj5s6qppGPvGMoOmWTo0PhEmu+k+7Kja/s8PBRgkrkredEcgqy8R2HttFSKLgjMMNsgnXPEJO6UriepKyExn61A0GfAPvbbq7qsuQUnwmg/xtLTnctKeqz4lO0oIVjaW1Gj8MU0yz6uM1fi1oaFAoCABASDgAMAmgyCGh0dHAvMS4xGgsKAgAFEgUBAAAAABoaCgIADRIUABIEAwgEBAEFAwgFBQEIBgYBAgEaLAoCADMSJgAkAB0AIPuu69Iuqs0SHYxa8PL8cuhQNVujQy+Gh+A/65fiBJsnGggKAgAtEgIBARoLCgIAKxIFBAMEAwMaBwoCABUSAQAggQYogwY=");

            YandexAssert.check(new JsonChecker(Files.readString(resource("features2.json"))),
                    JsonType.NORMAL.toString(features));
        }
        {
            final JsonObject features = TrafficFeaturesMaker.INSTANCE.makeFeatures(
                    "CpUCGhBhcHBsaWNhdGlvbi9qc29uKk1jb20ueWFuZGV4Lm1vYmlsZS5hdXRoLnNkay83LjE3LjIuNzE3MDIxODg4IChzYW1zdW5nIFNNLUczNTRMOyBBbmRyb2lkIDguOS4wKTIOMTc4LjE3Ni43NC4yMzY4qoQDQhYKBk9TTmFtZRIMQW5kcm9pZCBPcmVvQhcKC0RldmljZU1vZGVsEghTTS1HMzU0TEIXCgxEZXZpY2VWZW5kb3ISB1NBTVNVTkdCEgoJT1NWZXJzaW9uEgU4LjkuMEIMCgdpc1RvdWNoEgExQg0KCGlzTW9iaWxlEgExQg4KCWlzQnJvd3NlchIBMEITCghPU0ZhbWlseRIHQW5kcm9pZBKrAwgHEAcYASABKJwCMIWHrt4DOAZQbliUCmDw9QNwCIABAYgBAYECAAAAAAAA8D+KAvkCCgLALwoCwCsKAsAwCgLALAoCAJ4KAsAnCgIAZwoCwCgKAgBrCgIAowoCAJ8KAsypCgLMqAoCzKoKAsCvCgLArQoCwKMKAsCfCgLAXQoCwGEKAsBXCgLAUwoCAKIKAsCuCgLArAoCwKIKAsCeCgLAXAoCwGAKAsBWCgLAUgoCwCQKAgBqCgLAIwoCAEAKAsAKCgLAFAoCADkKAgA4CgLACQoCwBMKAgAzCgIAMgoCAJ0KAsChCgLAnQoCwFEKAgCcCgLAoAoCwJwKAsBQCgIAPQoCADwKAgA1CgIALwoCAP8SAQAaKgoCAAASJAAiAAAfbW9iaWxlcHJveHkucGFzc3BvcnQueWFuZGV4Lm5ldBoKCgIACxIEAwABAhoSCgIAChIMAAoAHQAXAB4AGQAYGgQKAgAjGgQKAgAWGgQKAgAXGjAKAgANEioAKAQDBQMGAwgHCAgICQgKCAsIBAgFCAYEAQUBBgEDAwMBAwIEAgUCBgIggQYogwY=");

            YandexAssert.check(new JsonChecker(Files.readString(resource("features3.json"))),
                    JsonType.NORMAL.toString(features));
        }
        {
            final JsonObject features = TrafficFeaturesMaker.INSTANCE.makeFeatures(
                    "CvwBEgRnemlwKkhjb20ueWFuZGV4Lm1vYmlsZS5hdXRoLnNkay83LjE2LjAuNzE2MDAxODUxIChIVUFXRUkgWUFMLUwyMTsgQW5kcm9pZCAxMCkyDTk0LjI1My45NS4xMDY44OsCQhYKDERldmljZVZlbmRvchIGSHVhd2VpQg8KCU9TVmVyc2lvbhICMTBCDAoHaXNUb3VjaBIBMUINCghpc01vYmlsZRIBMUIOCglpc0Jyb3dzZXISATBCEwoIT1NGYW1pbHkSB0FuZHJvaWRCFAoGT1NOYW1lEgpBbmRyb2lkIDEwQhYKC0RldmljZU1vZGVsEgdZQUwtTDIxErcECAoYASABKIgEMLmwq44OOAZQN1isC2D//wNwCIABAYgBAYoCkQQKAhMBCgITAgoCEwMKAsArCgLALAoCzKkKAsAvCgLAMAoCzKgKAsATCgLAFAoCAJwKAgCdCgIALwoCADUSAQAaKgoCAAASJAAiAAAfbW9iaWxlcHJveHkucGFzc3BvcnQueWFuZGV4Lm5ldBoECgIAFxoHCgL/ARIBABoOCgIAChIIAAYAHQAXABgaCAoCAAsSAgEAGucBCgIAIxLgARC3XV5iybmdiiLPPM5YDZpFt3zhW3gYWxuOuI27yqQkKuAIYI+FXJ7nSpMj2keputdIt2kFDECTGjdVoYVFsWOAMJ9sqq5mMkKnBGMhxQvO2GLALOtzDiflLJq/8Y3Xnsv/tnfKiUl40wI72BoebiuiFpvj9Mnn0ds2xtRH6kSj9ruQvePPjNLLPDDQlpfLJH/7ontwdbMop6sRZMKs8+XcJvvUgEshY9RD+6ZdbN57V2gsbTbM+di70S1fsmAvt1tY0eu89Q+qkTPA42tqwjfVe4PSHnrqKhalJ4kZsdIFGhQKAgAQEg4ADAJoMghodHRwLzEuMRoLCgIABRIFAQAAAAAaGgoCAA0SFAASBAMIBAQBBQMIBQUBCAYGAQIBGiwKAgAzEiYAJAAdACA1GGKBoqzT9GVGfAei0I8iwl8iX9rD+o+t9YZIL3ewDxoICgIALRICAQEaCwoCACsSBQQDBAMDGgcKAgAVEgEAIIEGKIMG");

            YandexAssert.check(new JsonChecker(Files.readString(resource("features4.json"))),
                    JsonType.NORMAL.toString(features));
        }
    }
}
