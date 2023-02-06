import { IRubric } from 'news/types/IRubric';
import { EContentType } from 'news/types/IRubricStory';

export const widget = {
  rubric: {
    id: 0,
    is_region: false,
  },
  stories: [{
    content_type: EContentType.POSTER,
    annot: {
      persistent_id: '777',
      snippets: [{
        doc: {
          source_name: 'Izvestia',
          pub_date: 1234567890,
        },
      }],
      title: {
        text: 'title',
      },
      story_url: '/story-url',
    },
    counts: {
      time_stat: {
        last_doc: 1234567809,
        last_non_dup: 1234567809,
      },
    },
  }],
} as IRubric;

export const appearedWidget = {
  rubric: {
    id: 0,
    is_region: false,
  },
  stories: [
    {
      content_type: EContentType.POSTER,
      annot: {
        persistent_id: '777',
        snippets: [
          {
            doc: {
              source_name: 'Izvestia',
              pub_date: 1234567890,
            },
          }],
        title: {
          text: 'title',
        },
        story_url: '/story-url',
      },
      counts: {
        time_stat: {
          last_doc: 1234567809,
          last_non_dup: 1234567809,
        },
      },
    },
    {
      content_type: EContentType.POSTER,
      annot: {
        persistent_id: '777',
        snippets: [{
          doc: {
            source_name: 'Izvestia',
            pub_date: 1234567890,
          },
        }],
        title: {
          text: 'title',
        },
        story_url: '/story-url',
      },
      counts: {
        time_stat: {
          last_doc: 1234567809,
          last_non_dup: 1234567809,
        },
      },
      quotes: null,
    },
    {
      content_type: EContentType.POSTER,
      annot: {
        persistent_id: '777',
        snippets: [{
          doc: {
            source_name: 'Izvestia',
            pub_date: 1234567890,
          },
        }],
        title: {
          text: 'title',
        },
        story_url: '/story-url',
      },
      counts: {
        time_stat: {
          last_doc: 1234567809,
          last_non_dup: 1234567809,
        },
      },
      quotes: null,
    },
  ],
} as IRubric;
