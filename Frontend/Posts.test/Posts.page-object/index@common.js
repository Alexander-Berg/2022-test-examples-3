const { ReactEntity } = require('../../../../../../vendors/hermione');
const { post } = require('../../../../../../components/Post/Post.test/Post.page-object/index@common');

const elems = {};

elems.posts = new ReactEntity({ block: 'Posts' });
elems.posts.item = post.copy();
elems.posts.firstItem = post.nthChild(1);
elems.posts.secondItem = post.nthChild(2);
elems.posts.sixthItem = post.nthChild(6);
elems.posts.moreButton = new ReactEntity({ block: 'Posts', elem: 'MoreButton' });

module.exports = elems;
