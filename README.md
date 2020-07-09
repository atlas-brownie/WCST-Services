# WCST-Services API

This project provides APIs for uploading PDF version of Form T4NG to the VWPO(The Veteran Widget Product Office)via the Benefits Intake API.

For frontend, see [WCST-UI](https://github.com/atlas-brownie/WCST-UI)

## Base setup

**See the [native setup instructions](native.md) if you can't use docker**

To start, fetch this code:

`git clone https://github.com/atlas-brownie/WCST-Services.git`

1. Install [Docker Engine](https://docs.docker.com/engine/install/) for your platform. We strongly recommend Docker Desktop for [Mac](https://docs.docker.com/engine/install/) or [Windows](https://docs.docker.com/docker-for-windows/install/) users.

1. Go to the file src/main/resources/application-local.yaml and update following properties
   - vaAuthHeaderValue ( with the apikey value obtained from [VA developer site](https://developer.va.gov/apply))
   - vaClaimIntakePointerUrl (Make sure it is pointing to sandbox box environment)

## Running the app

1. Create docker image

   docker build -t brownie1/wcstsrv:latest -f ./docker/Dockerfile .

2. Run the docker image

   docker run -ti -p 8080:8080 brownie1/wcstsrv:latest

3. Verify deployment by using [swagger end point](http://localhost:8080/swagger-ui.html)

4. Execute Claims Controller in swagger.

## Configuration

WCST-Services API is configured with [Config](https://github.com/atlas-brownie/WCST-Services/tree/develop/src/main/resources).

The default configuration is contained in [application-local.yaml](chttps://github.com/atlas-brownie/WCST-Services/blob/develop/src/main/resources/application-local.yaml). Update configuration specific to your needs.

When deploying in AWS environment use other configuration files and pass profile(dev, prod, stage) as environment variables
