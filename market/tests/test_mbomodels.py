# -*- coding: utf-8 -*-

from market.proto.content.mbo import MboParameters_pb2
import market.proto.content.mbo.ExportReportModel_pb2 as erm

from market.pylibrary.mbostuff import mbomodels


class Vendor(object):
    def __init__(self, published=True):
        self.published = published
        self.id = self.__hash__()


class Model(object):
    def __init__(self, vendor, published_white=True, published_blue=True, is_group=False, is_mod=False, vendor_min_publish_timestamp=None):
        self.vendor_id = vendor.id
        self.published_white = published_white and vendor.published
        self.published_blue = published_blue and vendor.published
        self.is_group = is_group
        self.is_modification = is_mod
        #
        self.id = self.__hash__()
        self.current_type = 'GURU'
        self.vendor_min_publish_timestamp = vendor_min_publish_timestamp

    def __str__(self):
        return '{}'.format(self.id())


class GuruCategory(object):
    def __init__(self, vendors, published=True, grouped=False):
        self.vendors = vendors
        self.published = published
        self.grouped = grouped
        #
        self.output_type = MboParameters_pb2.GURU

    def iter_vendors(self):
        return iter(self.vendors)


def test_iter_published_models():
    """Test model with not published vendor
    """
    vendor = Vendor()
    vendor_not_pub = Vendor(published=False)
    model1 = Model(vendor)
    model2 = Model(vendor_not_pub)
    model3 = Model(vendor)
    cat = GuruCategory(vendors=[vendor, vendor_not_pub])

    models = []
    for model, mods in mbomodels.iter_published_models(cat, [model1, model2, model3]):
        assert mods is None
        models.append(model)

    assert len(models) == 2
    assert models == [model1, model3]


def test_iter_published_models_with_vendor_timestamp():
    """Test model when vendor specify publish date
    """
    vendor = Vendor()
    model1 = Model(vendor, vendor_min_publish_timestamp=4297104000)  # skip, 03/04/2106 @ 12:00am (UTC)
    model2 = Model(vendor, vendor_min_publish_timestamp=1550102400)  # keep, 02/14/2019 @ 12:00am (UTC)
    cat = GuruCategory(vendors=[vendor])

    models = []
    for model, mods in mbomodels.iter_published_models(cat, [model1, model2]):
        assert mods is None
        models.append(model)

    assert len(models) == 1
    assert models == [model2]


def test_iter_published_models_with_ignore_vendor_timestamp():
    """Test model when vendor specify publish date, but we ignore it during iterating
    """
    vendor = Vendor()
    model1 = Model(vendor, vendor_min_publish_timestamp=4297104000)  # keep, 03/04/2106 @ 12:00am (UTC)
    model2 = Model(vendor, vendor_min_publish_timestamp=1550102400)  # keep, 02/14/2019 @ 12:00am (UTC)
    cat = GuruCategory(vendors=[vendor])

    models = []
    for model, mods in mbomodels.iter_published_models(cat, [model1, model2], ignore_vendor_min_publish_timestamp=True):
        assert mods is None
        models.append(model)

    assert len(models) == 2
    assert models == [model1, model2]


def test_iter_published_blue_models():
    """Test model with not published vendor
    """
    vendor = Vendor()
    vendor_not_pub = Vendor(published=False)
    model1 = Model(vendor)
    model2 = Model(vendor_not_pub)
    model3 = Model(vendor, published_white=False)
    cat = GuruCategory(vendors=[vendor, vendor_not_pub])

    models = []
    for model, mods in mbomodels.iter_published_models(
        cat,
        [model1, model2, model3],
        published_blue=False
    ):
        assert mods is None
        models.append(model)

    assert len(models) == 1
    assert models == [model1]


def test_iter_published_models_group():
    """Test group without published mods
    """
    vendor = Vendor()
    models = [
        Model(vendor, is_group=True),
        Model(vendor, is_mod=True),
        Model(vendor, is_group=True),
        Model(vendor, is_mod=True),
    ]
    cat = GuruCategory(vendors=[vendor])
    groups = []
    for model, mods in mbomodels.iter_published_models(cat, models):
        assert model.is_group
        assert len(mods) == 1
        groups.append(model)

    assert len(groups) == 2


def test_iter_published_blue_models_group():
    """Test group without published mods
    """
    vendor = Vendor()
    models = [
        Model(vendor, is_group=True, published_white=False),
        Model(vendor, is_mod=True, published_white=False),
        Model(vendor, is_group=True),
        Model(vendor, is_mod=True,),
    ]
    cat = GuruCategory(vendors=[vendor])
    groups = []
    for model, mods in mbomodels.iter_published_models(cat, models, published_blue=False):
        assert model.is_group
        assert len(mods) == 1
        groups.append(model)

    assert len(groups) == 1


def test_xl_picture():
    """Проверка картинки, хранящейся в поле pictures
    """
    nice_url = '//example.com/nice_deer.jpg'
    nice_x = 100
    nice_y = 200

    model_pb = erm.ExportReportModel(
        pictures=[
            erm.Picture(
                xslName='XL-Picture',
                url=nice_url,
                width=nice_x,
                height=nice_y,
            ),
        ]
    )

    model = mbomodels.MboModel(model_pb, 'from_memory')
    assert model.xl_picture == nice_url
    assert model.xl_picture_size == (nice_x, nice_y)


def test_barcodes():
    model_pb = erm.ExportReportModel(
        parameter_values=[
            make_str_parameter_values('BarCode', ['100', '200', '300']),
        ],
    )

    barcodes = mbomodels.MboModel(model_pb, 'from_memory').get_barcodes()

    assert len(barcodes) == 3
    assert barcodes[0] == '100'
    assert barcodes[1] == '200'
    assert barcodes[2] == '300'


def make_str_parameter_values(xsl_name, values):
    str_values = [erm.LocalizedString(isoCode='ru', value=value) for value in values]
    return erm.ParameterValue(
        xsl_name=xsl_name,
        str_value=str_values,
    )


def make_str_parameter_value(xsl_name, value):
    return make_str_parameter_values(xsl_name, [value])


def make_int_parameter_value(xsl_name, value):
    return erm.ParameterValue(
        xsl_name=xsl_name,
        numeric_value=str(value),
    )
