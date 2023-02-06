from travel.avia.api_gateway.lib.avatars.url import (
    AvatarsUrlDescriptor,
    Operation,
    AVATARS_URL_FOR_INSTALLATION,
    AvatarsInstallationType,
)


def test_request_descriptor_from_orig_image_url():
    original_image_url = 'https://avatars.mds.yandex.net/get-avia/233213/2a0000015a8042af52eac47d97f85b4e0423/orig'
    descriptor: AvatarsUrlDescriptor = AvatarsUrlDescriptor.from_orig_image_url(
        original_image_url, Operation.getimageinfo
    )
    assert isinstance(descriptor, AvatarsUrlDescriptor)
    assert descriptor.installation == AVATARS_URL_FOR_INSTALLATION[AvatarsInstallationType.PRODUCTION].internal
    assert descriptor.operation == Operation.getimageinfo
    assert descriptor.namespace == 'avia'
    assert descriptor.group_id == '233213'
    assert descriptor.image_name == '2a0000015a8042af52eac47d97f85b4e0423'

    descriptor: AvatarsUrlDescriptor = AvatarsUrlDescriptor.from_orig_image_url(original_image_url, Operation.get)
    assert isinstance(descriptor, AvatarsUrlDescriptor)
    assert descriptor.installation == AVATARS_URL_FOR_INSTALLATION[AvatarsInstallationType.PRODUCTION].public
    assert descriptor.operation == Operation.get
    assert descriptor.namespace == 'avia'
    assert descriptor.group_id == '233213'
    assert descriptor.image_name == '2a0000015a8042af52eac47d97f85b4e0423'


def test_url_for_image_alias():
    original_image_url = 'https://avatars.mds.yandex.net/get-avia/233213/2a0000015a8042af52eac47d97f85b4e0423/orig'
    descriptor: AvatarsUrlDescriptor = AvatarsUrlDescriptor.from_orig_image_url(original_image_url, Operation.get)
    url = descriptor.url_for_image_alias('some-image-alias')
    assert url == 'https://avatars.mds.yandex.net/get-avia/233213/2a0000015a8042af52eac47d97f85b4e0423/some-image-alias'
