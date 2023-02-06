use Direct::Modern;

use Test::More;
use Test::Exception;

use Yandex::Test::ValidationResult;
use Direct::ValidationResult;
use Direct::Model::AdGroup;
use Settings;
use Direct::Validation::Errors;

BEGIN {
    use_ok('Direct::Validation::AdGroups', qw/
        validate_banners_adgroup
        validate_add_banners_adgroup
    /);
}

package Test::Banner1 {
    use Mouse;
    has num1 => (is => 'rw', isa => 'Num'); 
};

package Test::Banner2 {
    use Mouse;
    has num2 => (is => 'rw', isa => 'Num'); 
};

package Test::Banner3 {
    use Mouse;
    has num3 => (is => 'rw', isa => 'Num'); 
};

subtest 'valid banners' => sub {
    my $vr1 = validate_banners_adgroup({}, [], Direct::Model::AdGroup->new());
    ok_validation_result($vr1);
};

subtest 'only one kind of banners' => sub {
    my $banner1 = Test::Banner1->new(num1 => 0);
    my $banner2 = Test::Banner1->new(num1 => -19);
    my $banner3 = Test::Banner1->new(num1 => 89);
    my $banner4 = Test::Banner1->new(num1 => 104);

    my $validate_banner1 = sub  {
        my ($banners, $adgroup) = @_;
        my $vr_main = Direct::ValidationResult->new();
        for my $banner (@$banners) {
            my $vr = $vr_main->next;
            if ($banner->num1 < 0) {
                $vr->add(num1 => error_InvalidField());
            }
            if ($banner->num1 > 0) {
                $vr_main->add_generic(error_InvalidChars());
            }
            if ($banner->num1 == 104) {
                $vr_main->add_generic(error_EmptyField());
            }
        }
        return $vr_main;
    };

    my $vr1 = validate_banners_adgroup({
        'Test::Banner1' => $validate_banner1
    }, [$banner1, $banner2, $banner3, $banner4], Direct::Model::AdGroup->new());

    cmp_validation_result($vr1, {
        generic_errors => vr_errors('InvalidChars', 'EmptyField', 'InvalidChars'),
        objects_results => [
            {},
            {num1 => vr_errors('InvalidField')},
            {},
            {}
        ]
    });
};

subtest 'unsupported banner kind' => sub {
    my $banner1 = Test::Banner1->new(num1 => 600);
    my $banner2 = Test::Banner2->new(num2 => 0);
    my $banner4 = Test::Banner2->new(num2 => 500);
    my $banner3 = Test::Banner3->new();

    my $validate_banner1 = sub {
        my ($banners) = @_;
        my $vr_main = Direct::ValidationResult->new();
        for my $banner (@$banners) {
            my $vr = $vr_main->next;    
            if ($banner->num1 == 0) {
                $vr->add(num1 => error_EmptyField());
            }
        }           
        return $vr_main;
    };
    my $validate_banner2 = sub {
        my ($banners) = @_;
        my $vr_main = Direct::ValidationResult->new();
        for my $banner (@$banners) {
            my $vr = $vr_main->next;    
            if ($banner->num2 == 0) {
                $vr->add(num2 => error_EmptyField());
            }
        }           
        return $vr_main;
    };

    dies_ok {validate_banners_adgroup({'Test::Banner1' => $validate_banner1, 'Test::Banner2' => $validate_banner2}, [$banner1, $banner4, $banner3, $banner2], Direct::Model::AdGroup->new())} 'Unsupported Banner';
};

subtest "there aren't certain banners" => sub {
    my $banner1 = Test::Banner1->new(num1 => 600);
    my $banner2 = Test::Banner2->new(num2 => 0);
    my $banner3 = Test::Banner2->new(num2 => 600);

    my $validate_banner1 = sub {
        my ($banners) = @_;
        my $vr_main = Direct::ValidationResult->new();
        for my $banner (@$banners) {
            my $vr = $vr_main->next;    
            if ($banner->num1 == 0) {
                $vr->add(num1 => error_EmptyField());
            }
        }           
        return $vr_main;
    };
    my $validate_banner2 = sub {
        my ($banners) = @_;
        my $vr_main = Direct::ValidationResult->new();
        for my $banner (@$banners) {
            my $vr = $vr_main->next;    
            if ($banner->num2 == 0) {
                $vr->add(num2 => error_EmptyField());
            }
        }           
        return $vr_main;
    };
    my $validate_banner3 = sub {
        my ($banners) = @_;
        my $vr_main = Direct::ValidationResult->new();
        for my $banner (@$banners) {
            my $vr = $vr_main->next;    
            if ($banner->num3 == 0) {
                $vr->add(num3 => error_EmptyField());
            }
        }           
        return $vr_main;
    };

    my $vr1 = validate_banners_adgroup({
        'Test::Banner1' => $validate_banner1, 'Test::Banner2' => $validate_banner2,
        'Test::Banner3' => $validate_banner3
    }, [$banner1, $banner2, $banner3], Direct::Model::AdGroup->new());

    cmp_validation_result($vr1, [
       {},
       {num2 => vr_errors('EmptyField')},
       {}
    ]);

    my $vr2 = validate_banners_adgroup({
        'Test::Banner1' => $validate_banner1, 'Test::Banner2' => $validate_banner2,
        'Test::Banner3' => $validate_banner3
    }, [$banner1, $banner3], Direct::Model::AdGroup->new());
    ok_validation_result($vr2);
};

subtest 'accumulate generic errors' => sub {
    my $banner1 = Test::Banner1->new(num1 => 600);
    my $banner2 = Test::Banner2->new(num2 => 0);
    my $banner3 = Test::Banner2->new(num2 => 500);

    my $validate_banner1 = sub {
        my ($banners) = @_;
        my $vr_main = Direct::ValidationResult->new();
        for my $banner (@$banners) {
            my $vr = $vr_main->next;    
            $vr_main->add_generic(error_EmptyField());
            if ($banner->num1 == 555) {
                $vr->add(num1 => error_InvalidField());
            }
        }         
        return $vr_main;
    };
    my $validate_banner2 = sub {
        my ($banners) = @_;
        my $vr_main = Direct::ValidationResult->new();
        for my $banner (@$banners) {
            my $vr = $vr_main->next;    
            $vr_main->add_generic(error_InvalidChars());
            if ($banner->num2 == 555) {
                $vr->add(num2 => error_InvalidField());
            }
        }           
        return $vr_main;
    };

    my $vr1 = validate_banners_adgroup({
        'Test::Banner1' => $validate_banner1, 'Test::Banner2' => $validate_banner2
    }, [$banner1, $banner2, $banner3], Direct::Model::AdGroup->new());
    cmp_validation_result($vr1, vr_errors('EmptyField', 'InvalidChars', 'InvalidChars'));


    my $banner4 = Test::Banner1->new(num1 => 555);
    my $banner5 = Test::Banner2->new(num2 => 0);
    my $banner6 = Test::Banner2->new(num2 => 555);
    my $banner7 = Test::Banner1->new(num1 => 7812);

    my $vr2 = validate_banners_adgroup({
        'Test::Banner1' => $validate_banner1, 'Test::Banner2' => $validate_banner2
    }, [$banner4, $banner5, $banner6, $banner7], Direct::Model::AdGroup->new());
    cmp_validation_result($vr2, {
        generic_errors => vr_errors('EmptyField', 'EmptyField', 'InvalidChars', 'InvalidChars'),
        objects_results => [
            {num1 => vr_errors('InvalidField')},
            {},
            {num2 => vr_errors('InvalidField')},
            {}
        ]
    });
};

subtest 'validate add banners' => sub {
    my $banner1 = Test::Banner1->new(num1 => 100);
    my $validate_banner1 = sub {
        my ($banners) = @_;
        my $vr_main = Direct::ValidationResult->new();
        for my $banner (@$banners) {
            my $vr = $vr_main->next;    
            if ($banner->num1 == 0) {
                $vr->add(num1 => error_EmptyField());
            }
        }           
        return $vr_main;
    };
    
    my $vr1 = validate_add_banners_adgroup({
        'Test::Banner1' => $validate_banner1
    }, [$banner1], Direct::Model::AdGroup->new(banners_count => $Settings::DEFAULT_CREATIVE_COUNT_LIMIT));
    cmp_validation_result($vr1, vr_errors('LimitExceeded'));

    my $vr2 = validate_add_banners_adgroup({
        'Test::Banner1' => $validate_banner1
    }, [$banner1], Direct::Model::AdGroup->new(banners_count => $Settings::DEFAULT_CREATIVE_COUNT_LIMIT - 1));
    ok_validation_result($vr2);

    my $vr3 = validate_add_banners_adgroup({
        'Test::Banner1' => $validate_banner1
    }, [$banner1], Direct::Model::AdGroup->new(banners_count => $Settings::DEFAULT_CREATIVE_COUNT_LIMIT),
    banners_count_to_delete => 1);
    ok_validation_result($vr3);

    my $vr4 = validate_add_banners_adgroup({
        'Test::Banner1' => $validate_banner1
    }, [($banner1) x $Settings::DEFAULT_CREATIVE_COUNT_LIMIT], Direct::Model::AdGroup->new(banners_count => 0));
    ok_validation_result($vr4);

    my $vr5 = validate_add_banners_adgroup({
        'Test::Banner1' => $validate_banner1
    }, [($banner1) x $Settings::DEFAULT_CREATIVE_COUNT_LIMIT], Direct::Model::AdGroup->new(banners_count => 1));
    cmp_validation_result($vr5, vr_errors('LimitExceeded'));
};


done_testing;
