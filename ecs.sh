#!/bin/bash
#Constants
REGION=us-east-1
REPOSITORY_NAME=wcst-services
CLUSTER=dev-services
FAMILY=sto2-cicd-stack-web-task
NAME=dev
SERVICE_NAME=dev-services

#Store the repositoryUri as a variable

REPOSITORY_URI=`aws ecr describe-repositories --repository-names ${REPOSITORY_NAME} --region ${REGION} --debug | jq .repositories[].repositoryUri | tr -d'"'`

#Replace the build number and respository URI placeholders with the constants above.

sed -e "s;%BUILD_NUMBER%;${BUILD_NUMBER};g" -e "s;%REPOSITORY_URI%;${REPOSITORY_URI};g" taskdef.json > ${NAME}-v_${BUILD_NUMBER}.json

#Register the task definition in the repository

aws ecs register-task-definition --family ${FAMILY} --cli-input-json file://${WORKSPACE}/${NAME}-v_${BUILD_NUMBER}.json --region ${REGION} --debug

SERVICES=`aws ecs describe-services --services ${SERVICE_NAME} --cluster ${CLUSTER} --region ${REGION} --debug | jq .failures[]`

#Get latest revision

REVISION=`aws ecs describe-task-definition --task-definition ${NAME} --region ${REGION} --debug | jq .taskDefinition.revision`
