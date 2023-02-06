const Entity = require('bem-page-object').Entity;

const fSubmission = new Entity({ block: 'f-submission' });
fSubmission.header = new Entity({
    block: 'f-submission',
    elem: 'header',
});

fSubmission.actionReject = new Entity({
    block: 'f-submission-workflow',
})
    .mods({
        action: 'reject',
    });

fSubmission.actionHandle = new Entity({
    block: 'f-submission-workflow',
})
    .mods({
        action: 'handle',
    });

fSubmission.candidate = new Entity({
    block: 'f-submission-field',
}).mods({
    type: 'candidate',
});

fSubmission.candidate.link = new Entity({
    block: 'link',
});

module.exports = fSubmission;
