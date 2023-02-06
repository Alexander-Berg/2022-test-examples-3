FROM node:8

ARG PR_NUMBER

ENV NODE_ENV prodiction
ENV PR_NUMBER $PR_NUMBER

ADD app /app
ADD node_modules /node_modules

CMD ["node", "/app/index.js"]
