/* eslint-disable */
import * as Sequelize from 'sequelize';

const enumValue = 'testRequired';

export default {
    async up(queryInterface: Sequelize.QueryInterface) {
        await queryInterface.sequelize.query(`ALTER TYPE "enum_drafts_status" ADD VALUE IF NOT EXISTS '${enumValue}';`);
    },

    async down(queryInterface: Sequelize.QueryInterface) {},
};
