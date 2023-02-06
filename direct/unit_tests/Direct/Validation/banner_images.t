use Direct::Modern;

use Test::More;
use Yandex::Test::ValidationResult;
use Direct::Model::BannerImage;
use Direct::Model::ImageFormat;

use Settings;


BEGIN {
    use_ok('Direct::Validation::BannerImages', qw/
        validate_banner_images
    /);
}


sub mk_image {
    my ($image_hash, $image_type) = @_;
    my $image = Direct::Model::BannerImage->new(
        hash => $image_hash,
        ($image_type ? (format => Direct::Model::ImageFormat->new(hash => $image_hash, image_type => $image_type)) : ())
    );
    return $image;
}

local *vr = sub {
      my ($image_hash, $image_type) = @_;

      my $image = mk_image($image_hash, $image_type);
      my $vr = validate_banner_images([$image]);
      return $vr;
};

subtest 'good_image' => sub {
    ok_validation_result(vr('meyVQTJWXl1SFcRm1jofYA', 'regular'));
};

subtest 'deleted_images' => sub {
    cmp_validation_result(vr('7QqxpmKjhXUZoES4E8wkNw'), [vr_errors(qr/не существует/)] );
    cmp_validation_result(vr('7QqxpmKjhXUZoES4E8wkNw', undef), [vr_errors(qr/не существует/)]);
};

subtest 'not_fit' => sub {

    cmp_validation_result(vr('dRCQtYpS-1S-DDD70hS8-g', 'small'), [vr_errors(qr/не соответствует/)]);

    my $vr_1 = validate_banner_images([mk_image('dRCQtYpS-1S-DDD70hS8-g', 'small')], allowed_type => ['small']);
    ok_validation_result($vr_1);
};


done_testing;

