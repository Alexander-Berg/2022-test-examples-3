# coding: utf-8

from market.proto.content.mbo.ExportReportModel_pb2 import ParameterValue, LocalizedString
from market.proto.content.mbo.ExportReportModel_pb2 import Picture as PictureProto


class Pictures(object):
    def __init__(self, num_add_pictures, package_index=0, has_main_picture=True, url=None):
        """
        Generates pictures for model give methods to obtain model's proto's and index's doc's properties
        There is always one main picture and 'num_add_pictures' (can be zero) of additional pictures
        Pictures objects with the same 'package_index' has same urls and sizes of corresponded pictures,
        thus same proto's and index's properties

        :param num_add_pictures: Количество дополнительных картинок
        :param package_index: Уникальный идентификатор набора картинок
        :param has_main_picture: Признак наличия клавной картинки
        """
        # 'XL-Picture' - идет как главная, additional начинаются с #2: 'XL-Picture_2', ...
        self.main_picture = XlPicture(package_index, 0, url) if has_main_picture else None
        self.add_pictures = [XlPicture(package_index, i, url) for i in range(2, num_add_pictures + 2)]

    def proto_pictures(self):
        """
            give it to model's proto "pictures" field
        """
        ret = [self.main_picture.proto_picture()] if self.main_picture else []
        for pic in self.add_pictures:
            ret += [pic.proto_picture()]
        return ret

    def index_properties(self):
        """
            look for it in index
        """
        ret = {}
        if self.main_picture:
            ret[self.main_picture_index_property_name()] = self.main_picture_index_property()
        if self.add_pictures:
            ret[self.additional_pictures_index_property_name()] = self.additional_pictures_index_property()

        return ret

    @staticmethod
    def index_properties_names():
        return Pictures.main_picture_index_property_name(), Pictures.additional_pictures_index_property_name()

    @staticmethod
    def main_picture_index_property_name():
        return 'PicInfo'

    @staticmethod
    def additional_pictures_index_property_name():
        return 'AddPicsInfo'

    def main_picture_index_property(self):
        return self.main_picture.index_property_part()

    def additional_pictures_index_property(self):
        ret = ''
        for pic in self.add_pictures:
            if ret:
                ret += '\t'
            ret += pic.index_property_part()
        return ret


class XlPicture(object):
    def __init__(self, package_index, picture_index, url=None):
        self.picture_index = picture_index
        self.package_index = package_index
        assert(self.picture_index >= -1)

        if url is not None:
            self._url = url
        else:
            self._url = 'xl_picture_url/{}/{}'.format(self.package_index, self.picture_index)

    def width(self):
        return (self.picture_index + 1) * 1000 + 300 + self.package_index

    def height(self):
        return (self.picture_index + 1) * 1000 + 400 + self.package_index

    def url(self):
        return self._url

    def additional_params(self):
        return [
            self.__string_parameter("stringParameter", "str({}, {})".format(self.package_index, self.picture_index + 1)),
            self.__numeric_parameter("numericParameter", self.package_index * 100 + self.picture_index + 1),
            self.__enum_parameter("enumParameter", (self.picture_index + 1) * 100 + self.package_index)
        ]

    def xsl_name(self):
        return self.__xsl_name('XL-Picture')

    def __xsl_name(self, prop):
        if not self.picture_index:
            return prop
        return prop + '_' + str(self.picture_index)

    def proto_picture(self):
        return PictureProto(xslName=self.xsl_name(),
                            url=self.url(),
                            width=self.width(),
                            height=self.height(),
                            parameter_values=self.additional_params()
                            )

    def index_property_part(self):
        return '{}#{}#{}'.format(self.url(), self.width(), self.height())

    @staticmethod
    def __string_parameter(name, s):
        ls = LocalizedString(isoCode='ru', value=s)
        return ParameterValue(xsl_name=name, str_value=[ls])

    @staticmethod
    def __numeric_parameter(name, n):
        return ParameterValue(xsl_name=name, numeric_value=str(n))

    @staticmethod
    def __enum_parameter(name, n):
        return ParameterValue(xsl_name=name, option_id=n)
