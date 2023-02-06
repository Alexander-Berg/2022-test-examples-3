import { assert } from 'chai';
import { getBlndrViewTypeIsText } from '../.';

describe('Blender view type checker', () => {
    it('should handle text type', () => {
        assert.isTrue(getBlndrViewTypeIsText({ markers: { blndrViewType: 'text' } }));
    });
    it('should handle not text type', () => {
        assert.isNotTrue(getBlndrViewTypeIsText({ markers: { blndrViewType: 'some-another-type' } }));
        assert.isNotTrue(getBlndrViewTypeIsText({ markers: {} }));
        assert.isNotTrue(getBlndrViewTypeIsText({}));
    });
});

