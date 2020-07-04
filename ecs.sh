#!/bin/bash
#Constants
REGION=us-east-1
REPOSITORY_NAME=wcst-services
CLUSTER=dev-services
FAMILY=`sed -n ‘s/.*”family”: “\(.*\)”,/\1/p’ taskdef.json`
NAME=`sed -n ‘s/.*”name”: “\(.*\)”,/\1/p’ taskdef.json`
SERVICE_NAME=${NAME}-service

#Store the repositoryUri as a variable

REPOSITORY_URI=`aws ecr describe-repositories --repository-names ${REPOSITORY_NAME} --region ${REGION} | jq .repositories[].repositoryUri | tr -d ‘“‘`

#Replace the build number and respository URI placeholders with the constants above.

sed -e “s;%BUILD_NUMBER%;${BUILD_NUMBER};g” -e “s;%REPOSITORY_URI%;${REPOSITORY_URI};g” taskdef.json > ${NAME}-v_${BUILD_NUMBER}.json

#Register the task definition in the repository

aws ecs register-task-definition --family ${FAMILY} --cli-input-json file://${WORKSPACE}/${NAME}-v_${BUILD_NUMBER}.json --region

${REGION}

SERVICES=`aws ecs describe-services --services ${SERVICE_NAME} --cluster ${CLUSTER} --region ${REGION} | jq .failures[]`

#Get latest revision

REVISION=`aws ecs describe-task-definition --task-definition ${NAME} --region ${REGION} | jq .taskDefinition.revision`
