const db = require('db/postgres');
const log = require('logger');

const sequences = [
    'admins_id_seq',
    'admins_to_roles_id_seq',
    'agencies_id_seq',
    'answers_id_seq',
    'auth_types_id_seq',
    'bans_id_seq',
    'categories_id_seq',
    'certificates_id_seq',
    'direct_sync_id_seq',
    'drafts_id_seq',
    'freezing_id_seq',
    'global_users_id_seq',
    'locks_id_seq',
    'proctoring_responses_id_seq',
    'proctoring_videos_id_seq',
    'questions_id_seq',
    'roles_id_seq',
    'sections_id_seq',
    'services_id_seq',
    'trial_template_allowed_fails_id_seq',
    'trial_templates_id_seq',
    'trial_templates_to_sections_id_seq',
    'trials_id_seq',
    'types_id_seq',
    'user_identifications_id_seq',
    'users_id_seq'
];

module.exports.clear = function *clear() {
    try {
        yield sequences.map(sequence => {
            return db.sequelize.query(`ALTER SEQUENCE ${sequence} RESTART WITH 1000;`);
        });

        yield db.Ban.destroy({ where: {} });
        yield db.UserIdentification.destroy({ where: {} });
        yield db.Certificate.destroy({ where: {} });
        yield db.AdminToRole.destroy({ where: {} });
        yield db.Draft.destroy({ where: {} });
        yield db.Lock.destroy({ where: {} });
        yield db.Admin.destroy({ where: {} });
        yield db.Freezing.destroy({ where: {} });

        yield db.Answer.destroy({ where: {} });
        yield db.TrialToQuestion.destroy({ where: {} });
        yield db.Question.destroy({ where: {} });
        yield db.ProctoringResponses.destroy({ where: {} });
        yield db.ProctoringVideos.destroy({ where: {} });
        yield db.Trial.destroy({ where: {} });

        yield db.TrialTemplateAllowedFails.destroy({ where: {} });
        yield db.TrialTemplateToSection.destroy({ where: {} });
        yield db.Section.destroy({ where: {} });
        yield db.Category.destroy({ where: {} });
        yield db.TrialTemplate.destroy({ where: {} });
        yield db.Type.destroy({ where: {} });
        yield db.Service.destroy({ where: {} });

        yield db.User.destroy({ where: {} });
        yield db.GlobalUser.destroy({ where: {} });
        yield db.AuthType.destroy({ where: {} });
        yield db.Role.destroy({ where: {} });
        yield db.Agency.destroy({ where: {} });

        yield db.DirectSync.destroy({ where: {} });
        yield db.TvmClient.destroy({ where: {} });
    } catch (err) {
        log.info('Reset db data failed', err);

        yield db.sequelize.drop();

        yield db.DirectSync.sync({ force: true });
        yield db.Agency.sync({ force: true });
        yield db.Role.sync({ force: true });
        yield db.AuthType.sync({ force: true });
        yield db.GlobalUser.sync({ force: true });
        yield db.User.sync({ force: true });
        yield db.Type.sync({ force: true });
        yield db.Service.sync({ force: true });
        yield db.TrialTemplate.sync({ force: true });
        yield db.Freezing.sync({ force: true });
        yield db.Category.sync({ force: true });
        yield db.Section.sync({ force: true });
        yield db.TrialTemplateToSection.sync({ force: true });
        yield db.TrialTemplateAllowedFails.sync({ force: true });
        yield db.Trial.sync({ force: true });
        yield db.ProctoringResponses.sync({ force: true });
        yield db.ProctoringVideos.sync({ force: true });
        yield db.Question.sync({ force: true });
        yield db.UserIdentification.sync({ force: true });
        yield db.TvmClient.sync({ force: true });

        const trialToQuestionsQuery = 'DROP TABLE IF EXISTS trials_to_questions;' +
            'CREATE TABLE trials_to_questions (' +
            'trial_id integer NOT NULL,' +
            'seq integer NOT NULL,' +
            'question_id integer NOT NULL,' +
            'question_version integer NOT NULL,' +
            'answered integer NOT NULL DEFAULT \'0\',' +
            'correct integer NOT NULL DEFAULT \'0\',' +
            'CONSTRAINT trials_to_questions_pkey PRIMARY KEY (trial_id, seq),' +
            'CONSTRAINT trials_to_questions_questions_fk FOREIGN KEY (question_id, question_version)' +
            'REFERENCES questions (id, version),' +
            'CONSTRAINT trials_to_questions_trials FOREIGN KEY (trial_id) REFERENCES trials (id)' +
            ');' +
            'CREATE INDEX trial_and_question_idx ON trials_to_questions (trial_id, question_id, answered);' +
            'CREATE INDEX trials_to_questions_question_id_idx ON trials_to_questions (question_id);';

        yield db.sequelize.query(trialToQuestionsQuery);

        const answersQuery = 'DROP TABLE IF EXISTS answers;' +
            'CREATE TABLE answers (' +
            'id SERIAL PRIMARY KEY NOT NULL,' +
            'question_id integer NOT NULL,' +
            'question_version integer NOT NULL,' +
            'correct integer NOT NULL,' +
            'text text NOT NULL,' +
            'active integer NOT NULL DEFAULT \'1\',' +
            'CONSTRAINT answers_questions_fk FOREIGN KEY (question_id, question_version)' +
            'REFERENCES questions (id, version)' +
            ');' +
            'CREATE INDEX question_id ON answers (question_id);';

        yield db.sequelize.query(answersQuery);

        yield db.Admin.sync({ force: true });
        yield db.AdminToRole.sync({ force: true });
        yield db.Certificate.sync({ force: true });
        yield db.Draft.sync({ force: true });
        yield db.Lock.sync({ force: true });
        yield db.Ban.sync({ force: true });

        yield sequences.map(sequence => {
            return db.sequelize.query(`ALTER SEQUENCE ${sequence} RESTART WITH 1000;`);
        });
    }
};
