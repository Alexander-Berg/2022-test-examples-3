const inherit = require('inherit');
const { create, Entity } = require('bem-page-object');
const PO = {};

const ReactEntity = inherit(Entity, null, { preset: 'react' });

PO.toolsLamp = new Entity('.tools-lamp');
PO.yndxBug = new Entity('.YndxBug');

PO.wrapper = new Entity('.Quotas');
PO.wrapper.spin = new Entity('.Spin2');

PO.list = new Entity('.QuotasList');

PO.list.item = new Entity('.QuotasItem');

PO.list.item1 = PO.list.item.firstChild();
PO.list.item1.spin = new Entity('.Spin2');

PO.list.preFilterService = new Entity('.Properties-Property_type_project .Properties-PropertyValue .yc-link[href*="asd2"]');

PO.list.item1.status = new Entity('.DispenserStatus');

PO.list.item1.actions = new Entity('.RequestCard-Actions');
PO.list.item1.actions.action1 = new Entity('.Button2').firstChild();
PO.list.item1.actions.actionCancel = new Entity('.hermione-CANCELLED');

PO.filters = new Entity('.Filters');
PO.filters.service = new Entity('.Filters-Service');
PO.filters.service.input = new Entity('input');
PO.filters.service.chosen = new Entity('.ToolsSuggest-Chosen');
PO.filters.reset = new Entity('.Filters-Reset');

PO.pagination = new Entity('.Pagination');
PO.pagination.p1 = new Entity('#pagination-1');
PO.pagination.p2 = new Entity('#pagination-2');

PO.summary = new Entity('.Summary');

PO.expandButton = new ReactEntity({ block: 'ExpandButtons', elem: 'Expand' });
PO.collapseButton = new ReactEntity({ block: 'ExpandButtons', elem: 'Collapse' });
PO.sidebarButton = new ReactEntity({ block: 'FolderTable', elem: 'SidebarButton' });

PO.changes = new Entity('.DispenserChanges');
PO.changes.cpu = new Entity('.DispenserChanges-ResourceLabel[title="CPU"]');
PO.changes.details = new Entity('.DispenserChanges-ProviderDetails');
PO.changes.remaindersHandler = new Entity('.Remainders-Handler');

PO.resourcesForm = new Entity('.ResourcesForm');
PO.resourcesForm.serviceSuggest = new Entity('.ResourcesForm-Service');
PO.resourcesForm.serviceSuggest.chosen = new Entity('.ToolsSuggest-Chosen');
PO.resourcesForm.provider = new Entity('.ResourcesForm-Provider');
PO.resourcesForm.row = new Entity('.ResourcesForm-Row');

PO.folderTable = new ReactEntity({ block: 'FolderTable' });
PO.folderTable.chevron = new ReactEntity({ block: 'FolderTable', elem: 'ChevronButton' });

PO.folderTable.col1 = new ReactEntity({ block: 'FolderTable', elem: 'Col1' });
PO.folderTable.col2 = new ReactEntity({ block: 'FolderTable', elem: 'Col2' });

PO.folderTable.header = new ReactEntity({ block: 'FolderTable', elem: 'Header' });
PO.folderTable.header.folderCell = PO.folderTable.col1;
PO.folderTable.header.folderCell.expandButton = PO.expandButton;
PO.folderTable.header.folderCell.collapseButton = PO.collapseButton;
PO.folderTable.header.providerCell = PO.folderTable.col2;
PO.folderTable.header.providerCell.expandButton = PO.expandButton;
PO.folderTable.header.providerCell.collapseButton = PO.collapseButton;

PO.folderTable.folderRow = new ReactEntity({ block: 'FolderTable', elem: 'Folder' });
PO.folderTable.providerRow = new ReactEntity({ block: 'FolderTable', elem: 'Provider' });
PO.folderTable.folderRow3 = PO.folderTable.folderRow.nthType(4);
PO.folderTable.folderRow5 = PO.folderTable.folderRow.nthType(6);
PO.folderTable.folderRow5Collapsed = PO.folderTable.folderRow5.mix(PO.folderTable.folderRow.mods({ collapsed: true }));
PO.folderTable.folderRow5.folderCell = PO.folderTable.col1;
PO.folderTable.folderRow5.folderCell.sidebarIcon = PO.sidebarButton;
PO.folderTable.folderRow5.folderCell.chevron = PO.folderTable.chevron;
PO.folderTable.folderRow3.providerRow1 = PO.folderTable.providerRow.nthType(1);
PO.folderTable.folderRow3.providerRow1Collapsed = PO.folderTable.folderRow3.providerRow1
    .mix(PO.folderTable.providerRow.mods({ collapsed: true }));
PO.folderTable.folderRow3.providerRow1.providerCell = PO.folderTable.col2;
PO.folderTable.folderRow3.providerRow1.providerCell.sidebarIcon = PO.sidebarButton;
PO.folderTable.folderRow3.providerRow1.providerCell.chevron = PO.folderTable.chevron;
PO.folderTable.folderRow3.providerRow1.resourceRow2 = new ReactEntity({ block: 'FolderTable', elem: 'Resource' }).nthType(2);
PO.folderTable.folderRow3.providerRow1.resourceRow2.rootResource = new ReactEntity({ block: 'FolderTable', elem: 'RootResourceCell' });
PO.folderTable.folderRow3.providerRow1.resourceRow2.rootResource.arrow = PO.folderTable.chevron;

PO.folderSidebar = new Entity('.FoldersSidebar-Body');
PO.folderSidebar.accounts1 = new Entity('.FoldersSidebar-Accounts');
PO.folderSidebar.accounts1.header = new Entity('.FoldersSidebar-Subheader');
PO.folderSidebar.accounts1.header.expandButton = new Entity('.FoldersSidebar-ExpandButtonsGroup .Button2:nth-of-type(1)');
PO.folderSidebar.accounts1.header.collapseButton = new Entity('.FoldersSidebar-ExpandButtonsGroup .Button2:nth-of-type(2)');
PO.folderSidebar.accounts1.header.addButton = new Entity('.FoldersSidebar-ControlButton');
PO.folderSidebar.accounts1.accountCreationForm = new Entity('.CreateProviderAccount');
PO.folderSidebar.accounts1.account1_mode_view = new Entity('.FoldersSidebar-Account_mode_view:nth-of-type(2)');
PO.folderSidebar.accounts1.account1_mode_view.accordionChevron = new Entity('.Accordion-Chevron');
PO.folderSidebar.accounts1.account1_mode_view.changeButton = new Entity('.FoldersSidebar-ControlButton');
PO.folderSidebar.accounts1.account1_mode_edit = new Entity('.FoldersSidebar-Account_mode_edit:nth-of-type(2)');
PO.folderSidebar.accounts1.account1_mode_edit.editQuota = new Entity('.FoldersSidebar-EditQuota');

PO.deltaInput = new Entity('.FoldersQuotas-ColDelta').descendant(new Entity('.Textinput-Control'));

PO.transferForm = new Entity('.TransferForm');
PO.transferForm.quotas = new Entity('.FoldersQuotas');
PO.transferForm.quotas.table = new Entity('.FoldersQuotas-Table');
PO.transferForm.quotas.table.head = new Entity('.FoldersQuotas-Thead');
PO.transferForm.quotas.table.resourceSelect = new Entity('.FoldersQuotas-ControlSelect');
PO.transferForm.quotas.table.resource = new Entity('.FoldersQuotas-Resource');
PO.transferForm.quotas.table.resource.deltaInput = PO.deltaInput;
PO.transferForm.quotas.table.resource.deleteButton = new Entity('.FoldersQuotas-BtnBasket');
PO.transferForm.quotas.table.secondResource = PO.transferForm.quotas.table.resource.nthType(2);
PO.transferForm.quotas.table.secondResource.deltaInput = PO.deltaInput;

PO.transferForm.quotas.providerSelect = new Entity('.FoldersQuotas-ControlSelect');
PO.transferForm.quotas.addResourceButton = new Entity('.FoldersQuotas-ControlButton');

PO.transferForm.footer = new Entity('.FoldersQuotas-Footer');
PO.transferForm.footer.submitButton = new Entity('.FoldersQuotas-FooterColButtons').descendant(new Entity('.Button2'));

PO.transferForm.errorMessage = new Entity('.Message_type_error');

PO.transferRequest = new Entity('.DispenserTransferRequest');

PO.transferRequestsList = new Entity('.DispenserTransferRequestsList');

module.exports = create(PO);
