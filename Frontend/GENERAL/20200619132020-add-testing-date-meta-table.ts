/* eslint-disable */
import { QueryInterface, DataTypes } from 'sequelize';

const tableName = 'devicesTestingRecords';

export default {
    async up(queryInterface: QueryInterface, dataTypes: DataTypes) {
        await queryInterface.createTable(tableName, {
            id: {
                type: dataTypes.UUID,
                primaryKey: true,
                defaultValue: dataTypes.UUIDV4,
            },
            skillId: {
                type: dataTypes.UUID,
                allowNull: false,
                references: { model: 'skills', key: 'id' },
                onUpdate: 'cascade',
                onDelete: 'cascade',
                unique: true,
            },
            type: {
                type: dataTypes.ENUM('online', 'offline'),
                allowNull: false,
            },
            options: {
                type: dataTypes.JSONB,
                allowNull: true,
            },
            createdAt: {
                type: dataTypes.DATE,
                allowNull: false,
            },
            updatedAt: {
                type: dataTypes.DATE,
                allowNull: false,
            },
        });
    },

    async down(queryInterface: QueryInterface) {
        await queryInterface.dropTable(tableName);
    },
};
