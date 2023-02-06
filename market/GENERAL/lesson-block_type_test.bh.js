module.exports = function (bh) {
    bh.match('lesson-block_type_test', function (ctx, json) {
        var data = json.data;
        var exercises = data.exercises;

        ctx.content([
            {
                elem: 'exercises',
                items: exercises,
                testId: data.lessonLink + '-' + data.anchor,
                metrikaGoalId: data.metrikaGoalId
            }
        ]);
    });

    bh.match('lesson-block__exercises', function (ctx, json) {
        var items = json.items;

        if (!items || items.length === 0) {
            return '';
        }

        ctx.content(items.map(function (item, idx) {
            return {
                elem: 'exercise',
                content: {
                    block: 'lesson-exercise',
                    data: item,
                    exerciseId: json.testId + '-' + idx,
                    metrikaGoalId: json.metrikaGoalId
                }
            };
        }));
    });
};
