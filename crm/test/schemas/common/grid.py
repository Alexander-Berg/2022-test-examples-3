from marshmallow import fields, Schema


class GridFieldItemSchema(Schema):
    id = fields.Int(required=True)
    name = fields.Str(required=True)
    isDeleted = fields.Bool(required=True)


class GridSortSchema(Schema):
    id = fields.Str(required=True)
    order = fields.Str(required=True)
    nullsOrderType = fields.Str(required=True)


class GridFilterSchema(Schema):
    userFilters = fields.Raw()


class GridMetaFieldSchema(Schema):
    id = fields.Str(required=True)
    title = fields.Str(required=True)
    sortable = fields.Bool(required=True)
    isPinned = fields.Bool(required=True)
    type = fields.Str(required=True)
    access = fields.Int(required=True)
    items = fields.List(fields.Nested(GridFieldItemSchema))
    provider = fields.Str()
    columnWidth = fields.Str()


class GridActionSchema(Schema):
    id = fields.Str(required=True)
    caption = fields.Str(required=True)
    formCaption = fields.Str(required=True)
    isConfirm = fields.Bool(required=True)
    isForm = fields.Bool(required=True)
    actionUrl = fields.Str(required=True)
    isRefresh = fields.Bool(required=True)
    mode = fields.Str(required=True)
    order = fields.Int(required=True)
    isReport = fields.Bool(required=True)
    reportType = fields.Str(required=True)


class GridMetaSchema(Schema):
    fieldsVisibility = fields.List(fields.Str, required=True)
    title = fields.Str(required=True)
    actions = fields.List(fields.Nested(GridActionSchema))
    fields = fields.List(fields.Nested(GridMetaFieldSchema, required=True), required=True)


class GridPageSchema(Schema):
    caption = fields.Str(required=True)
    url = fields.Str(required=True)


class GridPaginationSchema(Schema):
    pages = fields.List(fields.Nested(GridPageSchema), required=True)
    page = fields.Int(required=True)
    pageSize = fields.Int(required=True)


class GridDataFieldItemSchema(Schema):
    type = fields.Str(required=True)
    id = fields.Str(required=True)
    data = fields.Raw()


class GridDataFieldSchema(Schema):
    id = fields.Str(required=True)
    fields = fields.List(fields.Nested(GridDataFieldItemSchema), required=True)


class GridSchema(Schema):
    data = fields.List(fields.Nested(GridDataFieldSchema), required=True)
    meta = fields.Nested(GridMetaSchema, required=True)
    gridPagination = fields.Nested(GridPaginationSchema, required=True)
    filters = fields.Nested(GridFilterSchema, required=True)
    sort = fields.List(fields.Nested(GridSortSchema), required=True)
