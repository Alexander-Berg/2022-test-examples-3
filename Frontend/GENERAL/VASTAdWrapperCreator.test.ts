import { normalizeVast } from '../utils/normalizeVast';
import { VASTAdWrapperCreator } from './VASTAdWrapperCreator';

describe('VASTAdWrapperCreator', () => {
    it('should create correct VAST Ad Wrapper', () => {
        const expectedVast = normalizeVast(`
            <?xml version="1.0"?>
            <Ad id="123">
                <Wrapper>
                    <AdSystem>Yabs Ad Server</AdSystem>
                    <VASTAdTagURI><![CDATA[https://path-to-vast]]></VASTAdTagURI>
                </Wrapper>
            </Ad>
        `);
        const vastAdCreator = new VASTAdWrapperCreator({
            wrapperUrl: 'https://path-to-vast',
            adBlockId: '123',
        });

        const resultVast = normalizeVast(vastAdCreator.getXMLString());

        expect(resultVast).toEqual(expectedVast);
    });
});
