FROM node:12.0-stretch-slim AS builder
ENV APP=/var/www

# Create app directory
RUN mkdir -p $APP
WORKDIR $APP

# Install app dependencies
COPY ./src $APP/

# this should build in /dist/angui
COPY . $APP
RUN echo "Replace this command with the one to run the quality code scanner"



# docker build -t hellofront -f Dockerfile  .
# docker run -p 8080:80 -d --name hellofront hellofront

# to inspect
# docker run -it -p 8080:80  hellofront /bin/bash

