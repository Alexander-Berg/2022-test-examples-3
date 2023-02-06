const inherit = require('inherit');
const { create, Entity } = require('bem-page-object');
const PO = {};

const ReactEntity = inherit(Entity, null, { preset: 'react' });

PO.perfectionModal = new Entity({ block: 'PerfectionModal' });
PO.perfectionModal.wrapper = new Entity('.Modal-Wrapper');
PO.perfectionModal.wikiText = new Entity('.wiki-doc');
PO.perfectionModal.content = new ReactEntity({ block: 'ModalLayout', elem: 'Wrapper' });
PO.perfectionModal.content.title = new ReactEntity({ block: 'PerfectionIssue', elem: 'Title' });
PO.perfectionModal.menu = new ReactEntity({ block: 'Menu' });
PO.perfectionModal.menu.clarity = new Entity('[id="clarity"]');
PO.perfectionModal.menu.resources = new Entity('[id="resources"]');
PO.perfectionModal.menu.structure = new Entity('[id="structure"]');
PO.perfectionModal.menu.team = new Entity('[id="team"]');
PO.perfectionModal.menu.complaints = new Entity('[id="complaints"]');
PO.perfectionModal.firstIssue = new ReactEntity({ block: 'PerfectionIssue' }).nthType(2);
PO.perfectionModal.firstIssue.title = new ReactEntity({ block: 'PerfectionIssue', elem: 'Title' });
PO.perfectionModal.secondIssue = new ReactEntity({ block: 'PerfectionIssue' }).nthType(3);
PO.perfectionModal.secondIssue.title = new ReactEntity({ block: 'PerfectionIssue', elem: 'Title' });
PO.perfectionModal.appealBlock = new ReactEntity({ block: 'PerfectionAppeal' });
PO.perfectionModal.appealBlock.notProblemButton = new Entity('button');
PO.perfectionModal.appealBlock.form = new ReactEntity({ block: 'PerfectionAppeal', elem: 'Form' });
PO.perfectionModal.appealBlock.form.textarea = new Entity({ block: 'textarea', elem: 'control' });
PO.perfectionModal.appealBlock.explanation = new ReactEntity({ block: 'PerfectionAppeal', elem: 'Explanation' });
PO.perfectionModal.appealBlock.activeSubmitButton = new Entity('button[type="submit"][aria-disabled="false"]');
PO.perfectionModal.appealBlock.disabledSubmitButton = new Entity('button[type="submit"][aria-disabled="true"]');
PO.perfectionModal.appealBlock.cancelButton = new Entity('button[type="button"]');

PO.serviceHeader = new Entity({ block: 'abc-service', elem: 'header' });
PO.serviceTeam = new Entity({ block: 'service-team' });
PO.serviceTeam.scope = new Entity({ block: 'service-team-scope' });
PO.serviceHeader.perfectionInfo = new ReactEntity({ block: 'PerfectionServiceInfo' });
PO.serviceHeader.perfectionInfo.status = new ReactEntity({ block: 'PerfectionServiceInfo', elem: 'Status' });
PO.serviceHeader.perfectionInfo.status.trafficLights = new ReactEntity({ block: 'PerfectionTrafficLights' });
PO.serviceHeader.perfectionInfo.status.trafficLights.criticalIcon = new ReactEntity({ block: 'PerfectionIssueLevelIcon' }).mods({ level: 'critical' });
PO.serviceHeader.perfectionInfo.problemsDescription = new ReactEntity({ block: 'PerfectionStamp', elem: 'Description' });
PO.serviceHeader.perfectionInfo.problemsDescription.fixButton = new Entity('button');

PO.visiblePopup2 = new Entity('.Popup2_visible');

module.exports = create(PO);
