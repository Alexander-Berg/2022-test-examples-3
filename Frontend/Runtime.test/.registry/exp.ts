import { AdaptersExpRegistry, ClientStaticExpRegistry } from "../../..";

import { adapterTestOnlyExperimental } from '../fixtures/AdapterTestOnlyExperimental';
import { adapterTestTransform as adapterTestTransformExp1 } from '../fixtures/AdapterTestTransform';
import { adapterTestTransform as adapterTestTransformExp2 } from '../fixtures/AdapterTestTransformExp2';
import { adapterTestTransform_sub } from '../fixtures/AdapterTestTransform_sub';
import { adapterTestAjax } from '../fixtures/AdapterTestAjax';
import { adapterTestRender as adapterTestRenderExp1 } from '../fixtures/AdapterTestRender';
import { adapterTestRender as adapterTestRenderExp2 } from '../fixtures/AdapterTestRenderExp2';
import { adapterTestRender_sub } from '../fixtures/AdapterTestRender_sub';
import { adapterTestRender as adapterForcedAssets } from '../fixtures/AdapterForcedAsset';
import { adapterTestRender as adapterNonForcedAssets } from '../fixtures/AdapterNonForcedAsset';
import { adapterTestCondition } from "../fixtures/AdapterTestCondition";
import { CssExpRegistry } from "../../../Registry";

const registry = new AdaptersExpRegistry<Record<string, string | number>, string, string | number>('desktop');
const componentsExpRegistry = new ClientStaticExpRegistry<Record<string, string | number>, string, string | number>('desktop');
const cssExpRegistry = new CssExpRegistry<Record<string, string | number>, string, string | number, Record<string, Record<string, string>>>('desktop');

registry.set('transformFlag1', { type: 'test-transform' }, adapterTestTransformExp1);
registry.set('transformFlag2', { type: 'test-transform' }, adapterTestTransformExp2);
registry.set('transformSubFlag', { type: 'test-transform', subtype: 'sub' }, adapterTestTransform_sub);
registry.set('ajaxFlag', { type: 'test-ajax' }, adapterTestAjax);
registry.set('renderFlag', { type: 'test-render' }, adapterTestRenderExp1);
registry.set('renderSubFlag', { type: 'test-render', subtype: 'sub' }, adapterTestRender_sub);
registry.set('renderFlag1', { type: 'test-render' }, adapterTestRenderExp1);
registry.set('renderFlag2', { type: 'test-render' }, adapterTestRenderExp2);
registry.set('renderCondition', { type: 'test-render', subtype: 'condition' }, adapterTestCondition);
registry.set('onlyExperimentalFlag', { type: 'test-only-experimental' }, adapterTestOnlyExperimental);
registry.set('forcedAssets', { type: 'test-render' }, adapterForcedAssets);
registry.set('nonForcedAssets', { type: 'test-render' }, adapterNonForcedAssets);

componentsExpRegistry.set('componentsFlag', [{
    features: [{ type: 'test-render' }],
    serverCondition: () => true,
    patch: function () { },
}]);

cssExpRegistry.set('cssExpFlag', [{
    features: [{ type: 'test-render' }],
    css: () => {
        return '.testRender{display:block}';
    },
}])

cssExpRegistry.set('globalCssExpFlag', [{
    features: '*',
    css: () => {
        return '.body{margin:0}';
    },
}])

export { componentsExpRegistry, registry as adaptersExpRegistry, cssExpRegistry as cssExperiments };
