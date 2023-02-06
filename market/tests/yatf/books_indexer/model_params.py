# coding: utf-8
import base64
import hashlib
import six

from market.proto.content.pictures_pb2 import (
    Picture as PicrobotPicture,
    PictureMbo as PicrobotMboPicture
)
from market.proto.content.mbo.ExportReportModel_pb2 import (
    AUTO,
    OPERATOR_FILLED,
    ExportReportModel,
    LocalizedString,
    ParameterValue,
    Picture as MboPicture,
)
from market.proto.content.mbo.MboParameters_pb2 import (
    Option,
    Parameter,
    Word,
)


def string_param(values, auto=False):
    if not isinstance(values, list):
        values = [values]

    return ParameterValue(
        str_value=[
            LocalizedString(
                isoCode='ru',
                value=value,
            )
            for value in values
        ],
        value_source=(AUTO if auto else OPERATOR_FILLED),
    )


def enum_param(param_id, option_id):
    return ParameterValue(
        param_id=param_id,
        option_id=option_id,
    )


def make_enum(name, param_id, options):
    return Parameter(
        id=param_id,
        xsl_name=name,
        published=True,
        option=[
            Option(
                id=option_id,
                name=[
                    Word(name=option_name),
                ],
                published=True,
            )
            for option_id, option_name
            in options.items()
        ]
    )


def make_xl_picture(url, width=1000, height=1000, auto=False):
    return MboPicture(
        xslName='XL-Picture',
        url=url,
        width=width,
        height=height,
        value_source=(AUTO if auto else OPERATOR_FILLED),
    )


def make_model(
    model_id,
    category_id,
    params={},
    pictures=None,
    published_on_market=True,
    published_on_blue_market=False,
    vendor_id=None,
):
    def param_with_name(name, parameter_value):
        parameter_value.xsl_name = name
        return parameter_value

    return ExportReportModel(
        id=model_id,
        category_id=category_id,
        parameter_values=[
            param_with_name(name, value)
            for name, value in params.items()
        ],
        pictures=pictures,
        published_on_market=published_on_market,
        published_on_blue_market=published_on_blue_market,
        vendor_id=vendor_id,
    )


def make_b64(proto_str):
    return base64.b64encode(
        six.ensure_binary(proto_str),
        altchars=six.ensure_binary('-_')
    ).replace(
        six.ensure_binary('='),
        six.ensure_binary(',')
    )


def make_picrobot_picture_proto(pic):
    proto_picture = PicrobotPicture(**pic)
    proto_str = proto_picture.SerializeToString()
    return make_b64(proto_str)


def make_picrobot_picture_mbo_proto(pic):
    idx_pic = PicrobotMboPicture(
        url=pic.url,
        width=pic.width,
        height=pic.height,
        value_source=pic.value_source,
    )
    proto_str = idx_pic.SerializeToString()
    return make_b64(proto_str)


def make_uid_from_binary(binary):
    return base64.b64encode(
        six.ensure_binary(binary),
        altchars=six.ensure_binary('-_')
    )[:22]


def make_pic_id(url):
    h = hashlib.md5()
    h.update(six.ensure_binary(url))
    return make_uid_from_binary(h.digest())


def make_yt_record(model_id=0, barcodes=None):
    if barcodes is None:
        barcodes = []
    return {
        'model_id': model_id,
        'description': 'Model {} description'.format(model_id),
        'url': 'https://my-shop.ru/shop/books/{}.html'.format(model_id),
        'shop_name': 'my-shop-{}.ru'.format(model_id),
        'pic': [
            {
                'md5': make_pic_id('my-shop.ru/pics/{}.jpg'.format(model_id)),
                'group_id': 1234,
                'width': 200,
                'height': 200,
                'thumb_mask': 0,
            },
        ],
        'barcodes': barcodes,
    }
