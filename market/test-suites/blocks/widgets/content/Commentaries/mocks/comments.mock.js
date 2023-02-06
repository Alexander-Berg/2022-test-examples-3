import {profiles} from '@self/platform/spec/hermione/configs/profiles';

function newComment(props = {}) {
    return {
        id: '1234',
        state: 'NEW',
        entityId: props.entityId,
        deleted: false,
        votes: {
            dislikeCount: 0,
            likeCount: 0,
            userVote: 0,
        },
        ...props,
    };
}

const users = [{
    id: profiles.ugctest3.uid,
    uid: {
        value: profiles.ugctest3.uid,
    },
    public_id: profiles.ugctest3.publicId,
    login: 'ugctest3',
    dbfields: {
        'userinfo.firstname.uid': 'firstName',
        'userinfo.lastname.uid': 'lastName',
    },
    display_name: {
        name: 'Vasya',
        public_name: 'Vasya P.',
        avatar: {},
    },
}];

const othersComment = entityId => [{
    author: {id: '123456'},
    id: 1234,
    entity: 'commentary',
    entityId,
    state: 'NEW',
    text: `${'Почитай меня. '.repeat(200)} the end`,
}];

const ownComment = (uid, entityId) => ([{
    author: {id: uid},
    entityId,
    id: 12341,
    state: 'NEW',
    text: 'Мой комментарий',
    votes: {
        likeCount: 0,
        dislikeCount: 0,
        userVote: 0,
    },
}]);

const newComments = ({count, entityId, entity}) => {
    if (count < 1) {
        return [];
    }
    return [...Array(count)].map((element, index) => newComment({id: index + 1, entity, entityId}));
};

export {
    users,
    othersComment,
    ownComment,
    newComment,
    newComments,
};
