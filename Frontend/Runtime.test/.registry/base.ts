import { AdaptersRegistry } from '../../..';

import { AdapterTestEmpty } from '../fixtures/AdapterTestEmpty';
import { AdapterTestTransform } from '../fixtures/AdapterTestTransform';
import { AdapterTestTransform_sub } from '../fixtures/AdapterTestTransform_sub';
import { AdapterTestAjax } from '../fixtures/AdapterTestAjax';
import { AdapterTestRender } from '../fixtures/AdapterTestRender';
import { AdapterTestRender_sub } from '../fixtures/AdapterTestRender_sub';
import { AdapterTestFull } from '../fixtures/AdapterTestFull';

const PLATFORM = 'desktop';

const registry = new AdaptersRegistry(PLATFORM);

registry.set({ type: 'test-empty' }, AdapterTestEmpty);
registry.set({ type: 'test-transform' }, AdapterTestTransform);
registry.set({ type: 'test-transform', subtype: 'sub' }, AdapterTestTransform_sub);
registry.set({ type: 'test-ajax' }, AdapterTestAjax);
registry.set({ type: 'test-render' }, AdapterTestRender);
registry.set({ type: 'test-render', subtype: 'sub' }, AdapterTestRender_sub);
registry.set({ type: 'test-render', subtype: 'condition' }, AdapterTestRender);
registry.set({ type: 'test-full' }, AdapterTestFull);

export default registry;
