SELECT setval('grade.s_grade_id', floor(random()*100000000+1000000)::int, true);
SELECT setval('grade.s_grade_vote_id', floor(random()*100000000+1000000)::int, true);
SELECT setval('grade.s_security_data_id', floor(random()*100000000+1000000)::int, true);
SELECT setval('grade.s_achievement_event', floor(random()*100000000+1000000)::int, true);
