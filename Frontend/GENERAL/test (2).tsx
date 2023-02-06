import React from 'react';
import { createGetServerSideProps } from '@/helpers/ssr-props';
import { TContent } from '@/types';

interface IProps {
    content: TContent;
}

export default function PageTest(props: IProps) {
    const { content } = props;

    return (
        <div className="page-test">
            <h1>{content.page_test.title}</h1>
        </div>
    );
}

export const getServerSideProps = createGetServerSideProps();
