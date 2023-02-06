const _ = require('lodash');
const Promise = require('bluebird');

class BaseFactory {
    static get id() {
        this._id = this._id || 1;

        return this._id++;
    }

    static get subFactories() {
        return {};
    }

    static unique(data) {
        return _.pick(data, ['id']);
    }

    static create(data) {
        if (_.isArray(data)) {
            return Promise.mapSeries(data, item => this._create(item));
        }

        return this._create(data);
    }

    static async _create(data) {
        const defaults = { ...this.defaultData, ...data };

        /*
         Проходим по данным.
         Если в поле передан объект, и для поля есть субфабрика,
         создается связанная сущность, id которой кладется в поле
         в родительском объекте
        */
        const factories = this.subFactories;
        const subEntities = await Promise.props(
            _(defaults)
                .pickBy((value, field) =>
                    factories[field] && _.isPlainObject(value))
                .mapValues((value, field) =>
                    factories[field]
                        .create(value)
                        .then(_.property('id')),
                )
                .value(),

        );

        _.assign(defaults, subEntities);

        const where = this.unique(defaults);
        const [entity] = await this.table.findOrCreate({ where, defaults });

        return entity;
    }
}

module.exports = BaseFactory;
