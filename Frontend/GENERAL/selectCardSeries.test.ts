import { updateCurrentCard } from 'services/card/updateCurrentCard/updateCurrentCard';
import { testService } from 'services/spec/testService/testService';

import { jestBuilder } from 'spec/jest';

const builder = jestBuilder(__dirname);

describe('selectCardSeries', function() {
    testService('should update current card content if value is true', async function(base) {
        // arrange
        let state = await builder.card('5ef22c541993671b35808499').state();
        let service = base.getService('services/card/selectCardSeries/selectCardSeries');

        base.mockState(state);

        // act
        await base.callServiceAsync(service.selectCardSeries, { value: true, counter: { pageName: 'card' } });

        // assert
        base.assertServiceCall(updateCurrentCard, {
            value: {
                selected_contents: ['7afa90b3a55153da-5a8c2c4a7d82c'],
            },
        });

        base.assertCounter('clickAuthorsSelectAll', {
            action: 'select',
            pageName: 'card',
            type: 'image',
        });
    });

    testService('should update current card with empty contents if value is false', async function(base) {
        // arrange
        let state = await builder.card('5ef22c541993671b35808499').state();
        let service = base.getService('services/card/selectCardSeries/selectCardSeries');

        base.mockState(state);

        // act
        await base.callServiceAsync(service.selectCardSeries, { value: false, counter: { pageName: 'card' } });

        // assert
        base.assertServiceCall(updateCurrentCard, {
            value: {
                selected_contents: [],
            },
        });

        base.assertCounter('clickAuthorsSelectAll', {
            action: 'unselect',
            pageName: 'card',
            type: 'image',
        });
    });
});
