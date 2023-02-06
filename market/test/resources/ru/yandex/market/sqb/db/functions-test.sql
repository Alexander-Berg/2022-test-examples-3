
-- Проверка необходимых функций

select
  nvl(null,       'All the leaves are brown and the sky is gray'),
  nvl2(null,      'I''ve been for a walk on a winter''s day',
                  'I''d be safe and warm if I was in L.A'),
  lower(          'California dreamin'' on such a winter''s day'),

  substr(         'All the leaves are brown and the sky is gray', 7),
  decode(1, 2,    'I''ve been for a walk on a winter''s day',
            3,    'I''d be safe and warm if I was in L.A'),
  regexp_replace( 'California dreamin'' on such a winter''s day', '.+', ' '),

  coalesce(null,  'I stopped into a church I passed along the way',
                  'I''ve been for a walk on a winter''s day, yeah',
                  'I''d be safe and warm if I was in L.A.',
                  'California dreaming on such a winter''s day'
                  'California dreaming on such a winter''s day'),

  chr(13),
  sign(-1)
from dual;
