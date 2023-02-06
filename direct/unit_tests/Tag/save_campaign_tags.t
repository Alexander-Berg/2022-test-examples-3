#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

local $Yandex::DBShards::IDS_LOG_FILE = undef;

*save = *Tag::save_campaign_tags;
*get  = *Tag::get_all_campaign_tags;


my %db = (
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, cid =>  1, tag_name => 'first метка' },
                { tag_id => 2, cid =>  1, tag_name => 'second метка' },
                { tag_id => 3, cid =>  1, tag_name => 'third метка' },
            ],
            2 => [
                { tag_id => 4, cid => 11, tag_name => 'first метка' },
                { tag_id => 5, cid => 11, tag_name => 'second метка' },
                { tag_id => 6, cid => 11, tag_name => 'third метка' },
            ],
        },
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, pid => 1 },
                { tag_id => 2, pid => 1 },
                { tag_id => 2, pid => 2 },
                { tag_id => 3, pid => 2 },
            ],
            2 => [
                { tag_id => 4, pid => 11 },
                { tag_id => 5, pid => 11 },
                { tag_id => 5, pid => 12 },
                { tag_id => 6, pid => 12 },

            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid =>  1 },
                { pid =>  2 },
            ],
            2 => [
                { pid => 11 },
                { pid => 12 },
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id => 1, ClientID =>  1 },
            { tag_id => 2, ClientID =>  1 },
            { tag_id => 3, ClientID =>  1 },
            { tag_id => 4, ClientID => 11 },
            { tag_id => 5, ClientID => 11 },
            { tag_id => 6, ClientID => 11 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid =>  1, ClientID =>  1 },
            { cid =>  2, ClientID =>  2 },
            { cid => 11, ClientID => 11 },
            { cid => 12, ClientID => 12 },

        ],
        no_check => 1,
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
                { pid =>  1, ClientID =>  1 },
                { pid =>  2, ClientID =>  1 },
                { pid => 11, ClientID => 11 },
                { pid => 12, ClientID => 12 },
        ],
        no_check => 1,
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1, shard => 1 },
            { ClientID =>  2, shard => 1 },
            { ClientID => 11, shard => 2 },
            { ClientID => 12, shard => 2 },
        ],
        no_check => 1,
    },
);
my $new_tags;
my $new_tags_re = re(q/^(новая метка|third новая метка|еще метка)$/);
my $new_tags_array = [ 
    { name => 'новая метка',       tag_id => re('^(7|8|9)$') },
    { name => 'third новая метка', tag_id => re('^(7|8|9)$') },
    { name => 'еще метка',         tag_id => re('^(7|8|9)$') }
];

# кейс #1 на кампании есть три метки - одну изменяем, две других удаляем, добавляем новую
init_test_dataset(\%db);
lives_ok {
    undef $new_tags;
    $new_tags = save(1, [
            { tag_id => 2, name => 'отредактированная second метка' },
            { tag_id => 0, name => 'новая метка' },
        ], return_inserted_tag_ids => 1
    );
} 'shard 1: saving tags on campaign with existing tags';
cmp_bag(
    $new_tags,
    [ { name => 'новая метка', tag_id => 7 } ],
    'check returned data (new tags)'
);
cmp_bag(
    get(1),
    [
        { tag_id => 2, uses_count => 2, value => 'отредактированная second метка' },
        { tag_id => 7, uses_count => 0, value => 'новая метка' },
    ],
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { tag_id => 2, cid =>  1, tag_name => 'отредактированная second метка' },
                    { tag_id => 7, cid =>  1, tag_name => 'новая метка' },
                ],
                # в этом шарде ничего не изменилось
                2 => $db{tag_campaign_list}->{rows}->{2},
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    # удалались привязки для удаленных меток
                    { tag_id => 2, pid => 1 },
                    { tag_id => 2, pid => 2 },
                ],
                # ничего не изменилось
                2 => $db{tag_group}->{rows}->{2},
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                # удалилась привязка удаленных меток
                { tag_id => 2, ClientID =>  1 },
                { tag_id => 4, ClientID => 11 },
                { tag_id => 5, ClientID => 11 },
                { tag_id => 6, ClientID => 11 },
                # добавилась привязка для новой метки
                { tag_id => 7, ClientID =>  1 },
            ],
        },
    },
    'check database data'
);

# кейс #2 - добавляем метки на чистую кампанию
init_test_dataset(\%db);
lives_ok {
    undef $new_tags;
    $new_tags = save(2, [
            # { tag_id => 2, name => 'отредактированная second метка' },
            { tag_id => 0, name => 'новая метка' },
            {              name => 'еще метка' },
            { tag_id => 0, name => 'third новая метка' },
        ], return_inserted_tag_ids => 1
    );
} 'shard 1: saving tags on campaign without tags';
cmp_bag(
    $new_tags,
    $new_tags_array,
    'check returned data (new tags)'
);
cmp_bag(
    get(2),
    [
        { tag_id => 7, uses_count => 0, value => $new_tags_re },
        { tag_id => 8, uses_count => 0, value => $new_tags_re },
        { tag_id => 9, uses_count => 0, value => $new_tags_re },
    ],
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { tag_id => 1, cid =>  1, tag_name => 'first метка' },
                    { tag_id => 2, cid =>  1, tag_name => 'second метка' },
                    { tag_id => 3, cid =>  1, tag_name => 'third метка' },
                    { tag_id => 7, cid =>  2, tag_name => $new_tags_re   },
                    { tag_id => 8, cid =>  2, tag_name => $new_tags_re   },
                    { tag_id => 9, cid =>  2, tag_name => $new_tags_re   },
                ],
                # в этом шарде ничего не изменилось
                2 => $db{tag_campaign_list}->{rows}->{2},
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            # ничего не изменилось (метки новые и не привязаны к объявлениям)
            rows => $db{tag_group}->{rows},
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                { tag_id => 1, ClientID =>  1 },
                { tag_id => 2, ClientID =>  1 },
                { tag_id => 3, ClientID =>  1 },
                { tag_id => 4, ClientID => 11 },
                { tag_id => 5, ClientID => 11 },
                { tag_id => 6, ClientID => 11 },
                # добавились привязки для новых меток
                { tag_id => 7, ClientID =>  2 },
                { tag_id => 8, ClientID =>  2 },
                { tag_id => 9, ClientID =>  2 },
            ],
        },
    },
    'check database data'
);

# кейс #3 (аналогичен #1, но в другом шарде)
init_test_dataset(\%db);
lives_ok {
    undef $new_tags;
    $new_tags = save(11, [
            { tag_id => 5, name => 'отредактированная second метка' },
            { tag_id => 0, name => 'новая метка' },
        ], return_inserted_tag_ids => 1
    );
} 'shard 2: saving tags on campaign with existing tags';
cmp_bag(
    $new_tags,
    [ { name => 'новая метка', tag_id => 7 } ],
    'check returned data (new tags)'
);
cmp_bag(
    get(11),
    [
        { tag_id => 5, uses_count => 2, value => 'отредактированная second метка' },
        { tag_id => 7, uses_count => 0, value => 'новая метка' },
    ],
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                # в этом шарде ничего не изменилось
                1 => $db{tag_campaign_list}->{rows}->{1},
                2 => [
                    { tag_id => 5, cid => 11, tag_name => 'отредактированная second метка' },
                    { tag_id => 7, cid => 11, tag_name => 'новая метка' },
                ],
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                # ничего не изменилось
                1 => $db{tag_group}->{rows}->{1},
                2 => [
                    # удалались привязки для удаленных меток
                    { tag_id => 5, pid => 11 },
                    { tag_id => 5, pid => 12 },
                ],
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                { tag_id => 1, ClientID =>  1 },
                { tag_id => 2, ClientID =>  1 },
                { tag_id => 3, ClientID =>  1 },
                { tag_id => 5, ClientID => 11 },
                # удалилась привязка удаленных меток
                # { tag_id => 4, ClientID => 11 },
                # { tag_id => 6, ClientID => 11 },
                # добавилась привязка для новой метки
                { tag_id => 7, ClientID => 11 },
            ],
        },
    },
    'check database data'
);

# кейс #4 - (#2, но во втором шарде)
init_test_dataset(\%db);
lives_ok {
    undef $new_tags;
    $new_tags = save(12, [
            # { tag_id => 2, name => 'отредактированная second метка' },
            { tag_id => 0, name => 'новая метка' },
            {              name => 'еще метка' },
            { tag_id => 0, name => 'third новая метка' },
        ], return_inserted_tag_ids => 1
    );
} 'shard 2: saving tags on campaign without tags';
cmp_bag(
    $new_tags,
    $new_tags_array,
    'check returned data (new tags)'
);
cmp_bag(
    get(12),
    [
        { tag_id => 7, uses_count => 0, value => $new_tags_re },
        { tag_id => 8, uses_count => 0, value => $new_tags_re },
        { tag_id => 9, uses_count => 0, value => $new_tags_re },
    ],
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                # в этом шарде ничего не изменилось
                1 => $db{tag_campaign_list}->{rows}->{1},
                2 => [
                    { tag_id => 4, cid => 11, tag_name => 'first метка' },
                    { tag_id => 5, cid => 11, tag_name => 'second метка' },
                    { tag_id => 6, cid => 11, tag_name => 'third метка' },
                    { tag_id => 7, cid => 12, tag_name => $new_tags_re   },
                    { tag_id => 8, cid => 12, tag_name => $new_tags_re   },
                    { tag_id => 9, cid => 12, tag_name => $new_tags_re   },
                ],
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            # ничего не изменилось (метки новые и не привязаны к объявлениям)
            rows => $db{tag_group}->{rows},
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                { tag_id => 1, ClientID =>  1 },
                { tag_id => 2, ClientID =>  1 },
                { tag_id => 3, ClientID =>  1 },
                { tag_id => 4, ClientID => 11 },
                { tag_id => 5, ClientID => 11 },
                { tag_id => 6, ClientID => 11 },
                # добавились привязки для новых меток
                { tag_id => 7, ClientID => 12 },
                { tag_id => 8, ClientID => 12 },
                { tag_id => 9, ClientID => 12 },
            ],
        },
    },
    'check database data'
);

# и опять-таки по мотивам тестов АПИ. метки должны удалиться, и вместо них создаться новые
init_test_dataset(\%db);
lives_ok { 
    undef $new_tags;
    $new_tags = save(
        11,
        [
            { tag_id => 0, name => 'first метка' },
            { tag_id => 0, name => 'second метка' },
            { tag_id => 0, name => 'third метка' },
        ],
        return_inserted_tag_ids => 1
    );
} 'shard 2: saving duplicate tags (as new) on campaign with existing tags';
cmp_bag(
    $new_tags,
    [ 
        { name => 'first метка',  tag_id => re('^(7|8|9)$') },
        { name => 'second метка', tag_id => re('^(7|8|9)$') },
        { name => 'third метка',  tag_id => re('^(7|8|9)$') }
    ],
    'check returned data (new tags)'
);
cmp_bag(
    get(11),
    [
        { tag_id => re('7|8|9'), uses_count => 0, value => 'first метка' },
        { tag_id => re('7|8|9'), uses_count => 0, value => 'second метка' },
        { tag_id => re('7|8|9'), uses_count => 0, value => 'third метка' },
    ],
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset({
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{tag_campaign_list}->{rows}->{1},
            2 => [
                # это новые метки с новыми id, старые (4,5,6) - удаленны
                { tag_id => re('7|8|9'), cid => 11, tag_name => 'first метка' },
                { tag_id => re('7|8|9'), cid => 11, tag_name => 'second метка' },
                { tag_id => re('7|8|9'), cid => 11, tag_name => 'third метка' },
            ],
        },
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{tag_group}->{rows}->{1},
            # все метки удалили - вместе с привязками к группам
            2 => [],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id => 1, ClientID =>  1 },
            { tag_id => 2, ClientID =>  1 },
            { tag_id => 3, ClientID =>  1 },
            # 4, 5, 6 - удалены
            { tag_id => 7, ClientID => 11 },
            { tag_id => 8, ClientID => 11 },
            { tag_id => 9, ClientID => 11 },
        ],
    },
}, 'check database data (tags was deleted and created)');

# кейс 5 - переименование меток
init_test_dataset(\%db);
lives_ok { 
    undef $new_tags;
    $new_tags = save(
        1,
        [
            { tag_id => 2, name => 'first метка' },
            { tag_id => 3, name => 'second метка' },
            { tag_id => 1, name => 'third метка' },
        ],
        return_inserted_tag_ids => 1
    );
} 'shard 1: renaming tags on campaign (2->1->3->2)';
cmp_bag($new_tags, [ ], 'check returned data (no new tags)');
cmp_bag(
    get(1),
    [
        { tag_id => 1, uses_count => 1, value => 'third метка' },
        { tag_id => 2, uses_count => 2, value => 'first метка' },
        { tag_id => 3, uses_count => 1, value => 'second метка' },
    ],
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { tag_id => 2, cid =>  1, tag_name => 'first метка' },
                    { tag_id => 3, cid =>  1, tag_name => 'second метка' },
                    { tag_id => 1, cid =>  1, tag_name => 'third метка' },
                ],
                2 => $db{tag_campaign_list}->{rows}->{2},
            },
        },
        # привязки меток (по tag_id) к группам - не должны были измениться
        tag_group => $db{tag_group},
        shard_inc_tag_id => $db{shard_inc_tag_id},
    },
    'check database data'
);

init_test_dataset(\%db);
lives_ok { 
    undef $new_tags;
    $new_tags = save(
        11,
        [
            { tag_id => 4, name => 'third метка' },
            { tag_id => 5, name => 'first метка' },
            { tag_id => 6, name => 'second метка' },
        ],
        return_inserted_tag_ids => 1
    );
} 'shard 2: renaming tags on campaign (5->6->4->5)';
cmp_bag($new_tags, [ ], 'check returned data (no new tags)');
cmp_bag(
    get(11),
    [
        { tag_id => 4, uses_count => 1, value => 'third метка' },
        { tag_id => 5, uses_count => 2, value => 'first метка' },
        { tag_id => 6, uses_count => 1, value => 'second метка' },
    ],
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => $db{tag_campaign_list}->{rows}->{1},
                2 => [
                    { tag_id => 5, cid => 11, tag_name => 'first метка' },
                    { tag_id => 6, cid => 11, tag_name => 'second метка' },
                    { tag_id => 4, cid => 11, tag_name => 'third метка' },
                ],
            },
        },
        # привязки меток (по tag_id) к группам - не должны были измениться
        tag_group => $db{tag_group},
        shard_inc_tag_id => $db{shard_inc_tag_id},
    },
    'check database data'
);

# на всякий случай проверим, что если сохранить метки в том виде, как они есть на кампании - ничего не изменится
init_test_dataset(\%db);
lives_ok { 
    undef $new_tags;
    $new_tags = save(
        1,
        [
            { tag_id => 1, name => 'first метка' },
            { tag_id => 2, name => 'second метка' },
            { tag_id => 3, name => 'third метка' },
        ],
        return_inserted_tag_ids => 1
    );
} 'shard 1: saving tags on campaign as is';
cmp_bag($new_tags, [ ], 'check returned data (no new tags)');
cmp_bag(
    get(1),
    [
        { tag_id => 1, uses_count => 1, value => 'first метка' },
        { tag_id => 2, uses_count => 2, value => 'second метка' },
        { tag_id => 3, uses_count => 1, value => 'third метка' },
    ],
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(\%db, 'check database data');

init_test_dataset(\%db);
lives_ok { 
    undef $new_tags;
    $new_tags = save(
        11,
        [
            { tag_id => 4, name => 'first метка' },
            { tag_id => 5, name => 'second метка' },
            { tag_id => 6, name => 'third метка' },
        ],
        return_inserted_tag_ids => 1
    );
} 'shard 2: saving tags on campaign as is';
cmp_bag($new_tags, [ ], 'check returned data (no new tags)');
cmp_bag(
    get(11),
    [
        { tag_id => 4, uses_count => 1, value => 'first метка' },
        { tag_id => 5, uses_count => 2, value => 'second метка' },
        { tag_id => 6, uses_count => 1, value => 'third метка' },
    ],
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    \%db,
    'check database data'
);


init_test_dataset(\%db);
lives_ok { 
    undef $new_tags;
    $new_tags = save(
        11,
        [
            { tag_id => 4, name => 'FIRST метка' },
            { tag_id => 5, name => 'SECOND МЕТКА' },
            { tag_id => 6, name => 'THIRD МЕТКА' },
        ],
        return_inserted_tag_ids => 1
    );
} 'DIRECT-27467: saving same tags on campaign in diffrent case';
cmp_bag($new_tags, [ ], 'check returned data (no new tags)');
cmp_bag(
    get(11),
    [
        { tag_id => 4, uses_count => 1, value => 'FIRST метка' },
        { tag_id => 5, uses_count => 2, value => 'SECOND МЕТКА' },
        { tag_id => 6, uses_count => 1, value => 'THIRD МЕТКА' },
    ],
    'DIRECT-27467: check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => $db{tag_campaign_list}->{rows}->{1},
                2 => [
                    { tag_id => 4, cid => 11, tag_name => 'FIRST метка' },
                    { tag_id => 5, cid => 11, tag_name => 'SECOND МЕТКА' },
                    { tag_id => 6, cid => 11, tag_name => 'THIRD МЕТКА' },
                ],
            },
        },
        # привязки меток (по tag_id) к группам - не должны были измениться
        tag_group => $db{tag_group},
        shard_inc_tag_id => $db{shard_inc_tag_id},
    },
    'DIRECT-27467: check database data'
);

done_testing();
