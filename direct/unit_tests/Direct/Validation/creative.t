use Direct::Modern;

use Direct::Model::AdGroupText;
use Direct::Model::AdGroupCpmBanner;
use Direct::Model::AdGroupCpmGeoproduct;
use Direct::Model::BannerCreative;
use Direct::Model::BannerText;
use Direct::Model::BannerCpmBanner;
use Direct::Model::BannerCpcVideo;
use Direct::Model::CanvasCreative;
use Direct::Model::VideoAddition;
use Test::More tests => 4;
use Yandex::I18n qw/iget/;
use Direct::Validation::Errors;

use Settings;

BEGIN { use_ok('Direct::Validation::Creative'); }

*validate_banner_creative = \&Direct::Validation::Creative::validate_banner_creative;

my $DEFAULT_DURATION = $Direct::Validation::Creative::TGO_VIDEO_DURATION;

local *vr = sub {
    my ( $adgroup_type, $banner_type, $creative_type, $layout_id, $duration ) = @_;

    my $adgroup;
    if ( $adgroup_type eq 'base' ) {
        $adgroup = Direct::Model::AdGroupText->new();
    } elsif ( $adgroup_type eq 'cpm_banner') {
        $adgroup = Direct::Model::AdGroupCpmBanner->new();
    } elsif ( $adgroup_type eq 'cpm_geoproduct') {
        $adgroup = Direct::Model::AdGroupCpmGeoproduct->new();
    }

    my $creative;
    if ( $creative_type eq 'video_addition' ) {
        $creative = Direct::Model::VideoAddition->new(layout_id => $layout_id, duration  => $duration);
    } elsif ($creative_type eq 'canvas') {
        $creative = Direct::Model::CanvasCreative->new(layout_id => $layout_id);
    }
    $creative = Direct::Model::BannerCreative->new( creative => $creative );

    my $banner;
    if ( $banner_type eq 'text' ) {
        $banner = Direct::Model::BannerText->new( creative => $creative );
    }
    elsif ( $banner_type eq 'cpc_video' ) {
        $banner = Direct::Model::BannerCpcVideo->new( creative => $creative );
    } elsif ($banner_type eq 'cpm_banner') {
        $banner = Direct::Model::BannerCpmBanner->new( creative => $creative );
    }
    my $vr = validate_banner_creative( $adgroup, $banner );
    return $vr;
};

subtest 'text banner with video' => sub {
    is(vr('base', 'text', 'video_addition', '1',$DEFAULT_DURATION),undef);
    is_deeply(
            vr('base', 'text', 'video_addition', '1', 0), 
            error_BadUsage(iget('Длина видеодополнения текстово-графических объявлений должна быть %s секунд', $DEFAULT_DURATION)),
            "Video addition duration error"
            );
    is_deeply(
            vr('base', 'text', 'video_addition', '-1',$DEFAULT_DURATION), 
            error_InvalidField(iget('Указан несовместимый шаблон видео-дополнения')),
            "Video addition layout id error"
            );
};

subtest 'cpc video banner' => sub {
    is(vr('base', 'cpc_video', 'video_addition', '51',$DEFAULT_DURATION),undef);

    is_deeply(
            vr('base', 'cpc_video', 'video_addition', '51', 0), 
            error_BadUsage(iget('Длина видеообъявления должна быть %s секунд', $DEFAULT_DURATION)),
            "Video addition duration error"
            );

    is_deeply(
            vr('base', 'cpc_video', 'video_addition', '-1',$DEFAULT_DURATION), 
            error_InvalidField(iget('Выбранный креатив не подходит к типу объявления Видео')),
            "Video addition layout id error"
            );
};

subtest 'cpm canvas' => sub {
    is(vr('cpm_banner', 'cpm_banner', 'canvas', '1', undef), undef);

    is(vr('cpm_banner', 'cpm_banner', 'canvas', undef, undef), undef);

    is_deeply(
            vr('cpm_geoproduct', 'cpm_banner', 'canvas', '1', undef), 
            error_InvalidField(iget('Указан несовместимый шаблон креатива')),
            "Invalid canvas creative layout error"
            );

    is(vr('cpm_geoproduct', 'cpm_banner', 'canvas', '10', undef), undef);
};

done_testing;
