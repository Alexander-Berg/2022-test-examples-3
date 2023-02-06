# -*- coding: utf-8 -*-
import abc
import mock


def get_mock_patch_type():
    class Dummy:
        foo = None
    return type(mock.patch.object(Dummy, 'foo'))


MOCK_PATCH_TYPE = get_mock_patch_type()


class MagicMockPack(object):
    """
    Сущность для хранения объектов MagicMock

    Получаем MagicMock-и через атрибуты
    """
    def __repr__(self):
        mocks = {}
        for attr_name in dir(self):
            if attr_name.startswith('_'):
                continue
            mocks[attr_name] = getattr(self, attr_name)
        default_repr = super(MagicMockPack, self).__repr__()
        return "%s(%r)" % (default_repr, mocks)


class BaseStub(object):
    """
    Абстрактный класс для заглушек

    Использовать как context manager:
        > with Stub():
    Или делаем `start`, потом `stop`:
        > stub = Stub()
        > stub.start()
        > ...
        > stub.stop()
    """
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def start(self):
        pass

    @abc.abstractmethod
    def stop(self):
        pass

    def __enter__(self):
        return self.start()

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.stop()

    def __call__(self, func):
        """Позволяет использовать заглушку как декоратор к тестовому методу"""
        def wrapper(*args, **kwargs):
            with self:
                return func(*args, **kwargs)
        return wrapper


class ChainedPatchBaseStub(BaseStub):
    """
    Класс позволяющий описать заглушку как набор mock-ов

    В конструкторе указываем в качестве атрибутов патчи: `mock.patch(...)`
    https://docs.python.org/dev/library/cpp/testing/unittest.mock.html#unittest.mock.patch
    В классе наследнике вызываем родительский конструктор в самом конце
    """
    mocks = None

    def __init__(self, exclude_patches=None):
        self.exclude_patches = exclude_patches
        self._active_patch_names = None
        self._patches_prepared = False

    def _prepare_patches(self):
        if self._patches_prepared:
            return

        available_patch_names = set(self._get_available_patch_names())
        if not available_patch_names:
            raise AttributeError("Patches not found")

        if not self.exclude_patches:
            self._active_patch_names = available_patch_names
        elif not isinstance(self.exclude_patches, (list, tuple)):
            raise TypeError("`exclude_patches` should be `tuple` or `list`")
        else:
            exclude_patch_names = set(self.exclude_patches)
            unexpected_patch_names = exclude_patch_names - available_patch_names
            if unexpected_patch_names:
                raise ValueError("Found unexpected patches: %s. Available patches: %s" % (unexpected_patch_names, available_patch_names))
            self._active_patch_names = available_patch_names - exclude_patch_names

        if not self._active_patch_names:
            raise ValueError("Active patches not found")

        # копируем оригинальные патчи, т.к. они могут быть классовыми
        for patch_name in available_patch_names:
            orig_patch = getattr(self, patch_name)
            setattr(self, patch_name, orig_patch.copy())
        self._patches_prepared = True

    def _get_available_patch_names(self):
        patch_names = []
        for attr_name in dir(self):
            if attr_name.startswith('_'):
                continue
            attr = getattr(self, attr_name)
            if not isinstance(attr, MOCK_PATCH_TYPE):
                continue
            patch_names.append(attr_name)
        return patch_names

    def _get_active_patches(self):
        return {n: getattr(self, n) for n in self._active_patch_names}

    def start(self):
        self._prepare_patches()
        magic_mock_pack = MagicMockPack()
        for patch_name, patch in self._get_active_patches().iteritems():
            magic_mock = patch.start()
            setattr(magic_mock_pack, patch_name, magic_mock)
        self.mocks = magic_mock_pack
        return magic_mock_pack

    def stop(self):
        self._prepare_patches()
        for patch in self._get_active_patches().itervalues():
            patch.stop()
