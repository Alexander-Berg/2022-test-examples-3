module.exports = contextData => {
    const { responses } = contextData;
    const [firstResponse] = responses;
    const { data } = firstResponse;
    const { product } = data;

    return {
        ...contextData,
        responses: [
            {
                ...firstResponse,
                data: {
                    ...data,
                    product: {
                        ...product,
                        productsBeruModelsAssociation: [],
                    },
                },
            },
        ],
    };
};
