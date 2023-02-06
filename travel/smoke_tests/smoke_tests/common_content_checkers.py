class ResponseConditionCheck(object):
    """Проверка удовлетворения ответа указанному условию"""
    def __init__(self, name, condition_fun):
        """
        :param name: имя условия, задается, чтобы потом можно было опознать его в упавшем тесте
        :param condition_fun: функция с булевым результатом, на вход принимающая json ответа
        """
        self.name = name
        self.condition_fun = condition_fun

    def __call__(self, checker, response):
        if not self.condition_fun(response.json()):
            raise Exception(f'Condition "{self.name}" is failed, url: {checker.config.full_url}')


class MinItemsCount(object):
    """Проверяет, что ответ ручки содержит список с достаточным количество элементов"""
    def __init__(self, required_items_count, field_path=None):
        """
        :param required_items_count: минимальное допустимое число элементов
        :param field_path: путь к списку в ответе,
        задается как список имен ключей во вложенных словарях, None - если список в корне
        """
        self.field_path = [field_path] if isinstance(field_path, str) else field_path
        self.required_items_count = required_items_count

    def __call__(self, checker, response):
        items = response.json()
        if self.field_path:
            for field in self.field_path:
                if field not in items:
                    raise Exception(f'Field {str(self.field_path)} not found in response')
                items = items[field]

        if len(items) < self.required_items_count:
            field_path_description = str(self.field_path) if self.field_path else ''
            raise Exception(
                f'Not enough items in response ({field_path_description}), '
                f'found: {len(items)}, required:{self.required_items_count}, url: {checker.config.full_url}'
            )


class HasItem(object):
    """Проверяет, что в ответе есть указанное поле, по указанному пути"""
    def __init__(self, field_path, item_value=None):
        """
        :param field_path: путь к полю в ответе, задается как список имен ключей во вложенных словарях
        :param item_value: требуемое значение поля, если не указано, то не проверяется
        """
        self.field_path = [field_path] if isinstance(field_path, str) else field_path
        self.item_value = item_value

    def __call__(self, checker, response):
        items = response.json()
        for field in self.field_path:
            if field not in items:
                raise Exception(f'Field {str(self.field_path)} not found in response')
            items = items[field]

        if self.item_value and items != self.item_value:
            raise Exception(f'Wrong value of field {str(self.field_path)}, '
                            f'found: {items}, required: {self.item_value}, url: {checker.config.full_url}')
