FROM node:12.0-stretch-slim AS builder
ENV APP=/var/www

# Create app directory
RUN mkdir -p $APP
WORKDIR $APP

# Install app dependencies
COPY package*.json $APP/

RUN npm install

# this should build in /dist/angui
COPY . $APP

# docker build -t basebuild -f Stage0_baseimage.Docker  .


