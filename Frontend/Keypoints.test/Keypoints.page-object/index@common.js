const { create, ReactEntity } = require('../../../../vendors/hermione');

const PO = {};
PO.Keypoints = new ReactEntity({ block: 'Keypoints' });
PO.KeypointsExpandable = PO.Keypoints.mods({ expandable: true });
PO.KeypointsNotExpandable = PO.Keypoints.mods({ expandable: false });
PO.Keypoints.Title = new ReactEntity({ block: 'Keypoints', elem: 'Title' });
PO.Keypoints.Content = new ReactEntity({ block: 'Keypoints', elem: 'Content' });
PO.Keypoints.Timeline = new ReactEntity({ block: 'Keypoints', elem: 'Timeline' });
PO.Keypoints.List = new ReactEntity({ block: 'Keypoints', elem: 'List' });
PO.Keypoints.ListInner = new ReactEntity({ block: 'Keypoints', elem: 'ListInner' });
PO.Keypoints.Item = new ReactEntity({ block: 'Keypoints', elem: 'Item' });
PO.Keypoints.FirstItem = PO.Keypoints.Item.nthChild(1);
PO.Keypoints.SecondItem = PO.Keypoints.Item.nthChild(2);
PO.Keypoints.ThirdItem = PO.Keypoints.Item.nthChild(3);
PO.Keypoints.LastItem = PO.Keypoints.Item.lastChild();

module.exports = create(PO);
