use my_inc "../../../";
use Direct::Modern;

use Test::More skip_all => "tests broken";
use Direct::Model::BannerPerformance;
use Direct::Model::Creative;
use Test::CreateDBObjects;
use Test::Subtest;
use Yandex::Test::ValidationResult;

BEGIN {
    use_ok 'Direct::Validation::BannersPerformance';
}

create_tables;

subtest_ "Should validate creative / banner ownership" => sub {
    subtest_ "OK when they belong to same client" => sub {
        my $creative = Direct::Model::Creative->new(
            client_id => 1,
            id => 1,
        );
        my $banner = Direct::Model::BannerPerformance->new(
            client_id => 1,
            creative => $creative,
            creative_id => $creative->id,
        );
        ok_validation_result v([$banner]);
    };
    subtest_ "Error when client_id's don't match" => sub {
        my $creative = Direct::Model::Creative->new(
            client_id => 1,
            id => 1,
        );
        my $banner = Direct::Model::BannerPerformance->new(
            client_id => 2,
            creative => $creative,
            creative_id => $creative->id,
        );
        cmp_validation_result v([$banner]), [
            { creative => vr_errors('NotFound') },
        ];
    };
};

subtest_ "Creative should be used only once" => sub {
    my $mk = sub {
        my $creative_id = shift;
        Direct::Model::BannerPerformance->new(
            creative => Direct::Model::Creative->new(client_id => $creative_id, id => $creative_id + 1000),
            creative_id => $creative_id + 1000,
            client_id => $creative_id,
        ),
    };
    cmp_validation_result v([$mk->(1), $mk->(2), $mk->(3), $mk->(4), $mk->(1), $mk->(1), $mk->(2)]), [
        vr_errors('Duplicated'),
        vr_errors('Duplicated'),
        {},
        {},
        vr_errors('Duplicated'),
        vr_errors('Duplicated'),
        vr_errors('Duplicated'),
    ];
};

subtest_ "Should validate per-group banner count limit" => sub {
    my @banners = map {
        Direct::Model::BannerPerformance->new(
            creative => Direct::Model::Creative->new(client_id => $_ + 100, id => $_ + 200),
            creative_id => $_ + 200,
            client_id => $_ + 100,
        ),
    } 1..101;
    cmp_validation_result v(\@banners), vr_errors('LimitExceeded');
};

run_subtests();

sub v {
    &Direct::Validation::BannersPerformance::validate_performance_banners;
}

