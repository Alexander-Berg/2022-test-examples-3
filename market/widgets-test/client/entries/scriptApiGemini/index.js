import namespace from 'src/main/client/helpers/affiliateGlobal';
import {setupWidgetsContext} from 'src/widgets-create/client/helpers/widgetsContext';

setupWidgetsContext(window.__YaMarketAffiliate_contextParams__);

const getWidgetCreator = async creatorName => (await import(`./stubs/${creatorName}`)).default;

const createWidget = async ({containerId, ...params}) => {
    const Creator = await getWidgetCreator(params.type);

    return Creator.createWidget({
        containerEl: document.getElementById(containerId),
        ...params,
    });
};

namespace.createWidget = async params => {
    const widget = await createWidget(params);

    return widget.getPublicWidget();
};
